package kr.devslab.examples.apilogr2dbc.widget;

import java.time.LocalDateTime;

/**
 * Read-only projection of one row in the {@code api_log} table. The R2DBC
 * backend ships only a writer; this record is the demo's view of what got
 * persisted, intentionally typed so it serializes back over the HTTP read
 * endpoints with camelCase JSON field names.
 *
 * <p>{@code JSONB} columns are exposed as raw {@code String} (already cast to
 * text in the SELECT) — the api-log starter serializes payload/response with
 * Jackson, so they come out as JSON-formatted strings.
 */
public record ApiLogView(
    Long id,
    String eventType,
    String requestId,
    String endpoint,
    String payload,
    String response,
    Integer statusCode,
    String errorMessage,
    LocalDateTime timestamp,
    Integer retryCount,
    Boolean isRetry
) {
}
