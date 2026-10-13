package SentinellePay.Sentinelle.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import SentinellePay.Sentinelle.model.SkippedRow;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO describing the result of generating the output CSV.
 * <p>
 * Intended to be returned alongside the CSV download (e.g. as response headers / metadata).
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerateCsvResponseDto {
	private final int rowsProcessed;
	private final int rowsSkipped;
	private final List<SkippedRow> skippedDetails;
	private final String outputS3Key;
	private final String processedAt;

	/**
	 * Convenience factory to populate {@code processedAt} with the current time.
	 *
	 * @param rowsProcessed number of rows processed
	 * @param rowsSkipped number of rows skipped
	 * @param skippedDetails details for skipped rows (nullable)
	 * @param outputS3Key the S3 object key if stored (nullable)
	 * @return a populated DTO with {@code processedAt} set to {@link Instant#now()}
	 */
	public static GenerateCsvResponseDto of(
			int rowsProcessed,
			int rowsSkipped,
			List<SkippedRow> skippedDetails,
			String outputS3Key
	) {
		return GenerateCsvResponseDto.builder()
				.rowsProcessed(rowsProcessed)
				.rowsSkipped(rowsSkipped)
				.skippedDetails(skippedDetails)
				.outputS3Key(outputS3Key)
				.processedAt(Instant.now().toString())
				.build();
	}
}

