/**
 * Copyright 2021 Dynatrace LLC
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dynatrace.oss.junit.jupiter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporters.inmemory.InMemorySpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

public class BaseTracingTest {

	private static OpenTelemetry telemetry;
	private static InMemorySpanExporter exporter = InMemorySpanExporter.create();

	protected static OpenTelemetry getOpenTelemetry() {
		if(telemetry == null) {
            OtlpGrpcSpanExporter otlpGrpcSpanExporter =
                OtlpGrpcSpanExporter
                    .builder()
                    .setEndpoint(getExporterEndpoint())
                    .setTimeout(30, TimeUnit.SECONDS)
    				.build();

			Runtime.getRuntime().addShutdownHook(new Thread(() -> otlpGrpcSpanExporter.flush()));

			Resource serviceNameResource =
					Resource.create(Attributes.of(stringKey("service.name"), "junit-extension"));

			final SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
					.addSpanProcessor(SimpleSpanProcessor.create(otlpGrpcSpanExporter))
					.addSpanProcessor(SimpleSpanProcessor.create(exporter))
					.setResource(Resource.getDefault().merge(serviceNameResource))
					.build();

			telemetry = OpenTelemetrySdk.builder()
					.setTracerProvider(sdkTracerProvider)
					.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
					.build();
		}


		return  telemetry;
	}

    private static String getExporterEndpoint() {
        return System.getProperty("exporter-scheme", "http")
            + "://" + System.getProperty("exporter-host", "localhost")
            + ":" + System.getProperty("exporter-port", "4317");
    }

	@BeforeEach
	public void resetExporter() {
		exporter.reset();
	}

	protected static List<SpanData> getSpanData() {
		exporter.flush();
		return exporter.getFinishedSpanItems();
	}

}
