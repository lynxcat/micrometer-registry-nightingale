package com.lynxcat;

import io.micrometer.core.instrument.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class NightingaleMetricsConfig {

    @Autowired
    Environment environment;


    @Bean
    public NightingaleConfig nightingaleConfig() {
        return new NightingaleConfig() {
            @Override
            public String get(String key) {
                return environment.getProperty("management.metrics.export."+key);
            }
        };
    }

    @Bean
    public NightingaleMeterRegistry nightingaleMeterRegistry(NightingaleConfig nightingaleConfig, Clock clock){
        return new NightingaleMeterRegistry(nightingaleConfig, clock);
    }

}
