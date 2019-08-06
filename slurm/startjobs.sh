#!/bin/bash

if [ "$#" -ne 1 ]; then
	echo "Please inform the number of jobs to launch"
	exit
fi

#Exibe informações sobre o executável
#/usr/bin/ldd $EXEC

#Inicia as execucoes
for i in $(seq 0 $((${1}-1))); do
	echo "Starting $i"
	./filejobclient.py jobqueue >> "${PWD}/logs/job$i.txt" &
done
wait

echo "finished"
