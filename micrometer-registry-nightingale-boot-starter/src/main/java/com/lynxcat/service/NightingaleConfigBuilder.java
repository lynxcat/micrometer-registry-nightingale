package com.lynxcat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lynxcat.NightingaleConfig;
import com.lynxcat.MicrometerRegistryNightingaleProperties;
import com.lynxcat.entities.NightingaleNote;
import com.lynxcat.entities.NightingaleRegistryBody;
import com.lynxcat.entities.NightingaleResult;
import io.micrometer.core.ipc.http.HttpSender;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class NightingaleConfigBuilder {

	private Environment environment;

	private MicrometerRegistryNightingaleProperties micrometerRegistryNightingaleProperties;

	private final Logger logger = LoggerFactory.getLogger(NightingaleConfigBuilder.class);

	NightingaleConfigBuilder(Environment environment, MicrometerRegistryNightingaleProperties micrometerRegistryNightingaleProperties){
		this.environment = environment;
		this.micrometerRegistryNightingaleProperties = micrometerRegistryNightingaleProperties;
	}

	public NightingaleConfig getNightingaleConfig() throws Throwable {
		if (micrometerRegistryNightingaleProperties.getAutoRegistry()) {
			return instanceNightingaleConfig(getNodeIdByIdent(generateIdent()));
		} else {
			return instanceNightingaleConfig();
		}
	}

	private NightingaleConfig instanceNightingaleConfig() {
		return key -> environment.getProperty("management.metrics.export." + key);
	}

	private NightingaleConfig instanceNightingaleConfig(String nid) {
		return new NightingaleConfig() {
			@Override
			public String get(String key) {
				return environment.getProperty("management.metrics.export." + key);
			}

			@Override
			public String nid() {
				return nid;
			}
		};
	}

	private String registryNode(String ident, HttpSender httpClient, ObjectMapper mapper) throws Throwable {

		AtomicReference<String> nid = new AtomicReference<>("");

		NightingaleRegistryBody body = new NightingaleRegistryBody();
		body.setPid(Integer.valueOf(micrometerRegistryNightingaleProperties.getNid()));
		body.setName(environment.getProperty("spring.application.name"));
		body.setIdent(ident);
		body.setNote(ident);

		httpClient.post(micrometerRegistryNightingaleProperties.getApiAddr() + "/api/rdb/nodes")
				.withHeader("X-User-Token", micrometerRegistryNightingaleProperties.getUserToken())
				.withJsonContent(mapper.writeValueAsString(body)).send().onSuccess(response -> {

			NightingaleResult<NightingaleNote> result;

			try {
				result = mapper.readValue(response.body(), new TypeReference<NightingaleResult<NightingaleNote>>() {});
				if (result != null && result.getDat() != null) {
					nid.set(result.getDat().getId());
				}
			} catch (JsonProcessingException e) {
				logger.error(e.getMessage(), e);
			}
		});


		return nid.get();
	}

	private String generateIdent() throws UnknownHostException {
		InetAddress address;
		address = InetAddress.getLocalHost();
		return address.getHostAddress().replace(".", "-") + "-" + environment.getProperty("spring.application.name");
	}

	private String getNodeIdByIdent(String ident) throws Throwable {
		HttpSender httpClient = new HttpUrlConnectionSender();
		ObjectMapper mapper = new ObjectMapper();
		AtomicReference<String> nid = new AtomicReference<>("");

		httpClient.get(micrometerRegistryNightingaleProperties.getApiAddr() + "/api/rdb/tree?query=" + ident).withHeader("X-User-Token", micrometerRegistryNightingaleProperties.getUserToken()).send().onSuccess(response -> {

			NightingaleResult<List<NightingaleNote>> result;
			try {
				result = mapper.readValue(response.body(), new TypeReference<NightingaleResult<List<NightingaleNote>>>() {});
				if (result.getDat() != null && result.getDat().size() > 0) {
					for (NightingaleNote node : result.getDat()) {
						if (ident.equals(node.getIdent())) {
							nid.set(node.getId());
						}
					}
				}
			} catch (JsonProcessingException e) {
				logger.error(e.getMessage(), e);
			}
		});

		if (nid.get() == null || nid.get().equals("")) {
			nid.set(registryNode(ident, httpClient, mapper));
		}

		if (nid.get() == null || nid.get().equals("")) {
			throw new Throwable("can't get node id for nightingale");
		}

		return nid.get();
	}
}
