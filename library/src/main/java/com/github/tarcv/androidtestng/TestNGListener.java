package com.github.tarcv.androidtestng;

import android.app.Instrumentation;
import android.os.Bundle;
import android.support.test.internal.runner.listener.InstrumentationResultPrinter;
import android.util.Log;

import org.testng.IConfigurationListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static android.app.Instrumentation.REPORT_KEY_IDENTIFIER;

/**
 * A <i>TestNG</i> {@link ITestListener} sending status reports.
 */
public class TestNGListener implements ITestListener,
        IConfigurationListener {

    private static final String LOG_TAG = "TestNGListener";

    /** Value for {@link Instrumentation#REPORT_KEY_IDENTIFIER} */
    private static final String REPORT_VALUE_ID = "TestNGRunner";

    /** Total number of tests being run (sent with all status messages). */
    private static final String REPORT_KEY_NUM_TOTAL = "numtests";
    /** Sequence number of the current test. */
    private static final String REPORT_KEY_NUM_CURRENT = "current";
    /** Name of the current test class. */
    private static final String REPORT_KEY_NAME_CLASS = "class";
    /** The name of the current test. */
    private static final String REPORT_KEY_NAME_TEST = "test";
    /** Stack trace describing an error or failure. */
    private static final String REPORT_KEY_STACK = "stack";

    /** Test is starting. */
    private static final int REPORT_VALUE_RESULT_START = 1;
    /** Test completed successfully. */
    private static final int REPORT_VALUE_RESULT_OK = 0;
    /** Test completed with an error.*/
    //private static final int REPORT_VALUE_RESULT_ERROR = -1;
    /** Test completed with a failure. */
    private static final int REPORT_VALUE_RESULT_FAILURE = -2;
    /** Test was skipped. */
    private static final int REPORT_VALUE_RESULT_SKIPPED = -3;
    /** Test completed with an assumption failure. */
    //private static final int REPORT_VALUE_RESULT_ASSUMPTION_FAILURE = -4;

    /* ====================================================================== */

    private final AtomicInteger testNumber = new AtomicInteger();
    private final ConcurrentHashMap<String, AtomicInteger> tests;
    private final Bundle mResultTemplate;
    private Bundle mTestResult;
    private String mTestClass = null;
    private ITestResult mDescription;
    private final InstrumentationResultPrinter wrapped = new InstrumentationResultPrinter();

    /**
     * Create a new {@link TestNGListener} instance.
     *
     */
    TestNGListener() {
        this.tests = new ConcurrentHashMap<>();
        this.mTestResult = new Bundle();
        this.mResultTemplate = new Bundle();
    }

    /**
     * Notify that we are about to start testing.
     *
     * This method will setup the initial {@link Bundle} for notifications.
     */
    @Override
    public void onStart(ITestContext context) {

        mResultTemplate.putString(REPORT_KEY_IDENTIFIER, REPORT_VALUE_ID);

        final ITestNGMethod[] methods = context.getAllTestMethods();

        if ((methods == null) || (methods.length < 1)) {
            mResultTemplate.putInt(REPORT_KEY_NUM_TOTAL, 0);
        } else {
            mResultTemplate.putInt(REPORT_KEY_NUM_TOTAL, methods.length);
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        mDescription = result; // cache Description in case of a crash
        mTestResult = new Bundle(mResultTemplate);

        final String testClass = result.getInstanceName();
        final String resultName = result.getName();
        final String name = testClass + '.' + resultName;

        // Test methods can be invoked mutiple times, with data providers!
        if (! tests.contains(name)) tests.putIfAbsent(name, new AtomicInteger(0));
        final AtomicInteger count = tests.get(name);

        final int num = count.getAndIncrement();
        final String testName = num == 0 ? resultName :
                // TODO: consider providing actual execution name with params:
                String.format("%s[%d]", resultName, num); // JUnit Parameterized format

        mTestResult.putString(REPORT_KEY_NAME_CLASS, testClass);
        mTestResult.putString(REPORT_KEY_NAME_TEST, testName);
        mTestResult.putInt(REPORT_KEY_NUM_CURRENT, testNumber.incrementAndGet());
        // pretty printing
        if (testClass != null && !testClass.equals(mTestClass)) {
            mTestResult.putString(Instrumentation.REPORT_KEY_STREAMRESULT,
                    String.format("\n%s:", testClass));
            mTestClass = testClass;
        } else {
            mTestResult.putString(Instrumentation.REPORT_KEY_STREAMRESULT, "");
        }

        wrapped.sendStatus(REPORT_VALUE_RESULT_START, mTestResult);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        mTestResult.putString(Instrumentation.REPORT_KEY_STREAMRESULT, ".");
        wrapped.sendStatus(REPORT_VALUE_RESULT_OK, mTestResult);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        wrapped.sendStatus(REPORT_VALUE_RESULT_SKIPPED, mTestResult);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        reportFailure(result);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        reportFailure(result);
    }

    private void reportFailure(ITestResult failure) {
        String trace = failure.getThrowable().toString();
        mTestResult.putString(REPORT_KEY_STACK, trace);
        // pretty printing
        mTestResult.putString(Instrumentation.REPORT_KEY_STREAMRESULT,
                String.format("\nError in %s:\n%s",
                        failure.getName(), trace));
        wrapped.sendStatus(REPORT_VALUE_RESULT_FAILURE, mTestResult);
    }

    /**
     * Produce a more meaningful crash report including stack trace and report it back to
     * Instrumentation results.
     */
    public void reportProcessCrash(Throwable t) {
        try {
            String trace = t.toString();
            mTestResult.putString(REPORT_KEY_STACK, trace);
            // pretty printing
            mTestResult.putString(Instrumentation.REPORT_KEY_STREAMRESULT,
                    String.format("\nProcess crashed while executing %s:\n%s",
                            mDescription.getName(), trace));
            wrapped.sendStatus(REPORT_VALUE_RESULT_FAILURE, mTestResult);
        } catch (Exception e) {
            // ignore, about to crash anyway
            if (null == mDescription) {
                Log.e(LOG_TAG, "Failed to initialize test before process crash");
            } else {
                Log.e(LOG_TAG, "Failed to mark test " + mDescription.getName() +
                        " as finished after process crash");
            }
        }
    }

    public void setInstrumentation(Instrumentation instrumentation) {
        wrapped.setInstrumentation(instrumentation);
    }

    @Override
    public void onFinish(ITestContext context) {
        // Nothing to do
    }

    @Override
    public void onConfigurationSuccess(ITestResult result) {
        // We don't report any configuration success...
    }

    @Override
    public void onConfigurationFailure(ITestResult result) {
        // Emulate test failure
        this.onTestStart(result);
        this.onTestFailure(result);
    }

    @Override
    public void onConfigurationSkip(ITestResult result) {
        // Emulate test skipped
        this.onTestStart(result);
        this.onTestSkipped(result);
    }
}
