package kr.devslab.examples.easypagingkeyset.location;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.core.KeysetPage;
import kr.devslab.easypaging.core.KeysetRequest;
import org.springframework.stereotype.Service;

/**
 * The service owns the "size + 1" fetch and the {@code keyExtractor} that
 * tells {@link KeysetPage#build} which fields of the last visible row become
 * the {@code nextCursor}. The {@link CursorCodec} bean is registered by the
 * starter's auto-configuration.
 */
@Service
public class LocationService {

    private final LocationMapper mapper;
    private final CursorCodec codec;

    public LocationService(LocationMapper mapper, CursorCodec codec) {
        this.mapper = mapper;
        this.codec = codec;
    }

    public KeysetPage<Location> stream(UUID workerId, KeysetRequest req) {
        List<Location> rows = mapper.findAfter(
                workerId,
                req.keyAsInstant("time"),   // null on the first page
                req.keyAsLong("id"),        // null on the first page
                req.size() + 1);            // +1 lets KeysetPage.build set hasNext correctly

        return KeysetPage.build(rows, req, row -> Map.of(
                "time", row.getTime(),
                "id",   row.getId()
        ), codec);
    }
}
