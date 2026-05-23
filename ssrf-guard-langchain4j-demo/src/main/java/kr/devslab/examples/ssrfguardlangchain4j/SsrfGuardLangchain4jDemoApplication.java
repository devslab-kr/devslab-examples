package kr.devslab.examples.ssrfguardlangchain4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sibling of {@code ssrf-guard-springai-demo} — same SSRF story, different
 * LLM framework. The demo simulates a LangChain4j {@code AiServices}-style
 * agent that has a {@code fetch_url} tool implemented as a
 * {@link dev.langchain4j.service.tool.ToolExecutor}. A real LLM would decide
 * when to call the executor based on the user's message; here a
 * {@link kr.devslab.examples.ssrfguardlangchain4j.agent.FakeLlmService} stands
 * in for the LLM so the demo runs offline (no OpenAI / Anthropic / Bedrock
 * key required).
 *
 * <p>The point: <b>every {@code ToolExecutor} bean in this app is wrapped by
 * ssrf-guard-langchain4j automatically</b>. URL-shaped arguments the (fake)
 * LLM passes to {@code fetch_url} are validated against the configured
 * {@code UrlPolicy} before the executor runs. Attacker-supplied URLs come
 * back as a structured JSON error the LLM (or, here, the controller) can
 * interpret — not an unhandled exception that crashes the agent loop.
 */
@SpringBootApplication
public class SsrfGuardLangchain4jDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsrfGuardLangchain4jDemoApplication.class, args);
    }
}
