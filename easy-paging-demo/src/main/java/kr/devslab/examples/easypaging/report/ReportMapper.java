package kr.devslab.examples.easypaging.report;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * Notice this mapper has no {@code LIMIT} / {@code OFFSET} / {@code COUNT(*)} —
 * {@link kr.devslab.easypaging.annotation.AutoPaginate} on the controller method
 * sets up PageHelper's per-thread state before this query runs, and PageHelper
 * rewrites the SQL underneath.
 */
@Mapper
public interface ReportMapper {

    List<Report> findAll();
}
