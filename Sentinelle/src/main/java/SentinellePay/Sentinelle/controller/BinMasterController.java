package SentinellePay.Sentinelle.controller;

import SentinellePay.Sentinelle.dto.BinUploadResponseDto;
import SentinellePay.Sentinelle.service.BinMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for managing BIN (Bank Identification Number) master data.
 * Provides endpoints for uploading reference Excel files used for MDR calculations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bin-master")
@RequiredArgsConstructor
@Tag(name = "BIN Master")
public class BinMasterController {

    private final BinMasterService binMasterService;

  
    @PostMapping("/upload")
    @Operation(summary = "Upload BIN master Excel file")
    @ApiResponse(responseCode = "200", description = "File processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or file")
    @ApiResponse(responseCode = "415", description = "Unsupported media type")
    @ApiResponse(responseCode = "422", description = "Unprocessable entity (validation errors)")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<BinUploadResponseDto> upload(@RequestParam("file") MultipartFile file) {
        log.debug("Incoming request to upload BIN master: filename={}, size={}", 
                file.getOriginalFilename(), file.getSize());

        BinUploadResponseDto response = binMasterService.uploadBinMaster(file);
        return ResponseEntity.ok(response);
    }
}