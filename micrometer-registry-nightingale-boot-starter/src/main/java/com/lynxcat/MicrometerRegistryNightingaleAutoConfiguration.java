package com.lynxcat;

import io.micrometer.core.instrument.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import com.lynxcat.service.NightingaleConfigBuilder;


@Configuration
@ConditionalOnClass(NightingaleMeterRegistry.class)
@EnableConfigurationProperties(MicrometerRegistryNightingaleProperties.class)
@ConditionalOnProperty(prefix = "management.metrics.export.nightingale", name = "enabled", havingValue = "true")
@ComponentScan(basePackages = {"com.lynxcat"})
public class MicrometerRegistryNightingaleAutoConfiguration {
	@Autowired
	NightingaleConfigBuilder builder;

	@Bean
	public NightingaleConfig nightingaleConfig() throws Throwable {
		return builder.getNightingaleConfig();
	}

	@Bean
	public NightingaleMeterRegistry nightingaleMeterRegistry(NightingaleConfig nightingaleConfig) throws Throwable {
		return new NightingaleMeterRegistry(nightingaleConfig, Clock.SYSTEM);
	}
}
