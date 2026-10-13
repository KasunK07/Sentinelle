package SentinellePay.Sentinelle.infrastructure.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe in-memory implementation of {@link BinStore}.
 *
 * <p>Thread-safety strategy:
 * <ul>
 *   <li>The backing {@code HashSet} is declared {@code volatile} — any thread
 *       reading {@code binCodes} after a {@link #load} call is guaranteed to
 *       see the new reference, not a stale cached one.</li>
 *   <li>{@link #load} replaces the entire set reference atomically rather than
 *       mutating the existing set — this prevents readers from ever observing a
 *       partially-populated collection.</li>
 *   <li>{@link #isLoaded} uses an {@link AtomicBoolean} for safe cross-thread
 *       visibility without needing {@code synchronized}.</li>
 * </ul>
 *
 * @ThreadSafe
 */
@Slf4j
@Component
public class InMemoryBinStore implements BinStore {

    private volatile Set<String> binCodes = new HashSet<>();
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    @Override
    public void load(Set<String> newBinCodes) {
        this.binCodes = new HashSet<>(newBinCodes);
        this.loaded.set(true);
        log.info("BIN store loaded successfully with {} codes", this.binCodes.size());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Trims the input before lookup to guard against stray whitespace
     * that may survive CSV/Excel parsing.
     */
    @Override
    public boolean isLocal(String bin) {
        return bin != null && binCodes.contains(bin.trim());
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return binCodes.size();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLoaded() {
        return loaded.get();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Replaces the backing set with a fresh empty one (volatile write)
     * and resets the loaded flag.
     */
    @Override
    public void clear() {
        binCodes = new HashSet<>();
        loaded.set(false);
        log.info("BIN store cleared");
    }
}