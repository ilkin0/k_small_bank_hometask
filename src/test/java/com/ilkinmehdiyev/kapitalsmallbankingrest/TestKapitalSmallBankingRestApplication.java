package com.ilkinmehdiyev.kapitalsmallbankingrest;

import org.springframework.boot.SpringApplication;

public class TestKapitalSmallBankingRestApplication {

    public static void main(String[] args) {
        SpringApplication.from(KapitalSmallBankingRestApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
