package com.github.tarcv.androidtestng.test;

import android.support.test.InstrumentationRegistry;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class AndroidComponentsTest {

    @BeforeSuite
    public void beforeSuite() {
        Assert.assertNotNull(InstrumentationRegistry.getTargetContext(), "Non-null context in @BeforeSuite");
        Assert.assertNotNull(InstrumentationRegistry.getInstrumentation(), "Non-null instrumentation in @BeforeSuite");
    }

    @BeforeGroups(groups="Components")
    public void beforeGroups() {
        Assert.assertNotNull(InstrumentationRegistry.getTargetContext(), "Null context in @BeforeGroups");
        Assert.assertNotNull(InstrumentationRegistry.getInstrumentation(), "Null instrumentation in @BeforeGroups");
    }

    @BeforeClass
    public void beforeClass() {
        Assert.assertNotNull(InstrumentationRegistry.getTargetContext(), "Null context in @BeforeClass");
        Assert.assertNotNull(InstrumentationRegistry.getInstrumentation(), "Null instrumentation in @BeforeClass");
    }

    @BeforeTest
    public void beforeTest() {
        Assert.assertNotNull(InstrumentationRegistry.getTargetContext(), "Null context in @BeforeTest");
        Assert.assertNotNull(InstrumentationRegistry.getInstrumentation(), "Null instrumentation in @BeforeTest");
    }

    @Test(groups="Components")
    public void testComponents() {
        Assert.assertNotNull(InstrumentationRegistry.getTargetContext(), "Null context in @Test");
        Assert.assertNotNull(InstrumentationRegistry.getInstrumentation(), "Null instrumentation in @Test");
    }

}
