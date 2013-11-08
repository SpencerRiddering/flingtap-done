# How to build #

## One-time setup ##
1. Install dependency jars in local maven repo (flingtap-done/maven-repo-3rd-party/repository/)
    * Download jar files for VeeCheck, Admob, and Flurry. Google Maps can be found in your Android SDK directory.
    * Note: For each dependency jar, you must update the version (-Dversion=) value here and in the dependency section of flingtap-done/pom.xml and flingtap-done-common/pom.xml. The version should match the version of the jar you downloaded. 

    `mvn install:install-file -Dfile=$ABSOLUTE_PATH_TO_JAR/flurryagent.jar -DgroupId=com.flurry -DartifactId=FlurryAgent -Dversion=1.0 -Dpackaging=jar -DcreatePom=true -DlocalRepositoryPath=$ABSOLUTE_PATH_TO/flingtap-done/maven-repo-3rd-party/repository/ -DcreateChecksum=true`
    
    `mvn install:install-file -Dfile=$ABSOLUTE_PATH_TO_JAR/veecheck.jar -DgroupId=com.tomgibara.android.veecheck -DartifactId=Veecheck -Dversion=1.0 -Dpackaging=jar -DcreatePom=true -DlocalRepositoryPath=$ABSOLUTE_PATH_TO/flingtap-done/maven-repo-3rd-party/repository/ -DcreateChecksum=true`
    
    `mvn install:install-file -Dfile=$ABSOLUTE_PATH_TO_JAR/GoogleAdMobAdsSdk-6.4.1.jar -DgroupId=com.google.android.admob -DartifactId=admob -Dversion=6.4.1 -Dpackaging=jar -DlocalRepositoryPath=$ABSOLUTE_PATH_TO/flingtap-done/maven-repo-3rd-party/repository/ -DcreatePom=true -DcreateChecksum=true`
    
    `mvn install:install-file -DlocalRepositoryPath=$ABSOLUTE_PATH_TO/flingtap-done/maven-repo-3rd-party/repository/ -Dfile=$ANDROID_HOME/add-ons/addon-google_apis-google-4/libs/maps.jar -DgroupId=com.google.android.maps -DartifactId=maps -Dversion=4_r2 -Dpackaging=jar -DcreatePom=true -DcreateChecksum=true`
    
2. Copy api_keys_template.sh to make api_keys_test.sh and api_keys_release.sh in the project root directory
3. Acquire a Google Backup API Key, and Google Maps API Key (for both dev and release signing keys) and add them to api_keys_test.sh and  api_keys_release.sh 
4. Optionally acquire a Flurry ID and AdMob Publisher ID and add them to api_keys_release.sh

## Each build ##
1. Execute 
    * `build-debug.sh`  for a debug build.
    * `build-test.sh`  for a test build.
    * `build-release.sh`  for a release build.
