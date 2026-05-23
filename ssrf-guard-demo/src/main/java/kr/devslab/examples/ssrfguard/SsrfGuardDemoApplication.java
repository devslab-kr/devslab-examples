package kr.devslab.examples.ssrfguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The whole demo lives in three controllers — one per HTTP client integration:
 * {@code FetchController} (RestClient), {@code FetchResttemplateController}
 * (RestTemplate), {@code FetchWebClientController} (WebClient). All three
 * share the same {@code UrlPolicy} bean wired up by ssrf-guard's
 * auto-configuration, so changing {@code ssrf.guard.*} in application.yml
 * affects every client uniformly.
 *
 * <p>Run with {@code ./gradlew bootRun} and try the curls in {@code README.md}.
 */
@SpringBootApplication
public class SsrfGuardDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsrfGuardDemoApplication.class, args);
    }
}
