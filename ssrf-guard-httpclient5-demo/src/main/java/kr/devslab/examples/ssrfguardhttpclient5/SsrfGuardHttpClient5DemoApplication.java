package kr.devslab.examples.ssrfguardhttpclient5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Demo of {@code ssrf-guard-httpclient5}. Apache HttpClient 5 plugs into the
 * SSRF defenses at <b>DNS-resolution time</b> (not URL-parse time), which is a
 * different shape from the OkHttp / RestClient demos:
 *
 * <ul>
 *   <li>{@link kr.devslab.ssrfguard.httpclient5.SafeDnsResolver} replaces the
 *       default {@code DnsResolver} on the connection manager. Before any
 *       socket opens, it (a) rejects hosts not in the whitelist, and (b)
 *       filters out private / loopback / link-local / cloud-metadata IPs.
 *       The {@code InetAddress[]} it returns is the same array HttpClient
 *       hands to {@code Socket.connect}, closing the TOCTOU window.</li>
 *   <li>{@link kr.devslab.ssrfguard.httpclient5.SafeRedirectStrategy} runs the
 *       same policy on every redirect hop — scheme check + DNS gate.</li>
 * </ul>
 *
 * <p>Everything below is auto-wired by {@code SsrfGuardHttpClient5AutoConfiguration}
 * from the {@code ssrf-guard-httpclient5} module — when this Spring Boot app
 * starts, a guarded {@link org.apache.hc.client5.http.impl.classic.CloseableHttpClient}
 * appears in the context. The controller just injects it. No wiring code in
 * this demo's main().
 *
 * <p>See the README for the equivalent non-Spring wiring (5 lines on
 * {@code HttpClients.custom()}).
 */
@SpringBootApplication
public class SsrfGuardHttpClient5DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsrfGuardHttpClient5DemoApplication.class, args);
    }
}
