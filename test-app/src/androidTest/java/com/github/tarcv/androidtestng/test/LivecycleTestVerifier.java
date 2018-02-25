package com.github.tarcv.androidtestng.test;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LivecycleTestVerifier {

    @Test(dependsOnGroups="Lifecycle")
    public void verifyLifecycle() {
        Assert.assertEquals(LifecycleTest.EVENTS.size(), 12, LifecycleTest.EVENTS.toString());
        Assert.assertEquals(LifecycleTest.EVENTS.get( 0), "BEFORE_SUITE");
        Assert.assertEquals(LifecycleTest.EVENTS.get( 1), "BEFORE_TEST");
        Assert.assertEquals(LifecycleTest.EVENTS.get( 2), "BEFORE_CLASS");
        Assert.assertEquals(LifecycleTest.EVENTS.get( 3), "BEFORE_GROUPS");
        Assert.assertEquals(LifecycleTest.EVENTS.get( 4), "BEFORE_METHOD");
        Assert.assertEquals(LifecycleTest.EVENTS.get( 5), "FIRST_TEST");
        Assert.assertEquals(LifecycleTest.EVENTS.get( 6), "AFTER_METHOD");
        Assert.assertEquals(LifecycleTest.EVENTS.get( 7), "BEFORE_METHOD");
        Assert.assertEquals(LifecycleTest.EVENTS.get( 8), "SECOND_TEST");
        Assert.assertEquals(LifecycleTest.EVENTS.get( 9), "AFTER_METHOD");
        Assert.assertEquals(LifecycleTest.EVENTS.get(10), "AFTER_GROUPS");
        Assert.assertEquals(LifecycleTest.EVENTS.get(11), "AFTER_CLASS");
    }

}
