# HEXA Devotional — Android Starter

Kotlin + Jetpack Compose offline-first starter.

## Build APK on GitHub
1. Upload the contents of this ZIP to the root of a new GitHub repository.
2. Open Actions and enable workflows if prompted.
3. Run **Build Android APK** (or push a commit).
4. Open the completed workflow run.
5. Download the `hexa-devotional-debug-apk` artifact.
6. Extract the artifact ZIP and install the APK.

The cloud workflow uses JDK 17 and Gradle 8.9. Android Studio is not required on your phone.

## Included
- Dark-first UI and Day/Dark switch
- Circular settings button at top-left
- Live in-app clock and date
- “श्री गणेशाय नमः” deity menu with ten entries
- Deity categories and sample reader
- Dev Panchang placeholder and selectable default location

## Not implemented yet
The Panchang values are placeholders, not a verified astronomical calculation. The Nataraja 3D scene and final deity images are also placeholders. Replace these with selected/licensed assets in a later stage.

No account, backend, internet permission, or custom encryption is included.
