package com.lynxcat;

import io.micrometer.core.instrument.config.validate.Validated;
import io.micrometer.core.instrument.push.PushRegistryConfig;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.micrometer.core.lang.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static io.micrometer.core.instrument.config.MeterRegistryConfigValidator.*;
import static io.micrometer.core.instrument.config.validate.PropertyValidator.*;

public interface NightingaleConfig extends StepRegistryConfig {

    NightingaleConfig DEFAULT = k -> null;

    @Nullable
    String get(String key);

    default String prefix() {
        return "nightingale";
    }

    default String addr() {
        return getUrlString(this, "addr").orElse("http://localhost:2080/v1/push");
    }

    default String endpoint() {
        InetAddress addr = null;
        try {
            addr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return getString(this, "endpoint").orElse(addr.getHostAddress());
    }

    default String nid() {
        return getString(this, "nid").orElse("");
    }

    default String appendTags(){
        return getString(this, "append-tags").orElse("");
    }

    default String timestampFieldName() {
        return getString(this, "timestampFieldName").orElse("timestamp");
    }

    @Override
    default Validated<?> validate() {
        return checkAll(this,
                checkRequired("addr", NightingaleConfig::addr),
                check("connectTimeout", PushRegistryConfig::connectTimeout),
                check("readTimeout", PushRegistryConfig::readTimeout),
                check("batchSize", PushRegistryConfig::batchSize),
                check("numThreads", PushRegistryConfig::numThreads)
        );
    }
}
