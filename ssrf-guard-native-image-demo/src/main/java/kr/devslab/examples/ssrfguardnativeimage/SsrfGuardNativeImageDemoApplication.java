package kr.devslab.examples.ssrfguardnativeimage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Proves that ssrf-guard 3.1.0's GraalVM hints work end-to-end. The app:
 *
 * <ol>
 *   <li>Pulls {@code kr.devslab:ssrf-guard:3.1.0} (which transitively brings
 *       in {@code -restclient}, {@code -httpclient5}, {@code -core}, and the
 *       {@code RuntimeHintsRegistrar} entries each module contributes via
 *       {@code META-INF/spring/aot.factories}).</li>
 *   <li>Wires a {@link RestClient} that has the SSRF interceptor applied
 *       (via the {@code RestClientCustomizer} the {@code -restclient}
 *       autoconfig provides).</li>
 *   <li>Exposes a {@code /fetch} endpoint and an {@code /attacks} catalog —
 *       a stripped-down version of the main {@code ssrf-guard-demo}.</li>
 * </ol>
 *
 * <p>The point isn't the runtime behaviour — that's covered by the other
 * demos. The point is the <b>build</b>: when you run {@code ./gradlew
 * nativeCompile}, GraalVM walks the closed-world model that
 * {@code processAot} produces from the registered hints. If anything is
 * missing, the native image fails to start at runtime with
 * {@code MissingReflectionRegistrationError}. Running this demo's native
 * binary and getting a working {@code /fetch} response is the proof.
 *
 * <p>See the README for the verification flow ({@code processAot} →
 * {@code nativeCompile} → {@code nativeRun}).
 */
@SpringBootApplication
public class SsrfGuardNativeImageDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsrfGuardNativeImageDemoApplication.class, args);
    }

    /**
     * A {@link RestClient.Builder} that ssrf-guard-restclient's
     * {@link RestClientCustomizer} will have already applied its SSRF
     * interceptor to. This bean is just sugar so the controller can inject
     * a {@link RestClient} directly.
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
