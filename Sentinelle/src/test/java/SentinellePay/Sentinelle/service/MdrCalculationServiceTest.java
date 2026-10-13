package SentinellePay.Sentinelle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import SentinellePay.Sentinelle.enums.MdrRate;

/**
 * Unit tests for {@link MdrCalculationService}.
 */
@ExtendWith(MockitoExtension.class)
class MdrCalculationServiceTest {
	private final MdrCalculationService service = new MdrCalculationService();

	@Test
	@DisplayName("Local BIN: gross=1000.00 → fee=22.00, net=978.00")
	void localBin_1000_fee22_net978() {
		MdrCalculationService.CalculationResult result =
				service.calculate(new BigDecimal("1000.00"), MdrRate.LOCAL);

		assertThat(result.feeAmount()).isEqualByComparingTo("22.00");
		assertThat(result.netAmount()).isEqualByComparingTo("978.00");
	}

	@Test
	@DisplayName("Foreign BIN: gross=1000.00 → fee=35.00, net=965.00")
	void foreignBin_1000_fee35_net965() {
		MdrCalculationService.CalculationResult result =
				service.calculate(new BigDecimal("1000.00"), MdrRate.FOREIGN);

		assertThat(result.feeAmount()).isEqualByComparingTo("35.00");
		assertThat(result.netAmount()).isEqualByComparingTo("965.00");
	}

	@Test
	@DisplayName("HALF_UP rounding: gross=100.00, LOCAL → fee=2.20, net=97.80")
	void rounding_100_local_fee220_net9780() {
		MdrCalculationService.CalculationResult result =
				service.calculate(new BigDecimal("100.00"), MdrRate.LOCAL);

		assertThat(result.feeAmount()).isEqualByComparingTo("2.20");
		assertThat(result.netAmount()).isEqualByComparingTo("97.80");
	}

	@Test
	@DisplayName("HALF_UP rounding edge: gross=100.01, LOCAL → fee=2.20, net=97.81")
	void roundingEdge_10001_local_fee220_net9781() {
		MdrCalculationService.CalculationResult result =
				service.calculate(new BigDecimal("100.01"), MdrRate.LOCAL);

		assertThat(result.feeAmount()).isEqualByComparingTo("2.20");
		assertThat(result.netAmount()).isEqualByComparingTo("97.81");
	}

	@Test
	@DisplayName("Large amount with commas after stripping: 21,160.00 → 21160.00, LOCAL → fee=465.52, net=20694.48")
	void largeAmountWithCommas_stripped_local_fee46552_net2069448() {
		BigDecimal gross = new BigDecimal("21,160.00".replace(",", ""));

		MdrCalculationService.CalculationResult result =
				service.calculate(gross, MdrRate.LOCAL);

		assertThat(result.feeAmount()).isEqualByComparingTo("465.52");
		assertThat(result.netAmount()).isEqualByComparingTo("20694.48");
	}

	@Test
	@DisplayName("Null grossAmount → throws IllegalArgumentException")
	void nullGross_throws() {
		assertThatThrownBy(() -> service.calculate(null, MdrRate.LOCAL))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("grossAmount must not be null");
	}

	@Test
	@DisplayName("Negative grossAmount → throws IllegalArgumentException")
	void negativeGross_throws() {
		assertThatThrownBy(() -> service.calculate(new BigDecimal("-0.01"), MdrRate.LOCAL))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("grossAmount must not be negative");
	}

	@Test
	@DisplayName("Null rate → throws IllegalArgumentException")
	void nullRate_throws() {
		assertThatThrownBy(() -> service.calculate(new BigDecimal("10.00"), null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("MdrRate must not be null");
	}

	@Test
	@DisplayName("Zero amount → fee=0.00, net=0.00")
	void zeroAmount_fee0_net0() {
		MdrCalculationService.CalculationResult result =
				service.calculate(new BigDecimal("0.00"), MdrRate.LOCAL);

		assertThat(result.feeAmount()).isEqualByComparingTo("0.00");
		assertThat(result.netAmount()).isEqualByComparingTo("0.00");
	}
}

