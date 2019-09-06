#!/bin/bash

if [ "$#" -lt 1 ]; then
	echo "Please inform basedir"
	exit
fi

#for f in $1/*; do
#	ext=${f##*.}
#	echo $ext
#	if [ "$ext" == "choices" ]; then
#		echo $f
#	fi
#done

#find "$1" -type f -exec sh -c '
#  echo "$0"
#  ext=${0##*.}
#	if [ "$ext" == "choices" ]; then
#		#echo "zipping $file"
#		zip -Tmj "$0".zip $0  # -T to test, -m to remove the old file, -j to store file w/o full path
#	fi
#' {} \;
 
# the black magic of traverse via find is from https://stackoverflow.com/a/18897659/1251716
# the following gives a 'is a directory' message, but it was the only way find worked...
echo "Please ignore the 'Is a directory' message."
find `${1}` -print0 | while IFS= read -r -d '' file 
do 
	if [ "${file##*.}" == "choices" ]; then
		#echo "zipping $file"
		zip -Tmj "$file".zip $file  # -T to test, -m to remove the old file, -j to store file w/o full path
	fi
done


