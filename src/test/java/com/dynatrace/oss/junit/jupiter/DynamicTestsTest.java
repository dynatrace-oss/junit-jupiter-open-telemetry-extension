package com.dynatrace.oss.junit.jupiter;

import com.dynatrace.oss.junit.jupiter.test.util.TestLauncherUtil;
import com.dynatrace.oss.junit.jupiter.test.util.TraceAssert;
import com.dynatrace.oss.junit.jupiter.tracing.TracingExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.stream.Stream;

public class DynamicTestsTest extends BaseTracingTest{

	public static class InternalDynamicTest {

		@RegisterExtension
		public static final TracingExtension tracingExtension = new TracingExtension(DynamicTestsTest.getOpenTelemetry());

		@TestFactory
		Stream<DynamicContainer> dynamicTestsFromIntStream() {
			return Stream.of(DynamicContainer.dynamicContainer("myContainer",
					Stream.of(DynamicTest.dynamicTest("dynamicTest", () -> Assertions.fail()))));

		}
	}

	@Test
	void testDynamic() {
		TestLauncherUtil.executeTest(InternalDynamicTest.class);
		new TraceAssert(getSpanData()).findRootSpanByName("DynamicTestsTest$InternalDynamicTest")
				.childCount(3)
				.assertChildren(
						s -> s.name(n -> n.startsWith("Constructor:")),
						s -> s.name("TestFactory: dynamicTestsFromIntStream()"),
						s -> s.name("dynamicTest")
				);
	}


	



}
