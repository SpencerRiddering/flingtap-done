#!/bin/bash
#
# --------------------------------------------------
# Release build script
# --------------------------------------------------

source ./api_keys_release.sh 
# TODO: Add bash script prompt for keystore info

#  -Dsign.keystore=~/.android/debug.keystore \
#  -Dsign.alias=androiddebugkey \
#  -Dsign.storepass=android \
#  -Dsign.keypass=android \

mvn clean templating:filter-sources install \
  -Dcommand.line.flurryId=$FLURRY_API_KEY \
  -Dcommand.line.googleMapsApiKey=$GOOGLE_MAPS_API_KEY \
  -Dcommand.line.googleBackupApiKey=$GOOGLE_BACKUP_API_KEY \
  -Dcommand.line.productionMode=true \
  -Dcommand.line.admobPubId=$ADMOB_PUB_ID \
  -P release
