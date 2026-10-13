package SentinellePay.Sentinelle.service.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import SentinellePay.Sentinelle.dto.BinUploadResponseDto;
import SentinellePay.Sentinelle.exception.InvalidFileTypeException;
import SentinellePay.Sentinelle.infrastructure.parser.BinExcelParser;
import SentinellePay.Sentinelle.infrastructure.store.BinStore;
import SentinellePay.Sentinelle.service.BinMasterService;
import SentinellePay.Sentinelle.storage.FileStorageService;

import java.io.IOException;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class BinMasterServiceImpl implements BinMasterService {

    private static final Set<String> ACCEPTED_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/vnd.ms-excel"                                            // .xls
    );

    private static final String S3_KEY = "bin-master/latest.xlsx";
    private static final String S3_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final BinExcelParser binExcelParser;
    private final BinStore binStore;
    private final FileStorageService fileStorageService;

    /**
     * {@inheritDoc}
     *
     * <p>Steps:
     * <ol>
     *   <li>Validate file is present and non-empty</li>
     *   <li>Validate content type is Excel</li>
     *   <li>Parse BIN codes via {@link BinExcelParser}</li>
     *   <li>Load codes into {@link BinStore}</li>
     *   <li>Persist file to S3 (non-fatal if it fails)</li>
     *   <li>Return success DTO</li>
     * </ol>
     */
    @Override
    public BinUploadResponseDto uploadBinMaster(MultipartFile file) {
        log.info("BIN master upload initiated — file: '{}', size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        validateFile(file);

        Set<String> binCodes;
        try {
            binCodes = binExcelParser.parse(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Could not read uploaded file stream", e);
        }

        binStore.load(binCodes);

        String s3Key = storeFileQuietly(file);

        log.info("BIN master upload complete — {} codes loaded, s3Key: {}",
                binCodes.size(), s3Key);

        return BinUploadResponseDto.success(binCodes.size(), s3Key);
    }

    @Override
    public int getBinCount() {
        return binStore.size();
    }

    @Override
    public boolean isBinLoaded() {
        return binStore.isLoaded();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileTypeException("empty or null", "Excel (.xlsx / .xls)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ACCEPTED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException(
                    contentType != null ? contentType : "unknown",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );
        }
    }

    /**
     * Stores the uploaded file to S3. Any exception is caught and logged —
     * this must never propagate to the caller.
     *
     * @return the S3 key on success, or null if storage failed
     */
    private String storeFileQuietly(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            return fileStorageService.store(bytes, S3_KEY, S3_CONTENT_TYPE);
        } catch (Exception e) {
            log.error("Non-fatal: failed to store BIN master file to S3 — {}", e.getMessage(), e);
            return null;
        }
    }
}