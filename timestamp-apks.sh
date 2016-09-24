#!/bin/sh
BASE_DIR=$(dirname $(readlink -f "$0"))
TIMESTAMP=$(date +"%Y-%m-%d-%H-%M-%S")

ls "$BASE_DIR/app/build/outputs/apk/"*.apk | while read apkname; do
  newapkname=$(echo $apkname | sed -e "s#.apk\$#-$TIMESTAMP.apk#")
  echo "renaming $apkname"
  echo "      to $newapkname"
  mv "$apkname" "$newapkname"
done
