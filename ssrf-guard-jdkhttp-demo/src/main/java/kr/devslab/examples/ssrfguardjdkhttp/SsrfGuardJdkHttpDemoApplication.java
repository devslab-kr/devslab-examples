package kr.devslab.examples.ssrfguardjdkhttp;

import kr.devslab.ssrfguard.core.HostPolicy;
import kr.devslab.ssrfguard.core.NoOpSsrfGuardMetrics;
import kr.devslab.ssrfguard.core.SsrfGuardProperties;
import kr.devslab.ssrfguard.core.UrlPolicy;
import kr.devslab.ssrfguard.jdkhttp.SsrfGuardedHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.net.http.HttpClient;

/**
 * Demo of {@code ssrf-guard-jdkhttp}. Unlike the other modules, this one
 * has no Spring autoconfig — it's a thin wrapper over
 * {@link java.net.http.HttpClient}. So we wire the wrapper ourselves and
 * publish it as a bean.
 *
 * <p>The wiring below is the entire "how do I use this without Spring"
 * story — three Java lines:
 * <ol>
 *   <li>Build a {@link HostPolicy} from the configured whitelist.</li>
 *   <li>Build a {@link UrlPolicy} from the rest of the properties.</li>
 *   <li>Wrap a stock {@link HttpClient} with {@link SsrfGuardedHttpClient}.</li>
 * </ol>
 */
@SpringBootApplication
@EnableConfigurationProperties(SsrfGuardProperties.class)
public class SsrfGuardJdkHttpDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsrfGuardJdkHttpDemoApplication.class, args);
    }

    @Bean
    public SsrfGuardedHttpClient ssrfGuardedHttpClient(SsrfGuardProperties props) {
        // Build the policy from the configured properties. In a non-Spring
        // app you'd either hardcode the whitelist or read it from your own
        // config source — but the policy types (HostPolicy, UrlPolicy) are
        // POJOs, no Spring required.
        HostPolicy hostPolicy = new HostPolicy(props.getExactHosts(), props.getSuffixes());
        UrlPolicy urlPolicy = new UrlPolicy(
                props.getAllowedSchemes(),
                props.getAllowedPorts(),
                hostPolicy,
                props.isRejectIpLiteralHosts(),
                props.isRejectUserInfo(),
                NoOpSsrfGuardMetrics.INSTANCE
        );

        // Wrap any HttpClient — Java 11+ default builder is fine, but you
        // could also pass a HttpClient configured with proxy / TLS / HTTP2
        // settings. The guard wraps the SEND path, not the construction.
        return new SsrfGuardedHttpClient(
                HttpClient.newBuilder().build(),
                urlPolicy,
                props.isBlockPrivateNetworks()
        );
    }
}
