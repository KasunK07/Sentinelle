package SentinellePay.Sentinelle.controller;

import SentinellePay.Sentinelle.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for processing transaction data sets.
 * Handles CSV transformations and provides results as downloadable files.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;


    @PostMapping("/generate-csv")
    @Operation(summary = "Generate transformed CSV from transaction file")
    @ApiResponse(responseCode = "200", description = "CSV generated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid CSV format")
    @ApiResponse(responseCode = "415", description = "Unsupported media type")
    @ApiResponse(responseCode = "422", description = "Data validation errors")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<Resource> generateCsv(@RequestParam("file") MultipartFile file) {
        log.debug("Processing transaction file for CSV generation: {}", file.getOriginalFilename());

        TransactionService.TransactionProcessingResult result = transactionService.process(file);
        
        byte[] csvBytes = Objects.requireNonNull(result.csvBytes(), "result.csvBytes() must not be null");
        ByteArrayResource resource = new ByteArrayResource(csvBytes);
        
        // Prepare summary for Base64 header
        String summaryHeaderValue = createBase64Summary(result);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"generated_data_set.csv\"")
                .header("X-Rows-Processed", String.valueOf(result.rowsProcessed()))
                .header("X-Rows-Skipped", String.valueOf(result.rowsSkipped()))
                .header("X-Processing-Summary", summaryHeaderValue)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    private String createBase64Summary(TransactionService.TransactionProcessingResult result) {
        try {
            Map<String, Object> summary = new HashMap<>();
            summary.put("processed", result.rowsProcessed());
            summary.put("skipped", result.rowsSkipped());
            summary.put("s3Key", result.outputS3Key());
            
            String json = objectMapper.writeValueAsString(summary);
            return Base64.getEncoder().encodeToString(json.getBytes());
        } catch (JsonProcessingException e) {
            log.error("Failed to encode summary header", e);
            return "";
        }
    }
}