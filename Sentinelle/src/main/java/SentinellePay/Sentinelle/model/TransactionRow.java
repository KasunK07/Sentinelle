package SentinellePay.Sentinelle.model;


public record TransactionRow(
		String orderStatus,
		String orderDate,
		String lastUpdatedDate,
		String orderId,
		String orderAmount,
		String capturedAmount,
		String accountIdentifier
) {
	public TransactionRow {
		if (accountIdentifier == null || accountIdentifier.length() < 6) {
			throw new IllegalArgumentException(
					"accountIdentifier must be non-null and at least 6 characters to derive BIN"
			);
		}
	}

	public String bin() {
		return accountIdentifier.substring(0, 6);
	}
}

