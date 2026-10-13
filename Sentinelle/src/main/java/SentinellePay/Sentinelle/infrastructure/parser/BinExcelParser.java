package SentinellePay.Sentinelle.infrastructure.parser;


import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import SentinellePay.Sentinelle.exception.FileParseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;


@Slf4j
@Component
public class BinExcelParser {

    private static final int HEADER_ROW_INDEX = 0;
    private static final int BIN_COLUMN_INDEX = 0;

    public Set<String> parse(InputStream inputStream, String fileName) {
        log.debug("Starting BIN Excel parse for file: {}", fileName);

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Set<String> binCodes = new HashSet<>();

            for (Row row : sheet) {
                if (row.getRowNum() == HEADER_ROW_INDEX) {
                    continue; // skip header
                }

                Cell cell = row.getCell(BIN_COLUMN_INDEX);
                String binCode = extractBinFromCell(cell);

                if (binCode != null) {
                    binCodes.add(binCode);
                }
            }

            if (binCodes.isEmpty()) {
                throw new FileParseException(fileName, "No valid BIN codes found in column A", null);
            }

            log.info("Parsed {} BIN codes from file: {}", binCodes.size(), fileName);
            return binCodes;

        } catch (FileParseException e) {
            throw e;
        } catch (IOException e) {
            throw new FileParseException(fileName, "Could not read Excel file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new FileParseException(fileName, "Unexpected error parsing Excel: " + e.getMessage(), e);
        }
    }

    private String extractBinFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case NUMERIC -> {
                long binAsLong = (long) cell.getNumericCellValue();
                yield String.valueOf(binAsLong);
            }
            case STRING -> {
                String value = cell.getStringCellValue().trim();
                yield value.isEmpty() ? null : value;
            }
            case BLANK -> null;
            default -> {
                log.warn("Unexpected cell type {} in BIN column — skipping", cell.getCellType());
                yield null;
            }
        };
    }
}