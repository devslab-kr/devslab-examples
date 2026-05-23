package kr.devslab.examples.easypagingkeyset.location;

import java.time.Instant;
import java.util.UUID;

/**
 * A single GPS ping for a field worker — the kind of unbounded time-series
 * row where keyset (cursor) pagination earns its keep. With millions of rows
 * per worker, traditional {@code OFFSET}-based paging gets slower the deeper
 * the client scrolls, and {@code COUNT(*)} is wasted work for an
 * append-only stream.
 *
 * <p>The composite keyset {@code (time DESC, id DESC)} guarantees a stable
 * ordering even when two pings share the same timestamp.
 */
public class Location {

    private Long id;
    private UUID workerId;
    private Instant time;
    private Double lat;
    private Double lng;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getWorkerId() {
        return workerId;
    }

    public void setWorkerId(UUID workerId) {
        this.workerId = workerId;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }
}
