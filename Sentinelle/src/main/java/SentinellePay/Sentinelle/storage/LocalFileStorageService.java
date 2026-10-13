package SentinellePay.Sentinelle.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "false", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {
	private static final Path BASE_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "payable");

	@Override
	public String store(byte[] data, String key, String contentType) {
		log.debug("Local store — key={}", key);
		Path target = BASE_DIR.resolve(key);
		try {
			Path parent = target.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			} else {
				Files.createDirectories(BASE_DIR);
			}

			Files.write(target, data);
			return target.toAbsolutePath().toString();
		} catch (IOException e) {
			throw new RuntimeException("Failed to store file locally for key [" + key + "]", e);
		}
	}

	@Override
	public Optional<byte[]> retrieve(String key) {
		log.debug("Local retrieve — key={}", key);
		Path target = BASE_DIR.resolve(key);
		if (!Files.exists(target)) {
			return Optional.empty();
		}

		try {
			return Optional.of(Files.readAllBytes(target));
		} catch (IOException e) {
			throw new RuntimeException("Failed to retrieve file locally for key [" + key + "]", e);
		}
	}

	@Override
	public void delete(String key) {
		log.debug("Local delete — key={}", key);
		Path target = BASE_DIR.resolve(key);
		try {
			Files.deleteIfExists(target);
		} catch (IOException e) {
			throw new RuntimeException("Failed to delete file locally for key [" + key + "]", e);
		}
	}
}

