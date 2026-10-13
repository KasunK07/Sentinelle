package SentinellePay.Sentinelle.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO returned after uploading the BIN master file.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BinUploadResponseDto {
	private final String message;
	private final int totalBinsLoaded;
	private final String uploadedAt;
	private final String s3Key;

	/**
	 * Creates a success response for a completed BIN master upload.
	 *
	 * @param count the total number of BINs loaded
	 * @param s3Key the S3 object key if stored (nullable)
	 * @return a populated success DTO
	 */
	public static BinUploadResponseDto success(int count, String s3Key) {
		return BinUploadResponseDto.builder()
				.message("BIN master uploaded successfully")
				.totalBinsLoaded(count)
				.uploadedAt(Instant.now().toString())
				.s3Key(s3Key)
				.build();
	}
}

