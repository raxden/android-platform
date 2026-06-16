# https://github.com/ben-manes/gradle-versions-plugin

function dependencyUpdates {
    cd ..
  ./gradlew dependencyUpdates -Drevision=release
}

dependencyUpdates
