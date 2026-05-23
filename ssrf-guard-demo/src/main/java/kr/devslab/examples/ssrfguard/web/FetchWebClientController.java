package kr.devslab.examples.ssrfguard.web;

import kr.devslab.ssrfguard.core.SsrfGuardException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Reactive variant of the demo, backed by {@link WebClient}.
 *
 * <p>The {@code ssrf-guard-webclient} module installs an
 * {@code ExchangeFilterFunction} onto the autoconfigured
 * {@code WebClient.Builder}. Policy violations come back as
 * {@code Mono.error(SsrfGuardException)} — the controller catches them
 * inside the reactive chain via {@code onErrorResume} and returns a
 * structured response (so the JSON output shape matches the other two
 * controllers exactly).
 *
 * <p>Try:
 * <pre>
 *   curl 'http://localhost:8080/fetch-webclient?url=https://httpbin.org/get'
 *   curl 'http://localhost:8080/fetch-webclient?url=http://169.254.169.254/'
 * </pre>
 */
@RestController
@RequestMapping("/fetch-webclient")
public class FetchWebClientController {

    private final WebClient webClient;

    public FetchWebClientController(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @GetMapping
    public Mono<Map<String, Object>> fetch(@RequestParam String url) {
        // Map.<String, Object>of(...) explicitly types the map so Mono.map
        // can infer a Mono<Map<String, Object>> result. Without the witness,
        // javac picks Map<String, String> from the string-only values and
        // the chain stops compiling.
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .<Map<String, Object>>map(body -> Map.of(
                        "status", "allowed",
                        "client", "WebClient",
                        "url", url,
                        "bodyPreview", preview(body)
                ))
                .onErrorResume(SsrfGuardException.class, e -> Mono.just(Map.<String, Object>of(
                        "status", "blocked",
                        "client", "WebClient",
                        "url", url,
                        "reason", e.reason().label(),
                        "message", e.getMessage()
                )))
                .onErrorResume(throwable -> {
                    // Unwrap nested SsrfGuardException if Mono wrapped it.
                    Throwable root = throwable;
                    while (root.getCause() != null && root != root.getCause()) {
                        if (root.getCause() instanceof SsrfGuardException sg) {
                            return Mono.just(Map.<String, Object>of(
                                    "status", "blocked",
                                    "client", "WebClient",
                                    "url", url,
                                    "reason", sg.reason().label(),
                                    "message", sg.getMessage()
                            ));
                        }
                        root = root.getCause();
                    }
                    return Mono.just(Map.<String, Object>of(
                            "status", "error",
                            "client", "WebClient",
                            "url", url,
                            "error", throwable.getClass().getSimpleName(),
                            "message", String.valueOf(throwable.getMessage())
                    ));
                });
    }

    private static String preview(String body) {
        if (body == null) return null;
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}
