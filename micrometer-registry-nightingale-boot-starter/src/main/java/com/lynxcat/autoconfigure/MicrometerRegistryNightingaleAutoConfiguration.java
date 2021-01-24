package com.lynxcat.autoconfigure;


import com.lynxcat.NightingaleConfig;
import com.lynxcat.NightingaleMeterRegistry;
import io.micrometer.core.instrument.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnClass(NightingaleMeterRegistry.class)
@EnableConfigurationProperties(MicrometerRegistryNightingaleProperties.class)
@ConditionalOnProperty(prefix = "management.metrics.export.nightingale", name = "enabled", havingValue = "true")
public class MicrometerRegistryNightingaleAutoConfiguration {
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
