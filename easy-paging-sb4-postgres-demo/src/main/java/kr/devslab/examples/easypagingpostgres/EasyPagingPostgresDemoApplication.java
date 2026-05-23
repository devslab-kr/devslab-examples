package kr.devslab.examples.easypagingpostgres;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("kr.devslab.examples.easypagingpostgres")
public class EasyPagingPostgresDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyPagingPostgresDemoApplication.class, args);
    }
}
