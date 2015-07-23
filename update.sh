#!/bin/bash

function bumpVersion(){
	perl -pe 's/(versionCode )(\d+)/$1.($2+1)/e' < app/build.gradle > tmp
	perl -pe 's/(versionName \"\d+\.\d+\.)(\d+)(\")/$1.($2+1).$3/e' < tmp > app/build.gradle
	rm tmp
}

bumpVersion

./gradlew publishRelease --stacktrace || exit

bumpVersion # again for development purposes

git add .
git ci -m "bump version"
git push

VERSION=$(grep -o  "versionName.*" app/build.gradle| cut -d" " -f2 | sed 's/"//g')
git tag -a "release/$VERSION" -m "release/$VERSION"


