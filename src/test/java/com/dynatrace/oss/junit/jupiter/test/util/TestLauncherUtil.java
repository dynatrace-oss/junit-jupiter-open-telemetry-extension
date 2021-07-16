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
