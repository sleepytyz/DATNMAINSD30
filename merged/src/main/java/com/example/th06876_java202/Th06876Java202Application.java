package com.example.th06876_java202;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class Th06876Java202Application {

    public static void main(String[] args) {
        // [MERGE] Fix loi Invalid value for NanoOfSecond khi doc cot TIME.
        // Gio cham cong lay qua lop GioVN de dung gio Viet Nam.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(Th06876Java202Application.class, args);
    }
}