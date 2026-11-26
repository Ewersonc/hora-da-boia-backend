package br.com.horadaboia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HoraDaBoiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoraDaBoiaApplication.class, args);
    }
}
