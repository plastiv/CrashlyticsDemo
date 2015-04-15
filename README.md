Fabric Crashlytics Kit Setup Demo [![Build Status](https://travis-ci.org/plastiv/CrashlyticsDemo.svg)](https://travis-ci.org/plastiv/CrashlyticsDemo)
===========

Project shows how to:

* setup Fabric Crashlytics kit on Android with Gradle build system
* disable Crashlytics for the debug build type
* hide `apiSecret` and `apiKey` from repository
* support custom source folder structure

About
------

> Crashlytics for Android delivers deep, rich crash reporting across devices in real-time. Crashlytics performs a detailed analysis of every thread on every Android device to identify the most important issues.

Crashlytics is one of the crash reporting tools available to collect crash info from the user devices. Likely you are using one of the alternatives already: Acra, Bugsense, Crittercism, etc. Crashlytics have all the features we would expect from crash reporting tool. Most important are: Proguard deobfuscation, support for both handled and unhandled exceptions with no limits on free account. Crashlytics get acquired by Twitter and after rebranding is called Fabric.io now.

Setup
-----

First of all we need to login at Crashlytics dashboard [www.fabric.com](https://fabric.io/dashboard) and download Android Studio plugin. Successful authorization within Android Studio plugin is required to continue. Plugin will provide the credentials which will be used later at CI and plugin can be deleted after one-time onboarding.

We enable Crashlytics by adding crashlytics plugin and compile dependency to our project build script. Make sure to add Crashlytics maven repository because Crashlytics binaries are not available at maven central.

_app/build.gradle_
```gradle
buildscript {
    repositories {
        maven { url 'https://maven.fabric.io/public' }
    }
    dependencies {
        classpath 'io.fabric.tools:gradle:1.18.0'
    }
}
apply plugin: 'io.fabric'

repositories {
    maven { url 'https://maven.fabric.io/public' }
}
dependencies {
	compile "com.crashlytics.sdk.android:crashlytics:2.2.3"
}
```

Then start Crashlytics from application entry point and that's all is needed to track unhandled exceptions at Crashlytics dashboard.

_src/java/com.github.plastiv.crashlyticsdemo.CrashlyticsDemoApplication_
```java
public class CrashlyticsDemoApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    Fabric.with(this, new Crashlytics());
  }
}
```

Wait a second, if that's all what needed, how does Crashlytics authorize SDK and knows where to send the crashes? Glad you asked.

Crashlytics gradle plugin generates additional files with supporting information which are used later to get `apiKey` from code. After building the project we can see next autogenerated modification to sources:

_app/src/main/assets/crashlytics-build.properties_
```ini
version_name=0.0.1
package_name=com.github.plastiv.crashlyticsdemo
build_id=d8ba607c-1abc-49c5-9f55-2f45b7de083b
version_code=1
app_name=Crashlytics Demo
```

_app/src/main/res/values/com_crashlytics_export_strings.xml_
```xml
<resources>
    <string name="com.crashlytics.android.build_id" translatable="false">d8ba607c-1abc-49c5-9f55-2f45b7de083b</string>
</resources>
```

_app/src/main/AndroidManifest.xml_

```xml
<meta-data
      android:name="com.crashlytics.ApiKey"
      android:value="cc238b2a4866ceb061b008839cdb49a8b77"/>
```

Both `crashlytics-build.properties` and `com_crashlytics_export_strings.xml` are autogenerated files and should be excluded from source control as they would be modified on each build by Crashlytics plugin. Meta data at AndroidManifest not needed to be hardcoded as it would be autogenerated also.

Version name, code and package will be loaded from gradle build script. Crashlytics credentials can be controlled from `crashlytics.properties` file.

_app/crashlytics.properties_
```ini
apiSecret=7c9df6d057e7bb62b17ab364e8115a75fcf7430873b6274bb094d1a8adb
apiKey=cc238b2a4866ceb061b008839cdb49a8b77
```

We can get `apiSecret` and `apiKey` from `~/.crashlytics/User.json` where they were created after successful login with Crashlytics plugin GUI for Android Studio. It makes sense to keep this values private without sharing them at repository so we added `crashlytics.properties` file to the `.gitignore` too.

_.gitignore_
```ini
# Crashlytics plugin
com_crashlytics_export_strings.xml
crashlytics.properties
crashlytics-build.properties
```

Disabling Crashlytics for the debug builds
---

We may want to disable Crashlytics crash logging during debugging on local machine. Because all crashes are directly accessible from adb logcat with no need to use server infrastructure and mail notifications. This could be done with gradle build types or flavors and `ext.enableCrashlytics` parameter.

_app/build.gradle_
```gradle
android {
    buildTypes {
        debug {
            // disable crashlytics
            buildConfigField "boolean", "USE_CRASHLYTICS", "false"
            ext.enableCrashlytics = false
        }
        release {
            // enable crashlytics
            buildConfigField "boolean", "USE_CRASHLYTICS", "true"
            ext.enableCrashlytics = true
        }
    }
}
```

After disabling Crashlytics gradle plugin with `ext.enableCrashlytics=false` it stops to create `crashlytics-build.properties` and `com_crashlytics_export_strings.xml` files and starting Crashlytics from code results in exception due to missing information. We wrap `Crashlytics.start()` with declared build config field to prevent it initialization.

```java
@Override
public void onCreate() {
    if (BuildConfig.USE_CRASHLYTICS) {
        Fabric.with(this, new Crashlytics());
    }
}
```

Providing apiKey and apiSecret on CI
------

We may want to keep your `apiKey` and `apiSecret` private without sharing it within git repository. But we still need this values to be able to run build from CI or other team members machines. We can use the same technique here as when we hiding signingConfig keys from a repository by providing them with project properties.

_app/build.gradle_
```gradle
afterEvaluate {
    initCrashlyticsPropertiesIfNeeded()
}

def initCrashlyticsPropertiesIfNeeded() {
    def propertiesFile = file('crashlytics.properties')
    if (!propertiesFile.exists()) {
        def commentMessage = "This is autogenerated crashlytics property from system environment to prevent key to be committed to source control."
        ant.propertyfile(file: "crashlytics.properties", comment: commentMessage) {
            entry(key: "apiSecret", value: crashlyticsdemoApisecret)
            entry(key: "apiKey", value: crashlyticsdemoApikey)
        }
    }
}
```

Additional task is added which autogenerates `crashlytics.properties` file with `crashlyticsdemoApisecret` and `crashlyticsdemoApikey` project properties. With gradle it is really convenient to set project properties:

* First of all we can hardcode the properties in the script itself.

`crashlyticsdemoApikey=cc238b2a4866c96030`

* Or use the -P command-line argument to pass a property to the build script.

`gradlew assemble -PcrashlyticsdemoApikey=cc238b2a4866c96030`

* Or define a `gradle.properties` file and set the property in this file. We can place the file in our project directory or in the `<USER_HOME>/.gradle directory`. The properties defined in the property file in our home directory take precedence over the properties defined in the file in our project directory.

_gradle.properties_ or _~/.gradle/gradle.properties_

```ini
crashlyticsdemoApisecret=0cf7c9df6d057e7bb62b1427ab364e8115a75fcf7430873b6274bb094d1a8adb
crashlyticsdemoApikey=cc238b2a4866c96030
```

* Or use an environment variable of which the name starts with` ORG_GRADLE_PROJECT_` followed by the property name.

`export ORG_GRADLE_PROJECT_crashlyticsdemoApikey=cc238b2a4866c96030`

* Or use the Java system property that starts with `org.gradle.project.` followed by the property name.

`java -Dorg.gradle.project.crashlyticsdemoApikey="cc238b2a4866c96030"`

Custom source sets
-------

We can override Crashlytics source sets when default source folder structure is overrided with gradle so Crashlytics gradle plugin knows where to place autogenerated files. It is useful for projects compatible with Eclipse. 

_app/build.gradle_
```gradle
crashlytics.manifestPath = "relative/path/to/manifest"
crashlytics.resPath = "relative/path/to/res"
crashlytics.assetsPath = "relative/path/to/assets"
```

Run demo app
-------

Check yourself, that project doesn't have any of `crashlytics.properties`, `crashlytics-build.properties`, `com_crashlytics_export_strings.xml`
or meta tag at `AndroidManifest.xml` but still both `debug` and `release` apks compiles fine. Because of `apiKey` and `apiSecret`
are provided with environment variables.

[![Environment variables](/images/travis-ci-environment-variables.png)](https://travis-ci.org/plastiv/CrashlyticsDemo)
