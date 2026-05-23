package kr.devslab.examples.easypagingkeyset.location;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Unlike the offset demo (where the mapper has no pagination logic), keyset
 * SQL is explicit: the {@code WHERE} clause uses the cursor values to
 * "seek past" the previously-returned rows. The service fetches {@code size+1}
 * to detect whether a next page exists.
 */
@Mapper
public interface LocationMapper {

    /**
     * @param time  cursor timestamp; {@code null} on the first page
     * @param id    cursor id (tiebreaker for equal timestamps); {@code null} on the first page
     * @param limit page size + 1 (the extra row tells us whether more pages exist)
     */
    List<Location> findAfter(
            @Param("workerId") UUID workerId,
            @Param("time")     Instant time,
            @Param("id")       Long id,
            @Param("limit")    int limit);
}
