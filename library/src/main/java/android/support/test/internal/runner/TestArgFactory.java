package android.support.test.internal.runner;

public class TestArgFactory {
    public static RunnerArgs.TestArg createTestArg(String className, String methodName) {
        return new RunnerArgs.TestArg(className, methodName);
    }

    public static RunnerArgs.TestArg createTestArg(String className) {
        return new RunnerArgs.TestArg(className);
    }
}
