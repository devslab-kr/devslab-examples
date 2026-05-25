package kr.devslab.examples.apilogmybatis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Demo entry point.
 *
 * <p>The {@link MapperScan} targets THIS app's package only. The starter's own
 * {@code ApiLogMapper} (in {@code kr.devslab.apilog.mybatis.mapper}) is wired
 * up by the starter's auto-config - scanning it twice produces conflicting
 * mapper bean definitions.
 */
@SpringBootApplication
@MapperScan("kr.devslab.examples.apilogmybatis")
public class ApiLogMybatisDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiLogMybatisDemoApplication.class, args);
    }
}
