#!/bin/sh
# generate .properties files for all jars
# so that the sources will be attached in Eclipse
LIBS_DIR=$(dirname $(readlink -f "$0"))
rm -f $LIBS_DIR/*.properties
ls $LIBS_DIR/*.jar | while read file; do
	src_zip=$(echo $file | sed -e 's#\.jar$#-src.zip#')
	if [ -e $src_zip ]; then
		echo "src=$src_zip" >$file.properties
	fi
done