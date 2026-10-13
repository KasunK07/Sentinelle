package SentinellePay.Sentinelle.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import SentinellePay.Sentinelle.enums.MdrRate;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class MdrCalculationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * Calculates fee and net amount for the given gross amount and explicit MDR rate.
     *
     * @param grossAmount the captured (gross) transaction amount — must not be null or negative
     * @param rate        the applicable MDR rate — must not be null
     * @return {@link CalculationResult} containing the rounded fee and net amount
     * @throws IllegalArgumentException if grossAmount is null, negative, or rate is null
     */
    public CalculationResult calculate(BigDecimal grossAmount, MdrRate rate) {
        validateInputs(grossAmount, rate);

        BigDecimal fee = grossAmount
                .multiply(rate.getRate())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        BigDecimal net = grossAmount.subtract(fee);

        log.debug("MDR calc — gross: {}, rate: {}%, fee: {}, net: {}",
                grossAmount, rate.getRate(), fee, net);

        return new CalculationResult(fee, net);
    }

    /**
     * Convenience overload that resolves the MDR rate from BIN locality
     * and delegates to {@link #calculate(BigDecimal, MdrRate)}.
     *
     * @param grossAmount the captured transaction amount
     * @param isLocalBin  {@code true} if the BIN exists in the local BIN master
     * @return {@link CalculationResult} with fee and net
     */
    public CalculationResult calculate(BigDecimal grossAmount, boolean isLocalBin) {
        return calculate(grossAmount, MdrRate.forBin(isLocalBin));
    }


    private void validateInputs(BigDecimal grossAmount, MdrRate rate) {
        if (grossAmount == null) {
            throw new IllegalArgumentException("grossAmount must not be null");
        }
        if (grossAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "grossAmount must not be negative, got: " + grossAmount);
        }
        if (rate == null) {
            throw new IllegalArgumentException("MdrRate must not be null");
        }
    }

    public record CalculationResult(BigDecimal feeAmount, BigDecimal netAmount) {}
}