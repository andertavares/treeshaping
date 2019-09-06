#!/bin/bash

if [ "$#" -lt 1 ]; then
	echo "Please inform basedir"
	exit
fi

# recursive traversal vai shopt by: https://unix.stackexchange.com/a/139369/370461
shopt -s globstar dotglob
for file in $1/**/*; do 
	#echo $file
	if [ "${file##*.}" == "choices" ]; then
		#echo "zipping $file"
		zip -Tmj "$file".zip $file  # -T to test, -m to remove the old file, -j to store file w/o full path
	fi 
done 

