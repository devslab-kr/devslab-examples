package kr.devslab.examples.ssrfguardspringai.agent;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin HTTP face over {@link FakeLlmService}. Two endpoints:
 *
 * <ul>
 *   <li>{@code POST /agent/chat?message=...} — sends a user message through the
 *       fake LLM, which then drives the {@code fetch_url} tool. The response
 *       includes the full trace: detected URL, tool input, tool output,
 *       and whether the wrap blocked the call.</li>
 *   <li>{@code GET /agent/attacks} — pre-canned attack prompts ready to copy
 *       into the chat endpoint, with the expected outcome documented.</li>
 * </ul>
 */
@RestController
@RequestMapping("/agent")
public class AgentController {

    private final FakeLlmService llm;

    public AgentController(FakeLlmService llm) {
        this.llm = llm;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestParam("message") String message) {
        return llm.chat(message);
    }

    @PostMapping(value = "/chat", consumes = "application/json")
    public Map<String, Object> chatJson(@RequestBody Map<String, String> body) {
        return llm.chat(body.getOrDefault("message", ""));
    }

    @GetMapping("/attacks")
    public Map<String, Object> attacks() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("description",
                "Twelve natural-language prompts that would coax an LLM-powered agent " +
                "into making an SSRF request. Each is blocked by ssrf-guard-springai's " +
                "tool-callback wrap before the underlying fetch_url tool runs.");

        List<Map<String, String>> scenarios = FakeLlmService.attackScenarios().stream()
                .map(prompt -> Map.of(
                        "prompt", prompt,
                        "try", "curl -X POST 'http://localhost:8080/agent/chat?message="
                                + java.net.URLEncoder.encode(prompt, java.nio.charset.StandardCharsets.UTF_8)
                                + "'"
                ))
                .toList();
        root.put("scenarios", scenarios);
        root.put("alsoTry", List.of(
                Map.of("description", "Legitimate prompt — URL is in the whitelist (httpbin.org)",
                        "prompt", "Please fetch https://httpbin.org/get for me",
                        "try", "curl -X POST 'http://localhost:8080/agent/chat?message=Please%20fetch%20https%3A%2F%2Fhttpbin.org%2Fget%20for%20me'")
        ));
        return root;
    }
}
