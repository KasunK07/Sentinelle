package SentinellePay.Sentinelle.exception;

/**
 * Thrown when a requested operation requires the BIN master data set,
 * but it has not been uploaded/loaded yet.
 */
public class BinMasterNotLoadedException extends RuntimeException {
	private static final String DEFAULT_MESSAGE =
			"BIN master has not been loaded. Call POST /api/v1/bin-master/upload first.";

	public BinMasterNotLoadedException() {
		super(DEFAULT_MESSAGE);
	}
}

