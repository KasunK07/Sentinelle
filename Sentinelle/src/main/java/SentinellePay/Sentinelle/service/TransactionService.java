package SentinellePay.Sentinelle.service;


import org.springframework.web.multipart.MultipartFile;

import SentinellePay.Sentinelle.model.SkippedRow;

import java.util.List;


public interface TransactionService {


    TransactionProcessingResult process(MultipartFile file);

    /**
     * Immutable result of a transaction processing run.
     *
     * @param csvBytes       the generated output CSV as a byte array
     * @param rowsProcessed  number of rows successfully transformed
     * @param rowsSkipped    number of rows skipped due to unsupported status or data errors
     * @param skippedRows    detail records for each skipped row
     * @param outputS3Key    the S3 key where the output was stored (null if S3 disabled)
     */
    record TransactionProcessingResult(
            byte[] csvBytes,
            int rowsProcessed,
            int rowsSkipped,
            List<SkippedRow> skippedRows,
            String outputS3Key
    ) {}
}