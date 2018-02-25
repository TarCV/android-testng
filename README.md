TestNG runner for Android
=========================

This is an implementation of an Android
[Instrumentation](http://developer.android.com/reference/android/app/Instrumentation.html)
executing unit tests based on [TestNG](http://testng.org/) (Java testing framework that is more suitable for integration and complex automation tests than JUnit).

Usage
-----

Depending on your build system, your mileage might vary, but with
[Gradle](https://gradle.org/) the only required changes to your build files
should be limited to adding our repository,
then declaring the dependency and modifying your `testInstrumentationRunner`:

```groovy
// TO BE UPLOADED

// TestNG dependency, remember to update to the latest version
dependencies {
  androidTestCompile 'com.github.tarcv:android-testng:X.Y.Z'
}

// Android setup
android {
  defaultConfig {
    testInstrumentationRunner 'com.github.tarcv.androidtestng.TestNGRunner'
  }
}
```


Packages
--------

The runner will look for classes everywhere in your classpath - same as standard ASTL library


XML Suites
----------

**Unfortunately this is BROKEN on Android**

~~Test suites can also be defined using a [`testng.xml`](http://testng.org/doc/documentation-main.html#testng-xml)
file from your [`assets`](src/androidTest/assets) directory.~~

~~This is useful when tests do not reside in the standard application package
plus `....test`.~~

~~One caveat, though, is that the `<package />` element does not work _(yet)_,
as TestNG expects JAR files, while Android bundles everything into a DAX file.~~

~~For an example see the [`testng.xml`](src/androidTest/assets/testng.xml) file
included alongside these sources.~~


Contexts
--------

In order to have access to the Android's application
[Context](http://developer.android.com/reference/android/content/Context.html)
please use ASTL standard [InstrumentationRegistry#getTargetContext](https://developer.android.com/reference/android/support/test/InstrumentationRegistry.html#getTargetContext()).

In comparison with the original project Guice is absent here.


Options
--------

The options to enable some features on testing are same as [adb instrument](https://developer.android.com/studio/test/command-line.html). Current supported options are as below:
- debug
- coverage
- coverageFile

If you need to run tests from Android Studio, please use [Android Tests](https://www.jetbrains.com/help/idea/2016.1/run-debug-configuration-android-test.html) Configuration.

License
-------

Licensed under the [Apache License version 2](LICENSE.md)
