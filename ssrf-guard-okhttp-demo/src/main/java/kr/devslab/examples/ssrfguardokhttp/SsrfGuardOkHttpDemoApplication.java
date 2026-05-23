package kr.devslab.examples.ssrfguardokhttp;

import kr.devslab.ssrfguard.core.HostPolicy;
import kr.devslab.ssrfguard.core.NoOpSsrfGuardMetrics;
import kr.devslab.ssrfguard.core.SsrfGuardProperties;
import kr.devslab.ssrfguard.core.UrlPolicy;
import kr.devslab.ssrfguard.okhttp.SsrfGuardOkHttpDns;
import kr.devslab.ssrfguard.okhttp.SsrfGuardOkHttpInterceptor;
import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Demo of {@code ssrf-guard-okhttp}. OkHttp has two extension points the
 * library uses:
 *
 * <ul>
 *   <li>{@link okhttp3.Interceptor} — runs the {@link UrlPolicy} on every
 *       request URL before dispatch (URL-time gate).</li>
 *   <li>{@link okhttp3.Dns} — DNS-time gate; refuses to resolve hosts not
 *       in the whitelist and filters private IPs from the resolution result.</li>
 * </ul>
 *
 * <p>Both go onto the {@link OkHttpClient.Builder} — three lines of wiring.
 */
@SpringBootApplication
@EnableConfigurationProperties(SsrfGuardProperties.class)
public class SsrfGuardOkHttpDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsrfGuardOkHttpDemoApplication.class, args);
    }

    @Bean
    public OkHttpClient okHttpClient(SsrfGuardProperties props) {
        HostPolicy hostPolicy = new HostPolicy(props.getExactHosts(), props.getSuffixes());
        UrlPolicy urlPolicy = new UrlPolicy(
                props.getAllowedSchemes(),
                props.getAllowedPorts(),
                hostPolicy,
                props.isRejectIpLiteralHosts(),
                props.isRejectUserInfo(),
                NoOpSsrfGuardMetrics.INSTANCE
        );

        return new OkHttpClient.Builder()
                .addInterceptor(new SsrfGuardOkHttpInterceptor(urlPolicy))
                .dns(new SsrfGuardOkHttpDns(hostPolicy, props.isBlockPrivateNetworks()))
                // followRedirects gates the second-hop URL through the
                // interceptor again, so the SSRF-Guard policy covers the
                // whole redirect chain.
                .followRedirects(props.isFollowRedirects())
                .followSslRedirects(props.isFollowRedirects())
                .build();
    }
}
