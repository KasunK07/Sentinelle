package SentinellePay.Sentinelle.storage;

import java.util.Optional;


public interface FileStorageService {

	String store(byte[] data, String key, String contentType);


	Optional<byte[]> retrieve(String key);


	void delete(String key);
}

