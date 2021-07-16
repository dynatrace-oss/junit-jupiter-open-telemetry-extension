package com.dynatrace.oss.junit.jupiter;

import com.dynatrace.oss.junit.jupiter.test.util.TestLauncherUtil;
import com.dynatrace.oss.junit.jupiter.test.util.TraceAssert;
import com.dynatrace.oss.junit.jupiter.tracing.TracingExtension;
import io.opentelemetry.exporters.inmemory.InMemorySpanExporter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class RepeatedTest extends BaseTracingTest{


	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	public static class InternalRepeatedTest {

		@RegisterExtension
		public static final TracingExtension tracingExtension = new TracingExtension(RepeatedTest.getOpenTelemetry());

		@org.junit.jupiter.api.RepeatedTest(2)
		void testFoo() {

		}
	}

	@Test
	void testRepeated() throws InterruptedException {
		TestLauncherUtil.executeTest(InternalRepeatedTest.class);
		new TraceAssert(getSpanData())
				.findRootSpanByName("RepeatedTest$InternalRepeatedTest")
				.childCount(3)
				.assertChildren(
						s -> s.name(c -> c.startsWith("Constructor")),
						s -> s.name(c -> c.isEqualTo("repetition 1 of 2")),
						s -> s.name(c -> c.isEqualTo("repetition 2 of 2"))
				);
	}
}
