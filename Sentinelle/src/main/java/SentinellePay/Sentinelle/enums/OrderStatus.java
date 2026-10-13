package SentinellePay.Sentinelle.enums;

import java.util.Locale;
import java.util.Optional;

import lombok.Getter;


public enum OrderStatus {
	CAPTURED("Captured"),
	PARTIALLY_CAPTURED("Partially Captured"),
	REFUNDED("Refunded"),
	PARTIALLY_REFUNDED("Partially Refunded");

	@Getter
	private final String label;

	OrderStatus(String label) {
		this.label = label;
	}

	/**
	 * Parses a CSV value into an {@link OrderStatus}.
	 * <p>
	 * The input is trimmed and upper-cased. If no match is found, this returns {@link Optional#empty()}
	 * (no exception is thrown) so callers can safely skip unknown values.
	 *
	 * @param raw the raw CSV value (may be null)
	 * @return an {@link Optional} containing the matching status, or empty if unknown/blank
	 */
	public static Optional<OrderStatus> fromCsvValue(String raw) {
		if (raw == null) {
			return Optional.empty();
		}

		String normalized = raw.trim();
		if (normalized.isEmpty()) {
			return Optional.empty();
		}
		normalized = normalized.toUpperCase(Locale.ROOT);

		for (OrderStatus status : values()) {
			if (normalized.equals(status.name())) {
				return Optional.of(status);
			}

			String labelUpper = status.label.trim().toUpperCase(Locale.ROOT);
			if (normalized.equals(labelUpper)) {
				return Optional.of(status);
			}
			if (normalized.equals(labelUpper.replace(' ', '_'))) {
				return Optional.of(status);
			}
		}

		return Optional.empty();
	}
}

