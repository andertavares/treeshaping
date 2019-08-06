#!/bin/bash

#Configura a variavel do log - passada por parametro pela execucao do srun
#RUN_LOG=${1}

if [ "$#" -ne 1 ]; then
	echo "Please inform the number of jobs to launch"
	exit
fi

#Exibe informações sobre o executável
#/usr/bin/ldd $EXEC

#Inicia as execucoes
for i in $(seq 0 $((${1}-1))); do
	echo "starting $i"
	sleep 3 && echo "hi $i" &2>1
done
wait

echo "finished"