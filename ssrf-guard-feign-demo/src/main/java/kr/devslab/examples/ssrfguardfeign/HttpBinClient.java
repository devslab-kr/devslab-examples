package kr.devslab.examples.ssrfguardfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * A typical Feign client — declarative, type-safe HTTP. The URL is
 * configured by name="httpbin" + url="..."; ssrf-guard's RequestInterceptor
 * sees the same URL the dispatcher would dial and validates it against the
 * policy first.
 */
@FeignClient(name = "httpbin", url = "https://httpbin.org")
public interface HttpBinClient {

    @GetMapping("/get")
    String get();
}
