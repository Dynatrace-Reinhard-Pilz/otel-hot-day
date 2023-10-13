package com.dtcookie.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;

public class Otel {

	private Otel() {
		// prevent instantiation
	}

	public static OpenTelemetry init() {
		Resource serviceName = Optional.ofNullable(System.getenv("OTEL_SERVICE_NAME"))
				.map(n -> Attributes.of(AttributeKey.stringKey("service.name"), n)).map(Resource::create)
				.orElseGet(Resource::empty);

		OpenTelemetrySdk sdk = AutoConfiguredOpenTelemetrySdk.builder()
				.addResourceCustomizer((resource, properties) -> {
					Resource dtMetadata = Resource.empty();

					for (String name : new String[] { "dt_metadata_e617c525669e072eebe3d0f08212e8f2.properties",
							"/var/lib/dynatrace/enrichment/dt_metadata.properties" }) {
						try {
							Properties props = new Properties();
							props.load(name.startsWith("/var") ? new FileInputStream(name)
									: new FileInputStream(Files.readAllLines(Paths.get(name)).get(0)));
							dtMetadata = dtMetadata.merge(Resource.create(props.entrySet().stream()
									.collect(Attributes::builder,
											(b, e) -> b.put(e.getKey().toString(), e.getValue().toString()),
											(b1, b2) -> b1.putAll(b2.build()))
									.build()));
						} catch (IOException e) {
						}
					}

					return resource.merge(dtMetadata).merge(serviceName);
				}).build().getOpenTelemetrySdk();
		OpenTelemetryAppender.install(sdk);
		return sdk;
	}

	public static TextMapGetter<Map<String,List<String>>> newRequestHeaderGetter() {
    	return new TextMapGetter<Map<String,List<String>>> () {
    	    @Override
    	    public String get(Map<String,List<String>> carrier, String key) {
    	    	List<String> value = carrier.get(key);
    	    	if (value == null) {
    	    		value = carrier.get(key.toUpperCase());
    	    	}
    	    	if (value == null) {
    	    		value = carrier.get(key.toLowerCase());
    	    	}
    	    	if (value == null) {
    	    		return null;
    	    	}
    	    	if (value.isEmpty()) {
    	    		return null;
    	    	}    	
    	        return value.get(0);
    	    }
    	    
    	    @Override
    	    public Iterable<String> keys(Map<String,List<String>> carrier) {
    	        return carrier.keySet();
    	    }
    	};
    }
}
