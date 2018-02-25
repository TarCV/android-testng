TestNG runner for Android
=========================

This is an implementation of an Android
[Instrumentation](http://developer.android.com/reference/android/app/Instrumentation.html)
executing unit tests based on [TestNG](http://testng.org/) (the best testing framework for Java).

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
  androidTestCompile 'de.lemona.android:android-testng:X.Y.Z'
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

The runner will *ONLY* look for classes in the package specified by the
`targetPackage` entry in your `AndroidManifest.xml` file.

In [Gradle](https://gradle.org/) this defaults to your application package
plus `....test`.

If no tests can be found, verify the parameter in the manifest of your APK.

For example in our [manifest](src/main/AndroidManifest.xml) the declared
package is `com.github.tarcv.androidtestng`, henceforth after the build processes
it, all our tests will be automatically searched for in the
[`com.github.tarcv.androidtestng.test`](https://github.com/LemonadeLabInc/android-testng/tree/master/src/androidTest/java/de/lemona/android/testng/test)
package.


XML Suites
----------

Unfortunately this is broken on Android
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

In comparison with the original project Guice is removed here.


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
