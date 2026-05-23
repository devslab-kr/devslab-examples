package kr.devslab.examples.ssrfguardlangchain4j.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The kind of tool every LLM agent ends up with: "given a URL, fetch it and
 * return the text". This is the same {@code requests.get(url).text} pattern
 * you see in every Python LangChain demo — written for Java LangChain4j.
 *
 * <p>By default this would be a wide-open SSRF — the LLM can be coaxed into
 * passing {@code http://169.254.169.254/} (AWS metadata),
 * {@code http://internal-redis:6379/}, or any other private host as the
 * {@code url} argument. The agent dutifully fetches whatever's there and
 * hands the response back to the LLM, which can then exfiltrate it.
 *
 * <p>The demo's defense: <b>this executor is NOT wired with any guard code in
 * its own implementation</b>. Instead, ssrf-guard-langchain4j's autoconfig
 * registers a {@code BeanPostProcessor} that wraps every {@link ToolExecutor}
 * bean it sees in {@code SsrfGuardedToolExecutor}. The wrap parses the JSON
 * tool arguments, finds URL-shaped strings, validates each through the
 * configured {@code UrlPolicy}, and short-circuits with a structured error
 * if any URL is rejected — all before the {@link ToolExecutor#execute} method
 * below runs.
 */
@Configuration
public class FetchUrlTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The {@link ToolSpecification} the LLM sees in its function-calling
     * catalogue. In this demo we deliberately keep the schema simple — the
     * wrap walks the runtime arguments JSON regardless of the declared
     * schema, so the security story doesn't depend on schema details.
     */
    @Bean
    public ToolSpecification fetchUrlSpec() {
        return ToolSpecification.builder()
                .name("fetch_url")
                .description("Fetch the given URL and return its response body. " +
                        "Parameters: { \"url\": string — the URL to fetch (http or https) }")
                .build();
    }

    /**
     * Register the raw executor as a {@link ToolExecutor} bean.
     * ssrf-guard-langchain4j's BeanPostProcessor will pick it up and replace
     * it with a {@code SsrfGuardedToolExecutor} wrapping this one — the
     * agent controller never sees the unwrapped version.
     */
    @Bean
    public ToolExecutor fetchUrlExecutor() {
        return new ToolExecutor() {
            @Override
            public String execute(ToolExecutionRequest request, Object memoryId) {
                // PRETEND fetch. If this method runs at all, the wrapping
                // SsrfGuardedToolExecutor already approved every URL in the
                // request arguments. So in the demo we just echo back what
                // we'd have fetched — no real network IO needed to make the
                // security story clear.
                String arguments = request.arguments();
                try {
                    JsonNode root = MAPPER.readTree(arguments);
                    String url = root.has("url") ? root.get("url").asText() : "(no url field)";
                    return "PRETEND-FETCHED " + url + " — in a real app this would be HTTP body bytes.";
                } catch (Exception e) {
                    return "Failed to parse tool input: " + e.getMessage();
                }
            }
        };
    }
}
