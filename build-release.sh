#!/bin/bash

./gradlew clean
./gradlew IAP:publish
./gradlew app:assembleRelease
adb install --force-queryable -r -d app/build/outputs/apk/release/app-release.apk