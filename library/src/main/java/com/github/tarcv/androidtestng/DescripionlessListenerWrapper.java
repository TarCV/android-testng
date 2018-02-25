package com.github.tarcv.androidtestng;

import org.junit.runner.notification.RunListener;
import org.testng.IExecutionListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.TestNG;

public class DescripionlessListenerWrapper {
    public static void addListener(TestNG ng, RunListener listener) {
        ng.addListener(new RunListenerWrapper(listener));
        ng.addListener(new TestCaseListenerWrapper(listener));
    }

    private static class RunListenerWrapper implements IExecutionListener {
        private final RunListener runListener;

        RunListenerWrapper(RunListener runListener) {
            this.runListener = runListener;
        }

        @Override
        public void onExecutionStart() {
            try {
                runListener.testRunStarted(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onExecutionFinish() {
            try {
                runListener.testRunFinished(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class TestCaseListenerWrapper implements ITestListener {
        private final RunListener wrapped;

        TestCaseListenerWrapper(RunListener wrapped) {
            this.wrapped = wrapped;
        }


        @Override
        public void onTestStart(ITestResult result) {
            try {
                wrapped.testStarted(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void onTestFinish(ITestResult result) {
            try {
                wrapped.testFinished(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onTestSuccess(ITestResult result) {
            onTestFinish(result);
        }

        @Override
        public void onTestFailure(ITestResult result) {
            try {
                wrapped.testFailure(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onTestSkipped(ITestResult result) {
            try {
                wrapped.testIgnored(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
            onTestFailure(result);
        }

        @Override
        public void onStart(ITestContext context) {

        }

        @Override
        public void onFinish(ITestContext context) {

        }
    }
}