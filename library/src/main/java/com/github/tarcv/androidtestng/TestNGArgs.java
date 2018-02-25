package com.github.tarcv.androidtestng;

import android.app.Instrumentation;
import android.os.Bundle;
import android.support.test.internal.runner.RunnerArgs;
import android.support.test.runner.lifecycle.ApplicationLifecycleCallback;
import android.support.test.runner.screenshot.ScreenCaptureProcessor;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.support.test.internal.runner.TestArgFactory.createTestArg;

/**
 * A <i>TestNG</i> runner arguments.
 */
public class TestNGArgs {

    /* ARGUMENT KEYS */
    public static final String ARGUMENT_DEBUG = "debug";
    public static final String ARGUMENT_COVERAGE = "coverage";
    public static final String ARGUMENT_COVERAGE_PATH = "coverageFile";
    public static final String ARGUMENT_APP_LISTENER = "appListener";
    public static final String ARGUMENT_TARGET_PROCESS = "targetProcess";
    public static final String ARGUMENT_SCREENSHOT_PROCESSORS = "screenCaptureProcessors";
    public static final String ARGUMENT_ORCHESTRATOR_SERVICE = "orchestratorService";
    public static final String ARGUMENT_REMOTE_INIT_METHOD = "remoteMethod";
    public static final String ARGUMENT_LIST_TESTS_FOR_ORCHESTRATOR = "listTestsForOrchestrator";

    // used to separate multiple fully-qualified test case class names
    private static final String CLASS_SEPARATOR = ",";
    // used to separate fully-qualified test case class name, and one of its methods
    private static final char METHOD_SEPARATOR = '#';

    /* Default Values */

    private static final String DEFAULT_COVERAGE_FILE_NAME = "coverage.ec";


    public final boolean debug;
    public final boolean codeCoverage;
    public final String codeCoveragePath;
    public final List<ApplicationLifecycleCallback> appListeners;
    public final RunnerArgs.TestArg remoteMethod;
    public final String orchestratorService;
    public final boolean listTestsForOrchestrator;
    public final String targetProcess;
    public final List<ScreenCaptureProcessor> screenCaptureProcessors;

    private TestNGArgs(Builder builder) {
        this.debug = builder.debug;
        this.codeCoverage = builder.codeCoverage;
        this.codeCoveragePath = builder.codeCoveragePath;
        this.appListeners = Collections.unmodifiableList(builder.appListeners);
        this.remoteMethod = builder.remoteMethod;
        this.orchestratorService = builder.orchestratorService;
        this.listTestsForOrchestrator = builder.listTestsForOrchestrator;
        this.targetProcess = builder.targetProcess;
        this.screenCaptureProcessors =
                Collections.unmodifiableList(builder.screenCaptureProcessors);

        Log.d(TestNGLogger.TAG, this.toString());
    }

    public static class Builder {
        private final Instrumentation instrumentation;
        private boolean debug = false;
        private boolean codeCoverage = false;
        private String codeCoveragePath = null;
        private List<ApplicationLifecycleCallback> appListeners =
                new ArrayList<ApplicationLifecycleCallback>();
        private RunnerArgs.TestArg remoteMethod = null;
        private String orchestratorService = null;
        private boolean listTestsForOrchestrator = false;
        private String targetProcess = null;
        private List<ScreenCaptureProcessor> screenCaptureProcessors = new ArrayList<>();

        public Builder(Instrumentation instrumentation) {
            this.instrumentation = instrumentation;
        }

        public Builder fromBundle(Bundle bundle) {
            debug = parseBoolean(bundle.getString(ARGUMENT_DEBUG));
            codeCoverage = parseBoolean(bundle.getString(ARGUMENT_COVERAGE));
            codeCoveragePath = bundle.getString(ARGUMENT_COVERAGE_PATH);
            if (codeCoverage && codeCoveragePath == null) {
                codeCoveragePath = instrumentation.getTargetContext().getFilesDir().getAbsolutePath() +
                        File.separator + DEFAULT_COVERAGE_FILE_NAME;
            }
            this.appListeners.addAll(
                    parseLoadAndInstantiateClasses(
                            bundle.getString(ARGUMENT_APP_LISTENER), ApplicationLifecycleCallback.class, null));
            if (bundle.containsKey(ARGUMENT_REMOTE_INIT_METHOD)) {
                this.remoteMethod = parseTestClass(bundle.getString(ARGUMENT_REMOTE_INIT_METHOD));
            }
            this.orchestratorService = bundle.getString(ARGUMENT_ORCHESTRATOR_SERVICE);
            this.listTestsForOrchestrator =
                    parseBoolean(bundle.getString(ARGUMENT_LIST_TESTS_FOR_ORCHESTRATOR));
            this.targetProcess = bundle.getString(ARGUMENT_TARGET_PROCESS);
            this.screenCaptureProcessors.addAll(
                    parseLoadAndInstantiateClasses(
                            bundle.getString(ARGUMENT_SCREENSHOT_PROCESSORS),
                            ScreenCaptureProcessor.class,
                            null));

            return this;
        }

        public TestNGArgs build() {
            return new TestNGArgs(this);
        }

        /**
         * Parse boolean value from a String
         *
         * @return the boolean value, false on null input
         */
        private boolean parseBoolean(String booleanValue) {
            return booleanValue != null && Boolean.parseBoolean(booleanValue);
        }

        /**
         * Parse an individual test class and optionally method from given string.
         *
         * <p>Expected format: com.TestClass1[#method1]
         */
        private static RunnerArgs.TestArg parseTestClass(String testClassName) {
            if (TextUtils.isEmpty(testClassName)) {
                return null;
            }
            int methodSeparatorIndex = testClassName.indexOf(METHOD_SEPARATOR);
            if (methodSeparatorIndex > 0) {
                String testMethodName = testClassName.substring(methodSeparatorIndex + 1);
                testClassName = testClassName.substring(0, methodSeparatorIndex);
                return createTestArg(testClassName, testMethodName);
            } else {
                return createTestArg(testClassName);
            }
        }

        /**
         * Create a set of objects given a CSV string of full class names and type.
         *
         * @return the List of objects or empty list on null input
         */
        private <T> List<T> parseLoadAndInstantiateClasses(
                String classString, Class<T> type, Bundle bundle) {
            List<T> objects = new ArrayList<T>();
            if (classString != null) {
                for (String className : classString.split(CLASS_SEPARATOR)) {
                    loadClassByNameInstantiateAndAdd(objects, className, type, bundle);
                }
            }
            return objects;
        }

        /**
         * Load class by supplied name, instantiate and add object to supplied list.
         *
         * <p>No effect if input is null or empty.
         *
         * @param objects the List to add to
         * @param className the fully qualified class name
         * @param bundle The bundle to pass to the constructor, null if no bundle is to be passed.
         * @throws IllegalArgumentException if listener cannot be loaded
         */
        private <T> void loadClassByNameInstantiateAndAdd(
                List<T> objects, String className, Class<T> type, Bundle bundle) {
            if (className == null || className.length() == 0) {
                return;
            }
            try {
                @SuppressWarnings("unchecked")
                final Class<? extends T> klass = (Class<? extends T>) Class.forName(className);
                Constructor<? extends T> constructor;
                Object[] arguments;

                // Look for the default constructor first to ensure backwards compatibility with
                // previous code.
                try {
                    constructor = klass.getConstructor();
                    arguments = new Object[0];
                } catch (NoSuchMethodException nsme1) {
                    // Cannot find a default constructor so if a bundle is supplied then look for
                    // one that takes a Bundle.
                    if (bundle != null) {
                        try {
                            constructor = klass.getConstructor(Bundle.class);
                            arguments = new Object[] {bundle};
                        } catch (NoSuchMethodException nsme2) {
                            // Could not find a constructor that takes a bundle so rethrow the
                            // original exception, remembering to record that this exception was
                            // suppressed.
                            nsme2.initCause(nsme1);
                            throw nsme2;
                        }
                    } else {
                        // Rethrow exception as no bundle was provided.
                        throw nsme1;
                    }
                }
                constructor.setAccessible(true);
                @SuppressWarnings("unchecked")
                final T instance = constructor.newInstance(arguments);
                objects.add(instance);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Could not find extra class " + className);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(
                        "Must have no argument constructor for class " + className);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(className + " does not extend " + type.getName());
            } catch (InstantiationException | InvocationTargetException e) {
                throw new IllegalArgumentException("Failed to create: " + className, e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Failed to create listener: " + className, e);
            }
        }
    }

    public String toString() {
        return "[" + TestNGArgs.class.getSimpleName() + "]\n" +
                "\tdebug = " + debug + "\n" +
                "\tcodeCoverage = " + codeCoverage + "\n" +
                "\tcodeCoveragePath = " + codeCoveragePath;
    }

}
