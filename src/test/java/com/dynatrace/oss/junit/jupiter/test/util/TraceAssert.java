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
