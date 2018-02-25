package com.github.tarcv.androidtestng;

import android.support.test.internal.runner.listener.LogRunListener;
import android.util.Log;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Port of {@link LogRunListener}
 */
public class TestNGLogRunListener implements ITestListener {

    // use tag consistent with InstrumentationTestRunner
    private static final String TAG = "TestRunner";

    @Override
    public void onTestStart(ITestResult result) {
        Log.i(TAG, "started: " + result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        onTestFinished(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        onTestFinished(result);

        reportFailure(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        Log.i(TAG, "ignored: " + result.getName());
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        onTestFailure(result);
        Log.i(TAG, "within success percentage:");
        reportFailure(result);
    }

    @Override
    public void onStart(ITestContext context) {
        // TODO: check if this got called multiple times for multiple suites
        Log.i(TAG, String.format("run started: %d tests", context.getAllTestMethods().length));
    }

    @Override
    public void onFinish(ITestContext context) {
        // TODO: check if this got called multiple times for multiple suites
        int failed = 0;
        int ignored = 0;
        failed += context.getFailedTests().size();
        ignored += context.getSkippedTests().size();
        Log.i(TAG, String.format("run finished: %d tests, %d failed, %d ignored",
                context.getAllTestMethods().length, failed, ignored));
    }

    private void onTestFinished(ITestResult result) {
        Log.i(TAG, "finished: " + result.getName());
    }

    private void reportFailure(ITestResult result) {
        Log.i(TAG, "failed: " + result.getName());
        Log.i(TAG, "----- begin exception -----");
        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            Log.i(TAG, throwable.toString());
        }
        Log.i(TAG, "----- end exception -----");
    }
}
