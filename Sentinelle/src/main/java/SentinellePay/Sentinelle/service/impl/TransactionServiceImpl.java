package SentinellePay.Sentinelle.service.impl;

import SentinellePay.Sentinelle.enums.OrderStatus;
import SentinellePay.Sentinelle.enums.RowType;
import SentinellePay.Sentinelle.exception.BinMasterNotLoadedException;
import SentinellePay.Sentinelle.exception.InvalidFileTypeException;
import SentinellePay.Sentinelle.infrastructure.parser.TransactionCsvParser;
import SentinellePay.Sentinelle.infrastructure.store.BinStore;
import SentinellePay.Sentinelle.infrastructure.writer.OutputCsvWriter;
import SentinellePay.Sentinelle.model.OutputRow;
import SentinellePay.Sentinelle.model.SkippedRow;
import SentinellePay.Sentinelle.model.TransactionRow;
import SentinellePay.Sentinelle.service.MdrCalculationService;
import SentinellePay.Sentinelle.service.TransactionService;
import SentinellePay.Sentinelle.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final Set<String> ACCEPTED_CSV_TYPES = Set.of(
            "text/csv",
            "text/plain",
            "application/octet-stream",
            "application/csv"
    );

    private static final String S3_OUTPUT_PREFIX = "output/";
    private static final String S3_CONTENT_TYPE  = "text/csv";

    private final TransactionCsvParser   transactionCsvParser;
    private final BinStore               binStore;
    private final MdrCalculationService  mdrCalculationService;
    private final OutputCsvWriter        outputCsvWriter;
    private final FileStorageService     fileStorageService;

    /**
     * {@inheritDoc}
     *
     * @throws BinMasterNotLoadedException if BIN master has not been uploaded
     * @throws InvalidFileTypeException    if the uploaded file is not a CSV
     */
    @Override
    public TransactionProcessingResult process(MultipartFile file) {
        guardBinStoreLoaded();
        validateFile(file);

        log.info("Transaction processing started — file: '{}', size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        List<TransactionRow> inputRows = parseFile(file);
        List<OutputRow>      outputRows   = new ArrayList<>();
        List<SkippedRow>     skippedRows  = new ArrayList<>();

        int rowNumber = 1;
        for (TransactionRow inputRow : inputRows) {
            processRow(inputRow, rowNumber, outputRows, skippedRows);
            rowNumber++;
        }

        byte[] csvBytes  = outputCsvWriter.write(outputRows);
        String s3Key     = storeOutputQuietly(csvBytes, file.getOriginalFilename());

        log.info("Transaction processing complete — processed: {}, skipped: {}",
                outputRows.size(), skippedRows.size());

        return new TransactionProcessingResult(
                csvBytes,
                outputRows.size(),
                skippedRows.size(),
                skippedRows,
                s3Key
        );
    }

    private void processRow(
            TransactionRow input,
            int rowNumber,
            List<OutputRow> outputRows,
            List<SkippedRow> skippedRows
    ) {
        Optional<OrderStatus> statusOpt = OrderStatus.fromCsvValue(input.orderStatus());

        if (statusOpt.isEmpty()) {
            log.debug("Row {}: skipped — unsupported status '{}'", rowNumber, input.orderStatus());
            skippedRows.add(SkippedRow.unsupportedStatus(rowNumber, input.orderId(), input.orderStatus()));
            return;
        }

        OrderStatus orderStatus = statusOpt.get();
        RowType     rowType     = RowType.resolveFromOrderStatus(orderStatus);

        BigDecimal grossAmount = parseAmount(input.capturedAmount());
        BigDecimal requestAmount = parseAmount(input.orderAmount());

        boolean isLocal = binStore.isLocal(input.bin());

        MdrCalculationService.CalculationResult calc =
                mdrCalculationService.calculate(grossAmount, isLocal);

        OutputRow outputRow = OutputRow.builder()
                .rowType(rowType.name())
                .creationDate(input.orderDate())
                .approvedDate(input.lastUpdatedDate())
                .transactionId(input.orderId())
                .requestAmount(requestAmount)
                .grossLocalAmount(grossAmount)
                // localCurrency defaults to "LKR" via @Builder.Default in OutputRow
                .feeLocalAmount(calc.feeAmount())
                .netLocalAmount(calc.netAmount())
                .build();

        outputRows.add(outputRow);
    }


    private void guardBinStoreLoaded() {
        if (!binStore.isLoaded()) {
            throw new BinMasterNotLoadedException();
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileTypeException("empty or null", "CSV (.csv)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ACCEPTED_CSV_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileTypeException(
                    contentType != null ? contentType : "unknown",
                    "text/csv"
            );
        }
    }


    private List<TransactionRow> parseFile(MultipartFile file) {
        try {
            return transactionCsvParser.parse(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Could not read uploaded CSV stream", e);
        }
    }

    private BigDecimal parseAmount(String raw) {
        return new BigDecimal(raw.replace(",", ""));
    }


    private String storeOutputQuietly(byte[] csvBytes, String originalFileName) {
        try {
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                    .withZone(java.time.ZoneOffset.UTC)
                    .format(Instant.now());
            String key = S3_OUTPUT_PREFIX + timestamp + "-" + originalFileName
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            return fileStorageService.store(csvBytes, key, S3_CONTENT_TYPE);
        } catch (Exception e) {
            log.error("Non-fatal: failed to store output CSV to S3 — {}", e.getMessage(), e);
            return null;
        }
    }
}