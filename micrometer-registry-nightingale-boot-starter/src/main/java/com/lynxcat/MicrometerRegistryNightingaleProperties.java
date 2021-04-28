package com.lynxcat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "management.metrics.export.nightingale")
public class MicrometerRegistryNightingaleProperties {
    private String addr;
    private String endpoint;
    private Duration step;
    private String nid;
    private String appendTags;
    private Boolean enabled;
    private String metricBlockList;
    private Boolean autoRegistry = false;
    private String apiAddr;
    private String UserToken;

    public String getMetricBlockList() {
        return metricBlockList;
    }

    public void setMetricBlockList(String metricBlockList) {
        this.metricBlockList = metricBlockList;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Duration getStep() {
        return step;
    }

    public void setStep(Duration step) {
        this.step = step;
    }

    public String getNid() {
        return nid;
    }

    public void setNid(String nid) {
        this.nid = nid;
    }

    public String getAppendTags() {
        return appendTags;
    }

    public void setAppendTags(String appendTags) {
        this.appendTags = appendTags;
    }

	public Boolean getAutoRegistry() {
		return autoRegistry;
	}

	public void setAutoRegistry(Boolean autoRegistry) {
		this.autoRegistry = autoRegistry;
	}

	public String getApiAddr() {
		return apiAddr;
	}

	public void setApiAddr(String apiAddr) {
		this.apiAddr = apiAddr;
	}

	public String getUserToken() {
		return UserToken;
	}

	public void setUserToken(String userToken) {
		UserToken = userToken;
	}
}
