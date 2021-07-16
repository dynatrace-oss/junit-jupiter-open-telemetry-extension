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

import com.dynatrace.oss.junit.jupiter.test.util.TestLauncherUtil;
import com.dynatrace.oss.junit.jupiter.test.util.TraceAssert;
import com.dynatrace.oss.junit.jupiter.tracing.TracingExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

public class ParameterizedExtensionTest extends BaseTracingTest {


	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	static class ParameterizedInternalTest {
		@RegisterExtension
		public static final TracingExtension tracingExtension = new TracingExtension(getOpenTelemetry());

		@ParameterizedTest
		@ValueSource(ints = {1,2,3})
		void testParameter(int param) {
			assertThat(param).isGreaterThan(1);
			assumeThat(param).isLessThan(3);
		}
	}

	@Test
	void parametersTest() {
		TestLauncherUtil.executeTest(ParameterizedExtensionTest.ParameterizedInternalTest.class);

		new TraceAssert(getSpanData())
				.findRootSpanByName("ParameterizedExtensionTest$ParameterizedInternalTest")
				.childCount(4)
				.assertChildren(
						s -> s.name(n -> n.startsWith("Constructor")),
						s -> s.name("[1] 1").stringAttribute("junit.test.method","testParameter"),
						s -> s.name("[2] 2").stringAttribute("junit.test.method","testParameter"),
						s -> s.name("[3] 3").stringAttribute("junit.test.method","testParameter")
				);
	}

}
