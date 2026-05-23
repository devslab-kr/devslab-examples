package kr.devslab.examples.easypaging;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("kr.devslab.examples.easypaging")
public class EasyPagingDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyPagingDemoApplication.class, args);
    }
}
