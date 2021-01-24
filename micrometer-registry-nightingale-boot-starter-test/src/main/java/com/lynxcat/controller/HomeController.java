package com.lynxcat.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    static final Counter test = Metrics.counter("lynxcat.metrics.test", "country", "china");

    @GetMapping("/home")
    public String home(){
        return "home!";
    }

    @GetMapping("/gc")
    public String gc(){
        System.gc();
        return "doen.";
    }


    @GetMapping("/add")
    public String add(){
        test.increment();
        return "ok!";
    }

}
