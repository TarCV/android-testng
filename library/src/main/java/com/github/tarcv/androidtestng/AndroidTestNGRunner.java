package com.github.tarcv.androidtestng;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.os.Debug;
import android.support.annotation.VisibleForTesting;
import android.support.test.InstrumentationRegistry;
import android.support.test.internal.runner.listener.ActivityFinisherRunListener;
import android.support.test.internal.runner.listener.CoverageListener;
import android.support.test.orchestrator.instrumentationlistener.OrchestratedInstrumentationListener;
import android.support.test.runner.MonitoringInstrumentation;
import android.support.test.runner.lifecycle.ApplicationLifecycleCallback;
import android.support.test.runner.lifecycle.ApplicationLifecycleMonitorRegistry;
import android.support.test.runner.screenshot.Screenshot;
import android.util.Log;

import org.testng.TestNG;
import org.testng.xml.Parser;
import org.testng.xml.XmlSuite;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;

public class AndroidTestNGRunner
        extends MonitoringInstrumentation
        implements OrchestratedInstrumentationListener.OnConnectListener {
    private static final String TAG = "AndroidTestNgRunner";

    private Bundle mArguments;
    private TestNGListener mInstrumentationResultPrinter =
            new TestNGListener();
    private TestNGArgs args;
    private OrchestratedInstrumentationListener mOrchestratorListener;

    @Override
    public void onCreate(Bundle arguments) {
        mArguments = arguments;
        args = parseRunnerArgument(mArguments);

        if (args.debug) {
            Log.i(TAG, "Waiting for debugger to connect...");
            Debug.waitForDebugger();
            Log.i(TAG, "Debugger connected.");
        }

        super.onCreate(arguments);

        for (ApplicationLifecycleCallback listener : args.appListeners) {
            ApplicationLifecycleMonitorRegistry.getInstance().addLifecycleCallback(listener);
        }

        addScreenCaptureProcessors(args);

        if (args.orchestratorService != null
                && isPrimaryInstrProcess(args.targetProcess)) {
            /*
            TODO
            // If orchestratorService is provided, and we are the primary process
            // we await onOrchestratorConnect() before we start().
            mOrchestratorListener = new OrchestratedInstrumentationListener(this);
            mOrchestratorListener.connect(getContext());
             */
        } else {
            // If no orchestration service is given, or we are not the primary process we can
            // start() immediately.
            start();
        }
    }

    /**
     * Called when AndroidJUnitRunner connects to a test orchestrator, if the {@code
     * orchestratorService} parameter is set.
     */
    @Override
    public void onOrchestratorConnect() {
        start();
    }

    /**
     * Build the arguments.
     *
     * <p>Read from manifest first so manifest-provided args can be overridden with command line
     * arguments
     *
     * @param arguments
     */
    private TestNGArgs parseRunnerArgument(Bundle arguments) {
        // TODO: read from Manifest first, like AndroidJUnitRunner
        Log.d(TAG, "DEBUG arguments");
        for (String key : arguments.keySet()) {
            Log.d(TAG, "key " + key + " = " + arguments.get(key));
        }
        Log.d(TAG, "DEBUG argumetns END");
        TestNGArgs.Builder builder = new TestNGArgs.Builder(this).fromBundle(arguments);
        return builder.build();
    }

    /**
     * Get the Bundle object that contains the arguments passed to the instrumentation
     *
     * @return the Bundle object
     */
    private Bundle getArguments() {
        return mArguments;
    }

    @VisibleForTesting
    TestNGListener getInstrumentationResultPrinter() {
        return mInstrumentationResultPrinter;
    }

    @Override
    public void onStart() {
        setJsBridgeClassName("android.support.test.espresso.web.bridge.JavaScriptBridge");
        super.onStart();

        /*
         * The orchestrator cannot collect the list of tests as it is running in a different process
         * than the test app.  On first run, the Orchestrator will ask AJUR to list the tests
         * out that would be run for a given class parameter.  AJUR will then be successively
         * called with whatever it passes back to the orchestratorListener.
         */
        // TODO:
        /*
        if (args.listTestsForOrchestrator
                && isPrimaryInstrProcess(args.targetProcess)) {
            Request testRequest = buildRequest(args, getArguments());
            mOrchestratorListener.addTests(testRequest.getRunner().getDescription());
            finish(Activity.RESULT_OK, new Bundle());
            return;
        }
        */

        if (args.remoteMethod != null) {
            reflectivelyInvokeRemoteMethod(args.remoteMethod);
        }

        if (!isPrimaryInstrProcess(args.targetProcess)) {
            Log.i(TAG, "Runner is idle...");
            return;
        }

        Bundle results = new Bundle();
        try {
            TestNG executorBuilder = new TestNG(false);

            addListeners(args, executorBuilder);

            XmlSuite xmlSuite = buildSuite(args, getArguments());

            executorBuilder.setCommandLineSuite(xmlSuite);
            mInstrumentationResultPrinter.setInstrumentation(this);
            executorBuilder.run();
        } catch (RuntimeException e) {
            final String msg = "Fatal exception when running tests";
            Log.e(TAG, msg, e);
            // report the exception to instrumentation out
            results.putString(Instrumentation.REPORT_KEY_STREAMRESULT,
                    msg + "\n" + Log.getStackTraceString(e));
        }
        finish(Activity.RESULT_OK, results);
    }

    private void addListeners(TestNGArgs args, TestNG builder) {
        /* TODO if (args.logOnly) {
            // Only add the listener that will report the list of tests when running in logOnly
            // mode.
            builder.addRunListener(getInstrumentationResultPrinter());
        } else */ {
            builder.addListener(new MethodRulesHookable());
            builder.addListener(new TestNGLogRunListener());
            if (mOrchestratorListener != null) {
                builder.addListener(mOrchestratorListener);
            } else {
                builder.addListener(getInstrumentationResultPrinter());
            }
            DescripionlessListenerWrapper.addListener(builder, new ActivityFinisherRunListener(
                    this,
                                new ActivityFinisher(),
                                new Runnable() {
                                    // Yes, this is terrible and weird but avoids adding a new public API
                                    // outside the internal package.
                                    @Override
                                    public void run() {
                                        waitForActivitiesToComplete();
                                    }
                                }));
            addDelayListener(args, builder);
            addCoverageListener(args, builder);
        }
        addListenersFromArg(args, builder);
    }

    private void addScreenCaptureProcessors(TestNGArgs args) {
        Screenshot.addScreenCaptureProcessors(
                new HashSet<>(args.screenCaptureProcessors));
    }

    private void addCoverageListener(TestNGArgs args, TestNG builder) {
        if (args.codeCoverage) {
            builder.addListener(new CoverageListener(args.codeCoveragePath));
        }
    }

    /**
     * Sets up listener to inject a delay between each test, if specified.
     */
    private void addDelayListener(TestNGArgs args, TestNG builder) {
        /* TODO:
        if (args.delayInMillis > 0) {
            builder.addListener(new DelayInjector(args.delayInMillis));
        } else if (args.logOnly && Build.VERSION.SDK_INT < 16) {
            // On older platforms, collecting tests can fail for large volume of tests.
            // Insert a small delay between each test to prevent this
            builder.addListener(new DelayInjector(15 /* msec * /));
        }
        */
    }

    private void addListenersFromArg(TestNGArgs args, TestNG builder) {
        /* TODO:
        for (RunListener listener : args.listeners) {
            builder.addRunListener(listener);
        }
        */
    }

    @Override
    public boolean onException(Object obj, Throwable e) {
        TestNGListener instResultPrinter = getInstrumentationResultPrinter();
        if (instResultPrinter != null) {
            // report better error message back to Instrumentation results.
            instResultPrinter.reportProcessCrash(e);
        }
        return super.onException(obj, e);
    }

    /**
     * Builds a {@link XmlSuite} based on given input arguments.
     */
    @VisibleForTesting
    XmlSuite buildSuite(TestNGArgs runnerArgs, Bundle bundleArgs) {
        final TestNG ng = new TestNG(false);
        String targetPackage = this.getTargetContext().getPackageName();

        ng.setDefaultSuiteName("Android TestNG Suite");
        ng.setDefaultTestName("Android TestNG Test");

        // Try to load "testng.xml" from the assets directory...
        // TODO: fix for Android - tryLoadXml(ng);

        TestNGSuiteBuilder builder = createTestRequestBuilder(this, bundleArgs);

        // only scan for tests for current apk aka testContext
        // Note that this represents a change from InstrumentationTestRunner where
        // getTargetContext().getPackageCodePath() aka app under test was also scanned
        builder.addApkToScan(getContext().getPackageCodePath());

        // Nothing supported for now, removed: builder.addFromRunnerArgs(runnerArgs);

        XmlSuite xmlSuite = builder.buildNgSuite();
        xmlSuite.setVerbose(0);
        xmlSuite.setJUnit(false);
        xmlSuite.setName(InstrumentationRegistry.getTargetContext().getPackageName());
        return xmlSuite;
    }

    private void tryLoadXml(TestNG ng) {
        try {
            final InputStream input = this.getContext().getAssets().open("testng.xml");
            if (input != null) ng.setXmlSuites(new Parser(input).parseToList());
        } catch (final FileNotFoundException exception) {
            Log.d(TAG, "The \"testng.xml\" file was not found in assets");
        } catch (final Throwable throwable) {
            Log.e(TAG, "An unexpected error occurred parsing \"testng.xml\"", throwable);
 // TODO:            listener.fail(this.getClass().getName(), "onStart", throwable);
        }
    }

    /** Factory method for {@link TestNGSuiteBuilder}. */
    @VisibleForTesting
    TestNGSuiteBuilder createTestRequestBuilder(Instrumentation instr, Bundle arguments) {
        return new TestNGSuiteBuilder(instr, arguments);
    }
}
