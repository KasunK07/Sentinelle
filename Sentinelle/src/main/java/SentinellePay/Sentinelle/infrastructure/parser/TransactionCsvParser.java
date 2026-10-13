package SentinellePay.Sentinelle.infrastructure.parser;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import SentinellePay.Sentinelle.exception.FileParseException;
import SentinellePay.Sentinelle.model.TransactionRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TransactionCsvParser {

    // Updated to match the specific " (amount only)" suffix in your sample_data_set.csv
    private static final String H_STATUS = "Order Status";
    private static final String H_DATE = "Order Date";
    private static final String H_UPDATED = "Last Updated Date";
    private static final String H_ID = "Order ID";
    private static final String H_ORDER_AMT = "Order Amount (amount only)";
    private static final String H_CAPTURED_AMT = "Captured Amount (amount only)";
    private static final String H_ACC_ID = "Account Identifier";

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            H_STATUS, H_DATE, H_UPDATED, H_ID, H_ORDER_AMT, H_CAPTURED_AMT, H_ACC_ID
    );

    public List<TransactionRow> parse(InputStream inputStream, String fileName) {
        // Use UTF-8 and wrap in a reader
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            List<TransactionRow> rows = new ArrayList<>();
            Map<String, String> rowData = reader.readMap();

            if (rowData == null) {
                throw new FileParseException(fileName, "CSV file is empty", null);
            }

            // CLEAN HEADERS: Remove BOM and trim spaces
            Set<String> cleanActualHeaders = rowData.keySet().stream()
                    .map(h -> h.replace("\uFEFF", "").trim())
                    .collect(Collectors.toSet());

            validateHeaders(cleanActualHeaders, fileName);

            int rowNumber = 1;
            do {
                // We must use the exact raw keys from the map to retrieve data
                TransactionRow row = parseRow(rowData, rowNumber, fileName);
                if (row != null) {
                    rows.add(row);
                }
                rowNumber++;
            } while ((rowData = reader.readMap()) != null);

            return rows;

        } catch (IOException | CsvValidationException e) {
            throw new FileParseException(fileName, "Could not read CSV: " + e.getMessage(), e);
        }
    }

    private void validateHeaders(Set<String> actualHeaders, String fileName) {
        Set<String> missing = REQUIRED_HEADERS.stream()
                .filter(required -> !actualHeaders.contains(required))
                .collect(Collectors.toSet());

        if (!missing.isEmpty()) {
            throw new FileParseException(fileName, "Missing required CSV headers: " + missing, null);
        }
    }

    private TransactionRow parseRow(Map<String, String> rowData, int rowNumber, String fileName) {
        // Helper to find key even if it has a BOM or invisible character
        String status = getVal(rowData, H_STATUS);
        String orderAmt = getVal(rowData, H_ORDER_AMT);
        String capturedAmt = getVal(rowData, H_CAPTURED_AMT);
        String accId = getVal(rowData, H_ACC_ID);

        if (accId == null || accId.length() < 6 || !isValidAmount(orderAmt) || !isValidAmount(capturedAmt)) {
            log.warn("[{}] Skipping row {}: Invalid data", fileName, rowNumber);
            return null;
        }

        return new TransactionRow(
                status,
                getVal(rowData, H_DATE),
                getVal(rowData, H_UPDATED),
                getVal(rowData, H_ID),
                orderAmt,
                capturedAmt,
                accId
        );
    }

    private String getVal(Map<String, String> map, String key) {
        // Robust lookup: handles cases where the map key might still have the BOM
        if (map.containsKey(key)) return map.get(key).trim();
        return map.entrySet().stream()
                .filter(e -> e.getKey().contains(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("")
                .trim();
    }

    private boolean isValidAmount(String raw) {
        if (raw == null || raw.isBlank()) return false;
        try {
            new BigDecimal(raw.replace("\"", "").replace(",", "").trim());
            return true;
        } catch (Exception e) { return false; }
    }
}