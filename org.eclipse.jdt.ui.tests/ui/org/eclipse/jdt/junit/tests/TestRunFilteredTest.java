package org.eclipse.jdt.junit.tests;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;



public class TestRunFilteredTest extends AbstractTestRunListenerTest {



	private boolean currentIncludeFlag= true;

	private void createTypes() throws Exception {
		String includeSource=
				"package pack.include;\n" +
						"import junit.framework.TestCase;\n" +
						"public class IncudeTestCase extends TestCase {\n" +
						"    public void testSucceed() { }\n" +
						"}";
		createType(includeSource, "pack.include", "IncudeTestCase.java");
		String excludeSource=
				"package pack.exclude;\n" +
						"import junit.framework.TestCase;\n" +
						"public class ExcludeTestCase extends TestCase {\n" +
						"    public void testSucceed() {  }\n" +
						"}";
		createType(excludeSource, "pack.exclude", "ExcludeTestCase.java");
	}

	public void testIncludeFilter() throws Exception {
		createTypes();

		String[] expectedSequence= new String[] {
				"sessionStarted-" + TestRunListeners.sessionAsString("TestRunListenerTest", ProgressState.RUNNING, Result.UNDEFINED, 0),
				"testCaseStarted-" + TestRunListeners.testCaseAsString("testSucceed", "pack.include.IncudeTestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
				"testCaseFinished-" + TestRunListeners.testCaseAsString("testSucceed", "pack.include.IncudeTestCase", ProgressState.COMPLETED, Result.OK, null, 0),
				"sessionFinished-" + TestRunListeners.sessionAsString("TestRunListenerTest", ProgressState.COMPLETED, Result.OK, 0)
		};

		currentIncludeFlag= true;

		String[] actual= runTestInPorject();
		assertEqualLog(expectedSequence, actual);

	}

	public void testExcludeFilter() throws Exception {
		createTypes();

		String[] expectedSequence= new String[] {
				"sessionStarted-" + TestRunListeners.sessionAsString("TestRunListenerTest", ProgressState.RUNNING, Result.UNDEFINED, 0),
				"testCaseStarted-" + TestRunListeners.testCaseAsString("testSucceed", "pack.exclude.ExcludeTestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
				"testCaseFinished-" + TestRunListeners.testCaseAsString("testSucceed", "pack.exclude.ExcludeTestCase", ProgressState.COMPLETED, Result.OK, null, 0),
				"sessionFinished-" + TestRunListeners.sessionAsString("TestRunListenerTest", ProgressState.COMPLETED, Result.OK, 0)
		};

		currentIncludeFlag= false;

		String[] actual= runTestInPorject();
		assertEqualLog(expectedSequence, actual);

	}

	@Override
	protected ILaunchConfiguration createLaunchConfiguration(IJavaElement element) throws CoreException {
		return TestFilterJUnitLaunchShortcut.createConfiguration(element, "pack.include.*", currentIncludeFlag);
	}

	private static class TestFilterJUnitLaunchShortcut extends JUnitLaunchShortcut {
		public static ILaunchConfiguration createConfiguration(IJavaElement element, String pattern, boolean include) throws CoreException {
			ILaunchConfigurationWorkingCopy copy= new TestFilterJUnitLaunchShortcut().createLaunchConfiguration(element);
			copy.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER_FILTER_INCLUDE, include);
			copy.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER_FILTER_PATTERN, pattern);
			return copy.doSave();
		}
	}


	private String[] runTestInPorject() throws Exception {
		TestRunLog log= new TestRunLog();
		final TestRunListener testRunListener= new TestRunListeners.SequenceTest(log);
		JUnitCore.addTestRunListener(testRunListener);
		try {
			return launchJUnit(getProject(), log);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
	}
}
