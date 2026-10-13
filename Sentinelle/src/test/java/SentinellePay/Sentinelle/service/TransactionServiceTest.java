package SentinellePay.Sentinelle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import SentinellePay.Sentinelle.exception.BinMasterNotLoadedException;
import SentinellePay.Sentinelle.exception.InvalidFileTypeException;
import SentinellePay.Sentinelle.infrastructure.parser.TransactionCsvParser;
import SentinellePay.Sentinelle.infrastructure.store.BinStore;
import SentinellePay.Sentinelle.infrastructure.writer.OutputCsvWriter;
import SentinellePay.Sentinelle.model.OutputRow;
import SentinellePay.Sentinelle.model.TransactionRow;
import SentinellePay.Sentinelle.service.impl.TransactionServiceImpl;
import SentinellePay.Sentinelle.storage.FileStorageService;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
	@Mock private TransactionCsvParser transactionCsvParser;
	@Mock private BinStore binStore;
	@Mock private MdrCalculationService mdrCalculationService;
	@Mock private OutputCsvWriter outputCsvWriter;
	@Mock private FileStorageService fileStorageService;
	@Mock private MultipartFile file;

	@Captor private ArgumentCaptor<List<OutputRow>> outputRowsCaptor;

	private TransactionServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new TransactionServiceImpl(
				transactionCsvParser,
				binStore,
				mdrCalculationService,
				outputCsvWriter,
				fileStorageService
		);
	}

	@Test
	@DisplayName("BIN master not loaded → throws BinMasterNotLoadedException")
	void binMasterNotLoaded_throws() {
		when(binStore.isLoaded()).thenReturn(false);

		assertThatThrownBy(() -> service.process(file))
				.isInstanceOf(BinMasterNotLoadedException.class);
	}

	@Test
	@DisplayName("Null file → throws InvalidFileTypeException")
	void nullFile_throws() {
		when(binStore.isLoaded()).thenReturn(true);

		assertThatThrownBy(() -> service.process(null))
				.isInstanceOf(InvalidFileTypeException.class);
	}

	@Test
	@DisplayName("CAPTURED status → RowType = PAYMENT")
	void captured_mapsToPayment() throws Exception {
		TransactionService.TransactionProcessingResult result = runSingleRow("CAPTURED");

		assertThat(result.rowsProcessed()).isEqualTo(1);
		assertThat(result.rowsSkipped()).isZero();

		verify(outputCsvWriter).write(outputRowsCaptor.capture());
		assertThat(outputRowsCaptor.getValue()).hasSize(1);
		assertThat(outputRowsCaptor.getValue().get(0).getRowType()).isEqualTo("PAYMENT");
	}

	@Test
	@DisplayName("REFUNDED status → RowType = REFUND")
	void refunded_mapsToRefund() throws Exception {
		TransactionService.TransactionProcessingResult result = runSingleRow("REFUNDED");

		assertThat(result.rowsProcessed()).isEqualTo(1);
		assertThat(result.rowsSkipped()).isZero();

		verify(outputCsvWriter).write(outputRowsCaptor.capture());
		assertThat(outputRowsCaptor.getValue().get(0).getRowType()).isEqualTo("REFUND");
	}

	@Test
	@DisplayName("PARTIALLY_CAPTURED status → RowType = PAYMENT")
	void partiallyCaptured_mapsToPayment() throws Exception {
		TransactionService.TransactionProcessingResult result = runSingleRow("PARTIALLY_CAPTURED");

		assertThat(result.rowsProcessed()).isEqualTo(1);
		assertThat(result.rowsSkipped()).isZero();

		verify(outputCsvWriter).write(outputRowsCaptor.capture());
		assertThat(outputRowsCaptor.getValue().get(0).getRowType()).isEqualTo("PAYMENT");
	}

	@Test
	@DisplayName("PARTIALLY_REFUNDED status → RowType = REFUND")
	void partiallyRefunded_mapsToRefund() throws Exception {
		TransactionService.TransactionProcessingResult result = runSingleRow("PARTIALLY_REFUNDED");

		assertThat(result.rowsProcessed()).isEqualTo(1);
		assertThat(result.rowsSkipped()).isZero();

		verify(outputCsvWriter).write(outputRowsCaptor.capture());
		assertThat(outputRowsCaptor.getValue().get(0).getRowType()).isEqualTo("REFUND");
	}

	@Test
	@DisplayName("Unknown status \"PENDING\" → row is skipped, skippedRows list has 1 entry")
	void unknownStatus_skipped() throws Exception {
		when(binStore.isLoaded()).thenReturn(true);
		mockValidCsvFile();

		when(transactionCsvParser.parse(any(InputStream.class), anyString()))
				.thenReturn(List.of(row("PENDING", "100.00", "100.00", "458868xxxxxx1234")));

		when(outputCsvWriter.write(anyList())).thenReturn("csv".getBytes(StandardCharsets.UTF_8));
		when(fileStorageService.store(any(byte[].class), anyString(), anyString())).thenReturn("s3://bucket/key");

		TransactionService.TransactionProcessingResult result = service.process(file);

		assertThat(result.rowsProcessed()).isZero();
		assertThat(result.rowsSkipped()).isEqualTo(1);
		assertThat(result.skippedRows()).hasSize(1);
		assertThat(result.skippedRows().get(0).reason()).contains("Unsupported order status");
	}

	@Test
	@DisplayName("Local BIN → MDR 2.2% applied")
	void localBin_usesLocalMdrRate() throws Exception {
		when(binStore.isLoaded()).thenReturn(true);
		mockValidCsvFile();

		when(transactionCsvParser.parse(any(InputStream.class), anyString()))
				.thenReturn(List.of(row("CAPTURED", "1000.00", "1000.00", "458868xxxxxx1234")));

		when(binStore.isLocal("458868")).thenReturn(true);

		when(mdrCalculationService.calculate(new BigDecimal("1000.00"), true))
				.thenReturn(new MdrCalculationService.CalculationResult(new BigDecimal("22.00"), new BigDecimal("978.00")));

		when(outputCsvWriter.write(anyList())).thenReturn("csv".getBytes(StandardCharsets.UTF_8));
		when(fileStorageService.store(any(byte[].class), anyString(), anyString())).thenReturn("s3://bucket/key");

		service.process(file);

		verify(mdrCalculationService).calculate(new BigDecimal("1000.00"), true);
	}

	@Test
	@DisplayName("Foreign BIN → MDR 3.5% applied")
	void foreignBin_usesForeignMdrRate() throws Exception {
		when(binStore.isLoaded()).thenReturn(true);
		mockValidCsvFile();

		when(transactionCsvParser.parse(any(InputStream.class), anyString()))
				.thenReturn(List.of(row("CAPTURED", "1000.00", "1000.00", "458868xxxxxx1234")));

		when(binStore.isLocal("458868")).thenReturn(false);

		when(mdrCalculationService.calculate(new BigDecimal("1000.00"), false))
				.thenReturn(new MdrCalculationService.CalculationResult(new BigDecimal("35.00"), new BigDecimal("965.00")));

		when(outputCsvWriter.write(anyList())).thenReturn("csv".getBytes(StandardCharsets.UTF_8));
		when(fileStorageService.store(any(byte[].class), anyString(), anyString())).thenReturn("s3://bucket/key");

		service.process(file);

		verify(mdrCalculationService).calculate(new BigDecimal("1000.00"), false);
	}

	@Test
	@DisplayName("Mixed rows: 2 valid + 1 skipped → rowsProcessed=2, rowsSkipped=1")
	void mixedRows_countsMatch() throws Exception {
		when(binStore.isLoaded()).thenReturn(true);
		mockValidCsvFile();

		when(transactionCsvParser.parse(any(InputStream.class), anyString()))
				.thenReturn(List.of(
						row("CAPTURED", "10.00", "10.00", "458868xxxxxx1234"),
						row("PENDING", "10.00", "10.00", "458868xxxxxx1234"),
						row("REFUNDED", "10.00", "10.00", "458868xxxxxx1234")
				));

		when(binStore.isLocal("458868")).thenReturn(true);
		when(mdrCalculationService.calculate(new BigDecimal("10.00"), true))
				.thenReturn(new MdrCalculationService.CalculationResult(new BigDecimal("0.22"), new BigDecimal("9.78")));

		when(outputCsvWriter.write(anyList())).thenReturn("csv".getBytes(StandardCharsets.UTF_8));
		when(fileStorageService.store(any(byte[].class), anyString(), anyString())).thenReturn("s3://bucket/key");

		TransactionService.TransactionProcessingResult result = service.process(file);

		assertThat(result.rowsProcessed()).isEqualTo(2);
		assertThat(result.rowsSkipped()).isEqualTo(1);
		assertThat(result.skippedRows()).hasSize(1);
	}

	@Test
	@DisplayName("Case-insensitive: \"captured\" == \"CAPTURED\" == \"Captured\"")
	void caseInsensitiveCaptured_processed() throws Exception {
		when(binStore.isLoaded()).thenReturn(true);
		mockValidCsvFile();

		when(transactionCsvParser.parse(any(InputStream.class), anyString()))
				.thenReturn(List.of(
						row("captured", "10.00", "10.00", "458868xxxxxx1234"),
						row("CAPTURED", "10.00", "10.00", "458868xxxxxx1234"),
						row("Captured", "10.00", "10.00", "458868xxxxxx1234")
				));

		when(binStore.isLocal("458868")).thenReturn(true);
		when(mdrCalculationService.calculate(new BigDecimal("10.00"), true))
				.thenReturn(new MdrCalculationService.CalculationResult(new BigDecimal("0.22"), new BigDecimal("9.78")));

		when(outputCsvWriter.write(anyList())).thenReturn("csv".getBytes(StandardCharsets.UTF_8));
		when(fileStorageService.store(any(byte[].class), anyString(), anyString())).thenReturn("s3://bucket/key");

		TransactionService.TransactionProcessingResult result = service.process(file);

		assertThat(result.rowsProcessed()).isEqualTo(3);
		assertThat(result.rowsSkipped()).isZero();
	}

	private TransactionService.TransactionProcessingResult runSingleRow(String status) throws Exception {
		when(binStore.isLoaded()).thenReturn(true);
		mockValidCsvFile();

		when(transactionCsvParser.parse(any(InputStream.class), anyString()))
				.thenReturn(List.of(row(status, "1000.00", "1000.00", "458868xxxxxx1234")));

		when(binStore.isLocal("458868")).thenReturn(true);
		when(mdrCalculationService.calculate(any(BigDecimal.class), anyBoolean()))
				.thenReturn(new MdrCalculationService.CalculationResult(new BigDecimal("22.00"), new BigDecimal("978.00")));

		when(outputCsvWriter.write(anyList())).thenReturn("csv".getBytes(StandardCharsets.UTF_8));
		when(fileStorageService.store(any(byte[].class), anyString(), anyString())).thenReturn("s3://bucket/key");

		return service.process(file);
	}

	private void mockValidCsvFile() throws Exception {
		when(file.isEmpty()).thenReturn(false);
		when(file.getContentType()).thenReturn("text/csv");
		when(file.getOriginalFilename()).thenReturn("sample.csv");
		when(file.getSize()).thenReturn(10L);
		when(file.getInputStream()).thenReturn(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));
	}

	private static TransactionRow row(String status, String orderAmt, String capturedAmt, String accId) {
		return new TransactionRow(
				status,
				"2026-01-01T00:00:00Z",
				"2026-01-01T00:00:00Z",
				"ORDER-1",
				orderAmt,
				capturedAmt,
				accId
		);
	}
}

