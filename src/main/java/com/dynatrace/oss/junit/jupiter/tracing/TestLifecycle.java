package com.dynatrace.oss.junit.jupiter.tracing;

enum TestLifecycle {
	BEFORE_ALL,
	CLASS_CONSTRUCTOR,
	TEST_CLASS,
	BEFORE_EACH,
	DISABLED,
	FACTORY_METHOD,
	TEST,
	AFTER_EACH,
	AFTER_ALL;
}
