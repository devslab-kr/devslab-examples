package kr.devslab.examples.easypagingpostgres.product;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Note this mapper takes a {@code category} parameter but does no pagination
 * itself — same pattern as the H2 offset demo. The starter's aspect adds
 * {@code LIMIT/OFFSET} and {@code ORDER BY} at runtime; PageHelper translates
 * those into PostgreSQL-flavoured SQL underneath.
 */
@Mapper
public interface ProductMapper {

    /**
     * @param category optional category filter; pass {@code null} to return everything
     */
    List<Product> findAll(@Param("category") String category);
}
