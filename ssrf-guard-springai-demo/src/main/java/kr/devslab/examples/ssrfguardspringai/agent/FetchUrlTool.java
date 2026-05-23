package kr.devslab.examples.ssrfguardspringai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.DefaultToolMetadata;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

/**
 * The kind of tool every LLM agent ends up with: "given a URL, fetch it and
 * return the text". This is the {@code requests.get(url).text} you see in
 * every Python LangChain demo, the same pattern in Java/Spring AI.
 *
 * <p>By default this would be a wide-open SSRF — the LLM can be coaxed into
 * passing {@code http://169.254.169.254/} (AWS metadata),
 * {@code http://internal-redis:6379/}, or any other private host as the
 * {@code url} argument. The agent dutifully fetches whatever's there and
 * hands the response back to the LLM, which can then exfiltrate it.
 *
 * <p>The demo's defense: <b>this tool is NOT wired with any guard code in
 * its own implementation</b>. Instead, ssrf-guard-springai's autoconfig
 * registers a {@code BeanPostProcessor} that wraps every {@code ToolCallback}
 * bean it sees in {@code SsrfGuardedToolCallback}. The wrap parses the JSON
 * tool input, finds URL-shaped strings, validates each through the
 * configured {@code UrlPolicy}, and short-circuits with a structured error
 * if any URL is rejected — all before the {@code call()} method below runs.
 */
@Configuration
public class FetchUrlTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Register the raw tool as a {@link ToolCallback} bean. ssrf-guard-springai's
     * BeanPostProcessor will pick it up and replace it with a
     * {@code SsrfGuardedToolCallback} wrapping this one — the agent
     * controller never sees the unwrapped version.
     */
    // Bean name "fetchUrlCallback" — intentionally different from the
    // @Configuration class name so we don't trip the same-name override
    // check that fires when both the class AND the factory method want to
    // be registered under "fetchUrlTool".
    @Bean
    public ToolCallback fetchUrlCallback() {
        return new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name("fetch_url")
                        .description("Fetch the given URL and return its response body.")
                        .inputSchema("""
                                {
                                  "type": "object",
                                  "properties": {
                                    "url": {
                                      "type": "string",
                                      "description": "The URL to fetch (http or https)"
                                    }
                                  },
                                  "required": ["url"]
                                }
                                """)
                        .build();
            }

            @Override
            public ToolMetadata getToolMetadata() {
                return DefaultToolMetadata.builder().build();
            }

            @Override
            public String call(String toolInput) {
                // PRETEND fetch. If this method runs at all, the wrapping
                // SsrfGuardedToolCallback already approved every URL in the
                // input. So in the demo we just echo back what we'd have
                // fetched — no real network IO needed to make the security
                // story clear.
                try {
                    JsonNode root = MAPPER.readTree(toolInput);
                    String url = root.has("url") ? root.get("url").asText() : "(no url field)";
                    return "PRETEND-FETCHED " + url + " — in a real app this would be HTTP body bytes.";
                } catch (Exception e) {
                    return "Failed to parse tool input: " + e.getMessage();
                }
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                return call(toolInput);
            }
        };
    }
}
