package kr.devslab.examples.easypagingkeyset.location;

import java.util.UUID;
import kr.devslab.easypaging.annotation.KeysetPaginate;
import kr.devslab.easypaging.core.KeysetPage;
import kr.devslab.easypaging.core.KeysetRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Composite-key cursor pagination: {@code (time DESC, id DESC)}.
 *
 * <p>The {@link KeysetRequest} parameter is filled by the starter's argument
 * resolver from the {@code cursor} / {@code size} / {@code direction} query
 * params, falling back to the defaults declared in the annotation.
 *
 * <p>Sample flow (once the app is running):
 * <pre>
 *   # First page — no cursor.
 *   curl 'http://localhost:8080/locations?workerId=00000000-0000-0000-0000-000000000001&amp;size=10'
 *
 *   # Pass back the response's nextCursor as ?cursor=... for the next page.
 *   curl 'http://localhost:8080/locations?workerId=...&amp;size=10&amp;cursor=eyJrIjp7InRpbWUiOi...'
 *
 *   # Walking nextCursor until hasNext=false yields the whole stream — no OFFSET,
 *   # no COUNT(*), and the result set is stable even if rows are inserted while
 *   # the client is paging.
 * </pre>
 */
@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locations;

    public LocationController(LocationService locations) {
        this.locations = locations;
    }

    @GetMapping
    @KeysetPaginate(
            keys        = {"time", "id"},
            direction   = "DESC",
            defaultSize = 50,
            maxSize     = 200)
    public KeysetPage<Location> stream(KeysetRequest req, @RequestParam UUID workerId) {
        return locations.stream(workerId, req);
    }
}
