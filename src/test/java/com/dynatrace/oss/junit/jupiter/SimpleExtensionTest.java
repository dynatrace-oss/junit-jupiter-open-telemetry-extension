package com.dynatrace.oss.junit.jupiter;

import com.dynatrace.oss.junit.jupiter.test.util.SpanDataAssert;
import com.dynatrace.oss.junit.jupiter.test.util.TestLauncherUtil;
import com.dynatrace.oss.junit.jupiter.test.util.TraceAssert;
import com.dynatrace.oss.junit.jupiter.tracing.TracingExtension;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


public class SimpleExtensionTest extends BaseTracingTest {

	@DisplayName("MyInternalTest")
	static class InternalTest {

		@RegisterExtension
		public static final TracingExtension tracingExtension = new TracingExtension(BaseTracingTest.getOpenTelemetry());
		@Order(0)
		@Test
		void testSuccessful() {
			System.out.println("dummy");
		}

		@Order(1)
		@Test
		void testFailed() {
			Assertions.fail("expectedToFail");
		}
		@Order(2)
		@Test
		void testAssumed() {
			Assumptions.assumeTrue(false, "expected to assume");
		}
		@Order(3)
		@Disabled("some disabled reason")
		@Test
		void testDisabled() {

		}
	}

	private void assertConstructor(SpanDataAssert s) {
		s.name("Constructor: class " + InternalTest.class.getName());
	}

	@Test
	void simpleTest() throws InterruptedException {
		TestLauncherUtil.executeTest(InternalTest.class);

		new TraceAssert(getSpanData()).findRootSpanByName("MyInternalTest")
				.childCount(8)
				.allChildrenSatisfy(c -> c
						.name(AbstractStringAssert::isNotEmpty)
						.stringAttribute("junit.test.class", InternalTest.class.getCanonicalName())
				)
				.assertChildren(
					this::assertConstructor,
					s -> s.name("testSuccessful()")
						.stringAttribute("junit.test.result", "SUCCESSFUL")
						.hasNoStringAttribute("junit.test.result.reason")
						.stringAttribute("junit.test.method", "testSuccessful"),
					this::assertConstructor,
					s -> s.name("testAssumed()")
						.stringAttribute("junit.test.result", "ABORTED")
						.stringAttribute("junit.test.result.reason", "Assumption failed: expected to assume")
						.stringAttribute("junit.test.method", "testAssumed"),
					this::assertConstructor,
					s -> s.name("testDisabled()")
						.stringAttribute("junit.test.result", "DISABLED")
						.stringAttribute("junit.test.result.reason", "some disabled reason")
						.stringAttribute("junit.test.method", "testDisabled"),
					this::assertConstructor,
					s -> s.name("testFailed()")
						.stringAttribute("junit.test.result", "FAILED")
						.stringAttribute("junit.test.result.reason", "expectedToFail")
						.stringAttribute("junit.test.method", "testFailed")
				);
		}
}
