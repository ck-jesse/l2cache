package com.jd.platform.hotkey.dashboard;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableAsync
@EnableScheduling
@SpringBootApplication
public class DashboardApplication implements CommandLineRunner {

    private Logger logger = LoggerFactory.getLogger(getClass());


    public static void main(String[] args) {
        try {
            SpringApplication.run(DashboardApplication.class, args);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public void run(String... args)  {
    }

}
