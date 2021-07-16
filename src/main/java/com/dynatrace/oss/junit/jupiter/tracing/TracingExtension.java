package com.dynatrace.oss.junit.jupiter.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;


public class TracingExtension implements BeforeAllCallback, AfterAllCallback, TestWatcher, InvocationInterceptor {

	public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("tracing");
	private final Tracer tracer;


	public TracingExtension() {
		this.tracer = getTracer(GlobalOpenTelemetry.get());
	}

	public TracingExtension(OpenTelemetry openTelemetry) {
		this.tracer = getTracer(openTelemetry);
	}

	private static Tracer getTracer(OpenTelemetry openTelemetry) {
		return openTelemetry.getTracer("junit", "1.0.0");
	}


	Span createStartedSpan(ExtensionContext context, String spanName, TestLifecycle livecycle) {
		Span span = tracer
				.spanBuilder(spanName)
				.setSpanKind(SpanKind.INTERNAL)
				.startSpan();


		span.setAttribute("junit.test.lifecycle", livecycle.toString());
		context.getTags().forEach(t -> span.setAttribute("junit.test.tag", t));
		context.getTestClass().ifPresent(t -> span.setAttribute("junit.test.class", t.getCanonicalName()));
		context.getTestMethod().ifPresent(m -> span.setAttribute("junit.test.method", m.getName()));
		return span;
	}

	Span createStartedSpan(ExtensionContext context, TestLifecycle lifecycle) {
		return createStartedSpan(context, context.getDisplayName(), lifecycle);
	}

	<T> T createInterceptorSpan(Invocation<T> invocation, ReflectiveInvocationContext<?> invocationContext, ExtensionContext context, String spanName, TestLifecycle lifecycle) throws Throwable {
		Span span = createStartedSpan(context, spanName, lifecycle);
		try(Scope scope = span.makeCurrent()) {
			if(invocationContext != null && !invocationContext.getArguments().isEmpty()) {
				span.setAllAttributes(Attributes.builder()
						.put("junit.test.arguments",
								invocationContext.getArguments().stream()
								.map(String::valueOf)
								.toArray(String[]::new))
						.build()
				);
			}
			T returnValue = invocation.proceed();

			if(lifecycle == TestLifecycle.TEST) {
				span.setAttribute("junit.test.result", "SUCCESSFUL");
			}
			return returnValue;
		} catch (Throwable e) {
			if(e instanceof TestAbortedException) {
				span.setAttribute("junit.test.result", "ABORTED");
			} else {
				span.setAttribute("junit.test.result", "FAILED");
			}
			span.setAttribute("junit.test.result.reason", e.getMessage());
			span.recordException(e);
			throw e;
		}
		finally {
			span.end();
		}
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		if(context.getTestInstanceLifecycle().map(l -> l == TestInstance.Lifecycle.PER_METHOD).orElse(false)) {
			startRootSpan(context, TestLifecycle.TEST_CLASS);
		}
	}

	void startRootSpan(ExtensionContext context, TestLifecycle lifecycle) {
		Span classSpan = createStartedSpan(context, lifecycle);
		Scope scope = classSpan.makeCurrent();
		context.getStore(NAMESPACE).put("scope", scope);
		context.getStore(NAMESPACE).put("currentSpan", classSpan);
	}

	@Override
	public <T> T interceptTestClassConstructor(Invocation<T> invocation, ReflectiveInvocationContext<Constructor<T>> invocationContext, ExtensionContext extensionContext) throws Throwable {
		if(extensionContext.getTestInstanceLifecycle().map(l -> l == TestInstance.Lifecycle.PER_CLASS).orElse(false)) {
			startRootSpan(extensionContext, TestLifecycle.CLASS_CONSTRUCTOR);
		}
		return createInterceptorSpan(invocation, invocationContext, extensionContext, "Constructor" + extensionContext.getTestClass().map(c -> ": " + c).orElse(""),TestLifecycle.CLASS_CONSTRUCTOR);
	}

	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		createInterceptorSpan(invocation, invocationContext, extensionContext, "BeforeAll: " + extensionContext.getDisplayName(), TestLifecycle.BEFORE_ALL);
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		createInterceptorSpan(invocation, invocationContext, extensionContext, "BeforeEach: " + extensionContext.getDisplayName(),TestLifecycle.BEFORE_EACH);
	}

	@Override
	public void testDisabled(ExtensionContext context, Optional<String> reason) {
		Span methodSpan = createStartedSpan(context, TestLifecycle.DISABLED);

		methodSpan.setAttribute("junit.test.result", "DISABLED");
		reason.ifPresent(r -> methodSpan.setAttribute("junit.test.result.reason", r));
		methodSpan.end();
	}


	@Override
	public void afterAll(ExtensionContext context) {
		Span span = getCurrentSpan(context);
		span.end();
		context.getStore(NAMESPACE).get("scope", Scope.class).close();
	}


	private Span getCurrentSpan(ExtensionContext context) {
		return context.getStore(NAMESPACE).get("currentSpan", Span.class);
	}


	@Override
	public <T> T interceptTestFactoryMethod(Invocation<T> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		return createInterceptorSpan(invocation, invocationContext, extensionContext, "TestFactory: " + extensionContext.getDisplayName(), TestLifecycle.FACTORY_METHOD);
	}

	@Override
	public void interceptDynamicTest(Invocation<Void> invocation, ExtensionContext extensionContext) throws Throwable {
		createInterceptorSpan(invocation, null, extensionContext, extensionContext.getDisplayName(), TestLifecycle.TEST);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		createInterceptorSpan(invocation, invocationContext, extensionContext, "AfterEach:" + extensionContext.getDisplayName(), TestLifecycle.AFTER_EACH);
	}

	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		createInterceptorSpan(invocation, invocationContext, extensionContext, "AfterAll:" + extensionContext.getDisplayName(),TestLifecycle.AFTER_ALL);
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		createInterceptorSpan(invocation, invocationContext, extensionContext, extensionContext.getDisplayName(), TestLifecycle.TEST);
	}

	@Override
	public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		createInterceptorSpan(invocation, invocationContext, extensionContext, extensionContext.getDisplayName(), TestLifecycle.TEST);
	}
}

