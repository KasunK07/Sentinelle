package SentinellePay.Sentinelle.infrastructure.writer;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;

import SentinellePay.Sentinelle.model.OutputRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;


@Slf4j
@Component
public class OutputCsvWriter {

    private static final String[] HEADERS = {
            "ROW_TYPE",
            "CREATION_DATE",
            "APPROVED_DATE",
            "TRANSACTION_ID",
            "REQUEST_AMOUNT",
            "GROSS_LOCAL_AMOUNT",
            "LOCAL_CURRENCY",
            "FEE_LOCAL_AMOUNT",
            "NET_LOCAL_AMOUNT"
    };

    /**
     * Writes the given output rows to a CSV-formatted byte array.
     *
     * <p>The returned bytes are UTF-8 encoded and ready to be set directly
     * as a {@code ResponseEntity} body or uploaded to S3.
     *
     * @param rows the transformed transaction rows to write; must not be null
     * @return UTF-8 CSV content as a byte array, including the header row
     * @throws RuntimeException if an I/O error occurs during writing
     */
    public byte[] write(List<OutputRow> rows) {
        log.debug("Writing {} rows to output CSV", rows.size());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                ICSVWriter.DEFAULT_SEPARATOR,
                ICSVWriter.NO_QUOTE_CHARACTER,
                ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
                ICSVWriter.DEFAULT_LINE_END
        )) {
            writer.writeNext(HEADERS);

            for (OutputRow row : rows) {
                writer.writeNext(row.toCsvRow());
            }

            writer.flush();

        } catch (IOException e) {
            throw new RuntimeException("Failed to write output CSV", e);
        }

        log.info("Output CSV written successfully — {} data rows + 1 header row", rows.size());
        return outputStream.toByteArray();
    }
}