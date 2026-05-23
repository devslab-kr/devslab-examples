package kr.devslab.examples.easypagingkeyset;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("kr.devslab.examples.easypagingkeyset")
public class EasyPagingKeysetDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyPagingKeysetDemoApplication.class, args);
    }
}
