package com.github.tarcv.androidtestng;

import android.app.Instrumentation;
import android.os.Bundle;
import android.support.test.internal.runner.TestRequestBuilder;
import android.util.Log;

import org.testng.collections.Lists;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public class TestNGSuiteBuilder extends TestRequestBuilder {
    private static final String TAG = "NgSuiteBuilder";

    public TestNGSuiteBuilder(Instrumentation instr, Bundle bundle) {
        super(instr, bundle);
    }

    /**
     * Builds the TestNG suite based on provided data.
     *
     * @throws java.lang.IllegalArgumentException if provided set of data is not valid
     */
    public XmlSuite buildNgSuite() {
        // Currently included packages/classes not supported, so skip most of original method code
        Collection<String> classNames;

        removeTestPackage("org.testng"); // as we don't have access to DEFAULT_EXCLUDED_PACKAGES
        classNames = getClassNamesFromClassPath();

        // Our XML suite for running tests
        final XmlSuite xmlSuite = new XmlSuite();

        // Prepare our XML test and list of classes
        final XmlTest xmlTest = new XmlTest(xmlSuite);
        final List<XmlClass> xmlClasses = Lists.newArrayList();

        for (String className : classNames) {
            Log.d(TAG, "Adding potential test class " + className);

            try {
                // Currently setting custom classloader, so using the default one
                Class<?> loadedClass = Class.forName(className);
                xmlClasses.add(new XmlClass(loadedClass, true));
            } catch (ClassNotFoundException e) {
                String errMsg = String.format("Could not find class: %s", className);
                Log.e(TAG, errMsg);
            }
        }

        if (! xmlClasses.isEmpty()) {
            xmlTest.setXmlClasses(xmlClasses);
        }

        return xmlSuite;
    }

    private Collection<String> getClassNamesFromClassPath() {
        try {
            Method method = TestRequestBuilder.class.getDeclaredMethod(
                    "getClassNamesFromClassPath");
            method.setAccessible(true);
            return (Collection<String>) method.invoke(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
