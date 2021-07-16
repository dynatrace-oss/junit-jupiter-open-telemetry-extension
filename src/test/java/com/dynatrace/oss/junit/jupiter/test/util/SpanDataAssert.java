package com.dynatrace.oss.junit.jupiter.test.util;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractStringAssert;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public final class SpanDataAssert extends AbstractAssert<SpanDataAssert, SpanData> {

	private static final Logger LOG = Logger.getLogger(SpanDataAssert.class.getName());
	private final List<SpanData> trace;

	public SpanDataAssert(SpanData spanData, List<SpanData> trace) {
		super(spanData, SpanDataAssert.class);
		this.trace = Objects.requireNonNull(trace);
	}

	private final Stream<SpanData> children() {
		return trace.stream().filter(s -> s.getParentSpanId().equals(actual.getSpanId()));
	}

	private final Stream<SpanDataAssert> childrenAsserts() {
		return children().map(s -> new SpanDataAssert(s, trace));
	}

	public SpanDataAssert childCount(int expected) {
		assertThat(children()).hasSize(expected);
		return this;
	}

	public SpanDataAssert assertChildren(Consumer<SpanDataAssert>... asserts) {
		final List<SpanDataAssert> childrenAsserts = childrenAsserts()
				.collect(toList());

		for(int x = 0; x < asserts.length; x++) {
			LOG.info("Asserting child at index: " + x);
			asserts[x].accept(childrenAsserts.get(x));
		}
		return myself;
	}

	public SpanDataAssert allChildrenSatisfy(Consumer<SpanDataAssert> callback) {
		assertThat(childrenAsserts()).isNotEmpty().allSatisfy(callback);
		return this;
	}

	public SpanDataAssert name(String expected) {
		return name(n -> n.isEqualTo(expected));
	}

	public SpanDataAssert name(Consumer<AbstractStringAssert<?>> callback) {
		callback.accept(assertThat(actual.getName()));
		return myself;
	}

	public SpanDataAssert hasEnded() {
		assertThat(actual.hasEnded()).isTrue();
		return myself;
	}

	public SpanDataAssert hasNoParentSpanId() {
		assertThat(actual.getParentSpanId()).isEqualTo("0000000000000000");
		return myself;
	}

	public SpanDataAssert stringAttribute(String key, String value) {
		return stringAttribute(key, a -> a.isEqualTo(value));
	}

	public SpanDataAssert hasNoStringAttribute(String key) {
		assertThat(actual.getAttributes().asMap()).doesNotContainKey(stringKey(key));
		return this;
	}

	public SpanDataAssert stringAttribute(String key, Consumer<AbstractStringAssert<?>> callback) {
		callback.accept(assertThat(new String(actual.getAttributes().get(stringKey(key)).getBytes())));
		return myself;
	}

	public SpanDataAssert eventStringAttribute(String eventKey, String key, Consumer<AbstractStringAssert<?>> callback) {
		EventData event = actual.getEvents().stream().filter(e -> e.getName().equals(eventKey)).findFirst().get();
		callback.accept(assertThat(new String(event.getAttributes().get(stringKey(key)).getBytes())));
		return myself;
	}



	@Override
	public String toString() {
		return "TestDataAssert of: " + actual.toString();
	}
}
