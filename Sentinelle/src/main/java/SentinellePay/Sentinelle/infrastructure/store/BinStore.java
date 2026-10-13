package SentinellePay.Sentinelle.infrastructure.store;

import java.util.Set;

/**
 * Contract for the in-memory BIN (Bank Identification Number) store.
 * Implementations must be thread-safe — load() and isLocal() may be called
 * from different threads.
 */
public interface BinStore {

    void load(Set<String> binCodes);

    boolean isLocal(String bin);

    int size();

    boolean isLoaded();
    
    void clear();
}