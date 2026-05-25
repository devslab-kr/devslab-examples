package kr.devslab.examples.apilogr2dbc.widget;

import java.time.LocalDateTime;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * Reads from the {@code api_log} table — the api-log R2DBC backend only ships a
 * writer, so the demo brings its own reader to expose what the starter wrote.
 *
 * <p>Uses {@link DatabaseClient} (autoconfigured by
 * {@code spring-boot-starter-data-r2dbc}) instead of {@code R2dbcEntityTemplate}
 * because the table isn't ours — we don't want to declare a managed entity for a
 * table whose schema is owned by the starter, and {@code DatabaseClient} is the
 * lighter tool for ad-hoc SELECTs.
 *
 * <p>The {@code JSONB} columns are cast to {@code text} in the SELECT so they
 * bind cleanly into {@code String} fields without an explicit codec for the
 * driver's PGobject type.
 */
@Service
public class ApiLogReader {

    private static final String SELECT_COLS =
        "SELECT id, event_type, request_id, endpoint, " +
        "payload::text AS payload, response::text AS response, " +
        "status_code, error_message::text AS error_message, " +
        "timestamp, retry_count, is_retry " +
        "FROM api_log ";

    private final DatabaseClient databaseClient;

    public ApiLogReader(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Flux<ApiLogView> findByRequestId(String requestId) {
        return databaseClient.sql(SELECT_COLS + "WHERE request_id = :rid ORDER BY id ASC")
            .bind("rid", requestId)
            .map(ApiLogReader::mapRow)
            .all();
    }

    public Flux<ApiLogView> findRecent(int limit) {
        return databaseClient.sql(SELECT_COLS + "ORDER BY timestamp DESC, id DESC LIMIT :lim")
            .bind("lim", limit)
            .map(ApiLogReader::mapRow)
            .all();
    }

    public Flux<ApiLogView> findByEvent(String eventType) {
        return databaseClient.sql(SELECT_COLS + "WHERE event_type = :etype ORDER BY id ASC")
            .bind("etype", eventType)
            .map(ApiLogReader::mapRow)
            .all();
    }

    private static ApiLogView mapRow(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata meta) {
        return new ApiLogView(
            row.get("id", Long.class),
            row.get("event_type", String.class),
            row.get("request_id", String.class),
            row.get("endpoint", String.class),
            row.get("payload", String.class),
            row.get("response", String.class),
            row.get("status_code", Integer.class),
            row.get("error_message", String.class),
            row.get("timestamp", LocalDateTime.class),
            row.get("retry_count", Integer.class),
            row.get("is_retry", Boolean.class)
        );
    }
}
