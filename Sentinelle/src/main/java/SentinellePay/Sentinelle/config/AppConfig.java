package SentinellePay.Sentinelle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Application-wide configuration beans.
 */
@Configuration
public class AppConfig {
	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("SentinellePay API")
						.description("BIN-based MDR calculation and CSV transformation service")
						.version("1.0.0")
						.contact(new Contact().name("PAYable IT")))
				.addTagsItem(new Tag().name("BIN Master"))
				.addTagsItem(new Tag().name("Transactions"));
	}
}

