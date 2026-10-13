package SentinellePay.Sentinelle.enums;


public enum RowType {
	PAYMENT,
	REFUND;


	public static RowType resolveFromOrderStatus(OrderStatus status) {
		return switch (status) {
			case CAPTURED, PARTIALLY_CAPTURED -> PAYMENT;
			case REFUNDED, PARTIALLY_REFUNDED -> REFUND;
		};
	}
}

