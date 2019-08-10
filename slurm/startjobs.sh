#!/bin/bash

if [ "$#" -ne 1 ]; then
	echo "Please inform the number of jobs to launch"
	exit
fi

#Inicia as execucoes
for i in $(seq 0 $((${1}-1))); do
	echo "Starting $i"
	#sleep 3 && echo "hi" >> "logs/job$i.txt" &
	python3 filejobclient.py jobqueue >> "logs/job$i.txt" &
	sleep 1 # 1 second interval to prevent race conditions
done
wait

echo "finished"
