package SentinellePay.Sentinelle.model;


public record SkippedRow(
		int rowNumber,
		String orderId,
		String reason
) {
	
	public static SkippedRow unsupportedStatus(int rowNumber, String orderId, String rawStatus) {
		return new SkippedRow(rowNumber, orderId, "Unsupported order status: " + rawStatus);
	}
}

