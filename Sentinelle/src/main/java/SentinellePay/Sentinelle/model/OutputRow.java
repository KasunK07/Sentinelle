package SentinellePay.Sentinelle.model;

import java.math.BigDecimal;
import java.util.Locale;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Final transformed output row to be written to the output CSV.
 * <p>
 * Provides {@link #toCsvRow()} to render values in the required column order.
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class OutputRow {
	public static final String LKR_CURRENCY = "LKR";

	private final String rowType;
	private final String creationDate;
	private final String approvedDate;
	private final String transactionId;
	private final BigDecimal requestAmount;
	private final BigDecimal grossLocalAmount;

	@Builder.Default
	private final String localCurrency = LKR_CURRENCY;

	private final BigDecimal feeLocalAmount;
	private final BigDecimal netLocalAmount;

	public String[] toCsvRow() {
		return new String[] {
				rowType,
				creationDate,
				approvedDate,
				transactionId,
				formatAmount(requestAmount),
				formatAmount(grossLocalAmount),
				localCurrency,
				formatAmount(feeLocalAmount),
				formatAmount(netLocalAmount)
		};
	}

	private static String formatAmount(BigDecimal amount) {
		if (amount == null) {
			return "";
		}
		return String.format(Locale.ROOT, "%.2f", amount.doubleValue());
	}
}

