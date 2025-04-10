VERSION 0.8

build:
    FROM eclipse-temurin:17-jdk
    WORKDIR /project
    RUN apt-get update >/dev/null 2>&1 && apt-get -y install wget unzip >/dev/null 2>&1
    RUN echo "setting up Android SDK"
    ENV ANDROID_SDK_ROOT="/sdk"
    ENV ANDROID_HOME="$ANDROID_SDK_ROOT"
    ENV PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"
    # download URL from https://developer.android.com/studio?hl=en#command-line-tools-only:
    RUN curl -s https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /cmdline-tools.zip && \
        mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
        unzip -q /cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools/ && \
        mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest && \
        rm -f /cmdline-tools.zip
    RUN mkdir -p $ANDROID_SDK_ROOT/licenses \
        && echo "8933bad161af4178b1185d1a37fbf41ea5269c55\nd56f5187479451eabf01fb78af6dfcb131a6481e\n24333f8a63b6825ea9c5514f83c2829b004d1fee" > $ANDROID_SDK_ROOT/licenses/android-sdk-license \
        && echo "84831b9409646a918e30573bab4c9c91346d8abd\n504667f4c0de7af1a06de9f4b1727b84351f2910" > $ANDROID_SDK_ROOT/licenses/android-sdk-preview-license \
        && yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null
    RUN $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" >/dev/null
    COPY .git .git
    COPY gradle gradle
    COPY metadata metadata
    COPY app app
    COPY build.gradle ./
    COPY gradle.properties ./
    COPY gradlew ./
    COPY settings.gradle ./
    RUN TZ=Europe/Berlin ./gradlew clean test
    RUN TZ=Europe/Berlin ./gradlew assemble
    SAVE ARTIFACT app/build AS LOCAL build

build-and-release-on-github:
    ARG --required GITHUB_TOKEN
    BUILD +build
    FROM ubuntu:noble
    WORKDIR /project
    RUN apt-get update >/dev/null 2>&1 && apt-get -y install curl gpg >/dev/null 2>&1
    RUN curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | gpg --dearmor -o /usr/share/keyrings/githubcli-archive-keyring.gpg
    RUN echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | tee /etc/apt/sources.list.d/github-cli.list > /dev/null
    RUN apt-get update >/dev/null 2>&1 && apt-get -y install gh >/dev/null 2>&1
    COPY .git .git
    COPY +build/build build
    RUN --push export BRANCH=$(git rev-parse --abbrev-ref HEAD); \
               if [ "$BRANCH" != "main" -a "$BRANCH" != "master" ]; then \
                  echo "not releasing, we're on branch $BRANCH"; \
                  exit 0; \
               fi; \
               export TAG=$(git tag --points-at HEAD); \
               echo TAG: $TAG; \
               export MATCH=$(echo "$TAG" | grep -e "^v"); \
               if [ -n "$MATCH" ]; then \
                 export RELEASE_HASH=$(git rev-parse HEAD); \
                 echo RELEASE_HASH: $RELEASE_HASH; \
                 export FILES=$(ls build/outputs/apk/release/*.apk); \
                 echo FILES: $FILES; \
                 gh release create $TAG --target $RELEASE_HASH --title $TAG --notes "see changelog: https://zephyrsoft.org/trackworktime/history" $FILES; \
               else \
                 echo "not releasing, no eligible tag found"; \
               fi
