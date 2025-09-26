# Adhan Alarm

Adhan Alarm is an open-source application designed to help Muslims keep track of prayer times. In a world full of distractions, this app provides a simple and effective way to be alerted when it's time to pray. This is especially useful in regions where the call to prayer (Adhan) is not publicly announced.

The prayer times are calculated based on the user's geographical location (latitude and longitude) and can be customized with various calculation methods.

## Building the Application

You can build a debug version of the application by running the following command in the root of the project:

```bash
./gradlew assembleDebug
```

The generated APK file will be located in the `app/build/outputs/apk/debug/` directory. You can install this APK on an Android device or emulator to test the application.

## Publishing the Application

To publish the application on the Google Play Store, you need to sign it with a release key. Here are the steps to generate a signed APK:

### 1. Generate a Signing Key

First, you need to generate a private signing key using the `keytool` command. If you don't have a key, you can create one by running the following command:

```bash
keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

This will prompt you to create a password for the keystore and the key. Make sure to keep this file in a safe and private location.

### 2. Configure Gradle for Signing

Next, you need to configure the `app/build.gradle` file to sign your application. It's recommended to store your keystore credentials in a separate `keystore.properties` file and not check it into version control.

Create a file named `keystore.properties` in the root of the project with the following content:

```properties
storePassword=your_keystore_password
keyPassword=your_key_password
keyAlias=my-key-alias
storeFile=my-release-key.keystore
```

Then, add the following code to your `app/build.gradle` file to load the properties and configure the release build type:

```groovy
android {
    // ... other configurations

    signingConfigs {
        release {
            if (project.rootProject.file('keystore.properties').exists()) {
                def keystoreProperties = new Properties()
                project.rootProject.file('keystore.properties').withInputStream {
                    keystoreProperties.load(it)
                }
                storeFile project.rootProject.file(keystoreProperties.storeFile)
                storePassword keystoreProperties.storePassword
                keyAlias keystoreProperties.keyAlias
                keyPassword keystoreProperties.keyPassword
            }
        }
    }

    buildTypes {
        release {
            // ... other release configurations
            signingConfig signingConfigs.release
        }
    }
}
```

### 3. Build the Release APK

Finally, you can build the signed release APK by running the following command:

```bash
./gradlew assembleRelease
```

The signed APK will be generated in the `app/build/outputs/apk/release/` directory. You can then upload this file to the Google Play Console to publish your application.