package org.example.spartacafe;

import org.springframework.boot.SpringApplication;

public class TestSpartaCafeApplication {

    public static void main(String[] args) {
        SpringApplication.from(SpartaCafeApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
