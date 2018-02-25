package com.github.tarcv.androidtestng.test;

import android.app.Instrumentation;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;

import com.github.tarcv.androidtestng.TestNGArgs;

import junit.framework.Assert;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TestNGArgsTest {

    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

    @Test
    public void testParseArguments() {
        Map<String,String> args = new HashMap<String, String>(){{
            put(TestNGArgs.ARGUMENT_DEBUG,"true");
            put(TestNGArgs.ARGUMENT_COVERAGE,"true");
            put(TestNGArgs.ARGUMENT_COVERAGE_PATH, "somewhere");
        }};

        Bundle bundle = getTestArguments(args);

        TestNGArgs.Builder builder = new TestNGArgs.Builder(instrumentation);
        builder = builder.fromBundle(bundle);
        TestNGArgs testNGArgs = builder.build();
        Assert.assertTrue("argument should be true", testNGArgs.debug);
        Assert.assertTrue("argument should be true", testNGArgs.codeCoverage);
        Assert.assertEquals("argument should be received", testNGArgs.codeCoveragePath, "somewhere");

        args.put(TestNGArgs.ARGUMENT_DEBUG, "false");
        args.put(TestNGArgs.ARGUMENT_COVERAGE,"false");
        args.remove(TestNGArgs.ARGUMENT_COVERAGE_PATH);

        bundle = getTestArguments(args);
        builder = builder.fromBundle(bundle);
        TestNGArgs testNGArgs2 = builder.build();
        Assert.assertFalse("argument should be false", testNGArgs2.debug);
        Assert.assertFalse("argument should be false", testNGArgs2.codeCoverage);
        Assert.assertNull("argument should be null", testNGArgs2.codeCoveragePath);
    }


    private Bundle getTestArguments(Map<String, String> args) {
        Bundle bundle = new Bundle();
        for (String key : args.keySet()) {
            bundle.putString(key, args.get(key));
        }
        return bundle;
    }
}
