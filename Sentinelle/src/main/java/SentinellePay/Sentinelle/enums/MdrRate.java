package SentinellePay.Sentinelle.enums;

import java.math.BigDecimal;

import lombok.Getter;

/**
 * MDR (Merchant Discount Rate) classification and its associated percentage rate.
 * <p>
 * The {@code rate} is stored as a {@link BigDecimal} to avoid floating point precision issues.
 */
public enum MdrRate {
	LOCAL(new BigDecimal("2.2")),
	FOREIGN(new BigDecimal("3.5"));

	@Getter
	private final BigDecimal rate;

	MdrRate(BigDecimal rate) {
		this.rate = rate;
	}

	/**
	 * Returns the MDR rate for the given BIN locality.
	 *
	 * @param isLocal {@code true} for local BINs, {@code false} for foreign BINs
	 * @return {@link #LOCAL} when local, otherwise {@link #FOREIGN}
	 */
	public static MdrRate forBin(boolean isLocal) {
		return isLocal ? LOCAL : FOREIGN;
	}
}

