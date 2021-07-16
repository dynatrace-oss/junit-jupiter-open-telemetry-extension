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
package com.dynatrace.oss.junit.jupiter.test.util;

import io.opentelemetry.sdk.trace.data.SpanData;
import org.assertj.core.api.Assertions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public final class TraceAssert {

	private final List<SpanData> traces;


	public TraceAssert(List<SpanData> traces) {
		this.traces = new ArrayList<>();
		this.traces.addAll(traces);
		Collections.sort(this.traces, Comparator.comparingLong(SpanData::getStartEpochNanos));
	}



	public SpanDataAssert findRootSpan(Predicate<SpanData> predicate) {
		Optional<SpanData> rootSpan = traces.stream()
				.filter(s -> s.getParentSpanId().equals("0000000000000000"))
				.filter(predicate)
				.findFirst();

		assertThat(rootSpan).isPresent();

		return new SpanDataAssert(rootSpan.get(), traces.stream()
				.filter(r -> Objects.equals(r.getTraceId(), rootSpan.get().getTraceId()))
				.collect(Collectors.toList()));
	}

	public SpanDataAssert findRootSpanByName(String name) {
		return findRootSpan(s -> Objects.equals(s.getName(), name));
	}

}
