package kr.devslab.examples.ssrfguardfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * A {@code @FeignClient} pointing at a host that's NOT in the whitelist.
 * The ssrf-guard interceptor will reject every call here before it leaves
 * the JVM. Demo only — you'd never write this in production.
 */
@FeignClient(name = "evil", url = "https://evil.com")
public interface EvilClient {

    @GetMapping("/")
    String hit();
}
