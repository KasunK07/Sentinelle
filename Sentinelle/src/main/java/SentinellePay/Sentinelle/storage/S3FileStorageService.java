package SentinellePay.Sentinelle.storage;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;


@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "true")
public class S3FileStorageService implements FileStorageService {
	private final S3Client s3Client;

	@Value("${aws.s3.bucket-name}")
	private String bucketName;

	@Override
	public String store(byte[] data, String key, String contentType) {
		log.debug("S3 store — key={}", key);
		try {
			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.contentType(contentType)
					.build();

			s3Client.putObject(request, RequestBody.fromBytes(data));
			return "s3://" + bucketName + "/" + key;
		} catch (S3Exception e) {
			throw new RuntimeException("Failed to store object in S3 for key [" + key + "]", e);
		}
	}

	@Override
	public Optional<byte[]> retrieve(String key) {
		log.debug("S3 retrieve — key={}", key);
		try {
			GetObjectRequest request = GetObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.build();

			ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
			return Optional.of(response.asByteArray());
		} catch (NoSuchKeyException e) {
			return Optional.empty();
		} catch (S3Exception e) {
			throw new RuntimeException("Failed to retrieve object from S3 for key [" + key + "]", e);
		}
	}

	@Override
	public void delete(String key) {
		log.debug("S3 delete — key={}", key);
		try {
			DeleteObjectRequest request = DeleteObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.build();

			s3Client.deleteObject(request);
		} catch (NoSuchKeyException e) {
			// ignore
		} catch (S3Exception e) {
			throw new RuntimeException("Failed to delete object from S3 for key [" + key + "]", e);
		}
	}
}

