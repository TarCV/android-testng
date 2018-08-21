package com.github.tarcv.androidtestng;

import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.TestException;

import java.lang.reflect.Method;
import java.util.List;

public class MethodRulesHookable implements IHookable {
    @Override
    public void run(final IHookCallBack callBack, final ITestResult testResult) {
        Object target = testResult.getInstance();
        Method method = testResult.getMethod().getMethod();

        TestClass junitClass = new TestClass(target.getClass());

        List<TestRule> testRules = getTestRules(junitClass, target);
        List<MethodRule> methodRules = getMethodRules(junitClass, target);
        if (!testRules.isEmpty() || !methodRules.isEmpty()) {
            executeWithRules(callBack, testResult, target, method, testRules, methodRules);
        } else {
            callBack.runTestMethod(testResult);
        }
    }

    private void executeWithRules(
            final IHookCallBack callBack, final ITestResult testResult,
            Object target, Method method,
            List<TestRule> testRules, List<MethodRule> methodRules) {
        FrameworkMethod junitMethod = new FrameworkMethod(method);

        Statement result = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                callBack.runTestMethod(testResult);
            }
        };

        result = withMethodRules(junitMethod, testRules, target, result, methodRules);
        result = withTestRules(junitMethod, testRules, result);
        try {
            result.evaluate();
        } catch (Throwable throwable) {
            throw new TestException(throwable);
        }
    }

    protected List<TestRule> getTestRules(TestClass junitClass, Object target) {
        List<TestRule> result = junitClass.getAnnotatedMethodValues(target,
                Rule.class, TestRule.class);

        result.addAll(junitClass.getAnnotatedFieldValues(target,
                Rule.class, TestRule.class));

        return result;
    }

    private Statement withMethodRules(FrameworkMethod method, List<TestRule> testRules,
                                      Object target, Statement result, List<MethodRule> methodRules) {
        for (org.junit.rules.MethodRule each : methodRules) {
            if (!testRules.contains(each)) {
                result = each.apply(result, method, target);
            }
        }
        return result;
    }

    /**
     * @param target the test case instance
     * @return a list of MethodRules that should be applied when executing this
     *         test
     */
    protected List<MethodRule> getMethodRules(TestClass junitClass, Object target) {
        List<MethodRule> rules = junitClass.getAnnotatedMethodValues(target,
                Rule.class, MethodRule.class);

        rules.addAll(junitClass.getAnnotatedFieldValues(target,
                Rule.class, MethodRule.class));

        return rules;
    }

    private Statement withTestRules(FrameworkMethod method, List<TestRule> testRules,
                                    Statement statement) {
        return testRules.isEmpty() ? statement :
                new RunRules(statement, testRules, describeChild(method));
    }

    private Description describeChild(FrameworkMethod method) {
        return null;
    }
}
