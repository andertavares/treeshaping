#!/bin/bash

if [ "$#" -ne 2 ]; then
	echo "Please inform the initial and final job number."
	exit
fi

#Inicia as execucoes
for i in $(seq ${1} ${2}); do
	echo "Starting $i"
	#sleep 3 && echo "hi" >> "logs/job$i.txt" & # use this line to test (toggle comments with this and the one below)
	python3 filejobclient.py jobqueue >> "logs/job$i.txt" &

	#sleep 1 # 1 second interval to prevent race conditions
done
wait

echo "finished"
