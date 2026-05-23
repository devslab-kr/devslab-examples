package kr.devslab.examples.ssrfguardspringai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The demo simulates an LLM agent that has a {@code fetch_url} tool. A real
 * LLM would decide when to call the tool based on the user's message; here a
 * {@link kr.devslab.examples.ssrfguardspringai.agent.FakeLlmService} stands
 * in for the LLM so the demo runs offline (no OpenAI / Anthropic / Bedrock
 * key required).
 *
 * <p>The point: <b>every {@code ToolCallback} bean in this app is wrapped by
 * ssrf-guard-springai automatically</b>. URL-shaped arguments the (fake) LLM
 * passes to {@code fetch_url} are validated against the configured
 * {@code UrlPolicy} before the tool runs. Attacker-supplied URLs come back as
 * a structured JSON error the LLM (or, here, the controller) can interpret —
 * not an unhandled exception that crashes the agent loop.
 */
@SpringBootApplication
public class SsrfGuardSpringAiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsrfGuardSpringAiDemoApplication.class, args);
    }
}
