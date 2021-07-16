package com.dynatrace.oss.junit.jupiter.test.util;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public final class TestLauncherUtil {

	private TestLauncherUtil() {

	}

	public static TestExecutionSummary executeTest(Class<?> classToExecute){

		LauncherDiscoveryRequest request
				= LauncherDiscoveryRequestBuilder.request()
				.enableImplicitConfigurationParameters(true)
				.selectors(selectClass(classToExecute.getName()))
				.build();

		TestPlan plan = LauncherFactory.create().discover(request);
		Launcher launcher = LauncherFactory.create();
		SummaryGeneratingListener summaryGeneratingListener
				= new SummaryGeneratingListener();
		launcher.execute(
				request,
				new TestExecutionListener[] { summaryGeneratingListener });

		return summaryGeneratingListener.getSummary();
	}

}
