#!/bin/bash
#
# --------------------------------------------------
# Test build script
# --------------------------------------------------

source ./api_keys_test.sh 

mvn clean templating:filter-sources install \
  -Dsign.keystore=~/.android/debug.keystore \
  -Dsign.alias=androiddebugkey \
  -Dsign.storepass=android \
  -Dsign.keypass=android \
  -Dmaven.test.skip=false \
  -Dcommand.line.flurryId=$FLURRY_API_KEY \
  -Dcommand.line.googleMapsApiKey=$GOOGLE_MAPS_API_KEY \
  -Dcommand.line.googleBackupApiKey=$GOOGLE_BACKUP_API_KEY \
  -Dcommand.line.admobPubId=$ADMOB_PUB_ID \
  -Dcommand.line.productionMode=false \
  -P standard
