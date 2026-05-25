package kr.devslab.examples.apilogmybatis.widget;

import kr.devslab.apilog.mybatis.model.ApiLogRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Demo-side reader queries against {@code api_log}.
 *
 * <p>The starter's built-in {@link kr.devslab.apilog.mybatis.mapper.ApiLogMapper}
 * only exposes {@code insert} (used by the writer) and {@code findByRequestId}
 * (correlation lookup). For "recent N" / "by event type" reads the demo needs
 * its own mapper - we keep it in the demo so the starter's API surface stays
 * minimal (consumers add only what they actually query).
 */
@Mapper
public interface ApiLogQueryMapper {
    List<ApiLogRow> findRecent(int limit);
    List<ApiLogRow> findByEvent(String eventType);
}
