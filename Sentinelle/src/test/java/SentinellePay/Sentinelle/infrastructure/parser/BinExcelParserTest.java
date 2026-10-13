package SentinellePay.Sentinelle.infrastructure.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Set;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import SentinellePay.Sentinelle.exception.FileParseException;

class BinExcelParserTest {
	private final BinExcelParser parser = new BinExcelParser();

	@Test
	@DisplayName("Numeric cells (458868 stored as double) → parsed as \"458868\" (no decimal)")
	void numericCells_parsedWithoutDecimal() {
		byte[] xlsx = buildXlsxWithBins(CellType.NUMERIC, new Object[] { 458868d });

		Set<String> bins = parser.parse(asStream(xlsx), "bins.xlsx");

		assertThat(bins).containsExactly("458868");
	}

	@Test
	@DisplayName("String cells (\"458868\") → parsed as \"458868\"")
	void stringCells_parsedAsIs() {
		byte[] xlsx = buildXlsxWithBins(CellType.STRING, new Object[] { "458868" });

		Set<String> bins = parser.parse(asStream(xlsx), "bins.xlsx");

		assertThat(bins).containsExactly("458868");
	}

	@Test
	@DisplayName("Mixed numeric and string cells → all correctly parsed")
	void mixedCells_allParsed() {
		byte[] xlsx = buildMixedXlsx(new Object[] { 458868d, "123456" });

		Set<String> bins = parser.parse(asStream(xlsx), "bins.xlsx");

		assertThat(bins).containsExactlyInAnyOrder("458868", "123456");
	}

	@Test
	@DisplayName("Empty file / no data rows → throws FileParseException")
	void headerOnly_throws() {
		byte[] xlsx = buildHeaderOnlyXlsx();

		assertThatThrownBy(() -> parser.parse(asStream(xlsx), "bins.xlsx"))
				.isInstanceOf(FileParseException.class)
				.hasMessageContaining("No valid BIN codes found");
	}

	@Test
	@DisplayName("Header row skipped — only data rows returned")
	void headerSkipped_onlyDataReturned() {
		byte[] xlsx = buildHeaderAndOneDataRow("BIN", "458868");

		Set<String> bins = parser.parse(asStream(xlsx), "bins.xlsx");

		assertThat(bins).containsExactly("458868");
	}

	@Test
	@DisplayName("BIN codes stored in Set (no duplicates)")
	void duplicates_deduped() {
		byte[] xlsx = buildMixedXlsx(new Object[] { "458868", "458868", 458868d });

		Set<String> bins = parser.parse(asStream(xlsx), "bins.xlsx");

		assertThat(bins).containsExactly("458868");
	}

	private static InputStream asStream(byte[] bytes) {
		return new ByteArrayInputStream(bytes);
	}

	/**
	 * Builds an in-memory XLSX with a header row and one BIN column (A),
	 * where every data cell has the same explicit type.
	 */
	private static byte[] buildXlsxWithBins(CellType cellType, Object[] values) {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Sheet1");

			Row header = sheet.createRow(0);
			header.createCell(0, CellType.STRING).setCellValue("BIN");

			for (int i = 0; i < values.length; i++) {
				Row row = sheet.createRow(i + 1);
				switch (cellType) {
					case NUMERIC -> row.createCell(0, CellType.NUMERIC).setCellValue(((Number) values[i]).doubleValue());
					case STRING -> row.createCell(0, CellType.STRING).setCellValue(String.valueOf(values[i]));
					default -> throw new IllegalArgumentException("Unsupported cellType for test: " + cellType);
				}
			}

			return toBytes(workbook);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Builds an in-memory XLSX with header row and mixed numeric/string values in column A.
	 */
	private static byte[] buildMixedXlsx(Object[] values) {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Sheet1");

			Row header = sheet.createRow(0);
			header.createCell(0, CellType.STRING).setCellValue("BIN");

			for (int i = 0; i < values.length; i++) {
				Row row = sheet.createRow(i + 1);
				Object v = values[i];
				if (v instanceof Number n) {
					row.createCell(0, CellType.NUMERIC).setCellValue(n.doubleValue());
				} else {
					row.createCell(0, CellType.STRING).setCellValue(String.valueOf(v));
				}
			}

			return toBytes(workbook);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] buildHeaderOnlyXlsx() {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Sheet1");
			Row header = sheet.createRow(0);
			header.createCell(0, CellType.STRING).setCellValue("BIN");
			return toBytes(workbook);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] buildHeaderAndOneDataRow(String headerValue, String dataValue) {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Sheet1");
			Row header = sheet.createRow(0);
			header.createCell(0, CellType.STRING).setCellValue(headerValue);

			Row row = sheet.createRow(1);
			row.createCell(0, CellType.STRING).setCellValue(dataValue);
			return toBytes(workbook);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] toBytes(XSSFWorkbook workbook) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		workbook.write(out);
		return out.toByteArray();
	}
}

