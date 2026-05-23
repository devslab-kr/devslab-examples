package kr.devslab.examples.ssrfguardfeign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Demo of {@code ssrf-guard-feign}. A single {@code @FeignClient} interface
 * declares a remote API; the ssrf-guard auto-config registers a
 * {@code feign.RequestInterceptor} that validates every URL the Feign
 * dispatcher resolves against the {@code UrlPolicy} before the underlying
 * HTTP call.
 */
@SpringBootApplication
@EnableFeignClients
public class SsrfGuardFeignDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsrfGuardFeignDemoApplication.class, args);
    }
}
