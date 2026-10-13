package SentinellePay.Sentinelle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS client configuration for S3 integration.
 * <p>
 * This configuration intentionally does not hardcode any credentials. The {@link S3Client}
 * relies on the AWS SDK's default credential provider chain, which typically resolves
 * credentials from (in order) environment variables (e.g. {@code AWS_ACCESS_KEY_ID} /
 * {@code AWS_SECRET_ACCESS_KEY}), system properties, shared credentials/config files,
 * and instance/container metadata (IAM roles) when running in AWS.
 */
@Configuration
@ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "true")
public class AwsConfig {
	@Value("${aws.s3.region}")
	private String region;

	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
				.region(Region.of(region))
				.build();
	}
}

