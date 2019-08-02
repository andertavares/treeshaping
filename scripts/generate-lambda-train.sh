#!/bin/bash

# outputs the necessary commands to run the training  experiments
# parameter: train matches & number of repetitions

for map in {basesWorkers8x8,NoWhereToRun9x8,TwoBasesBarracks16x16}; do

	for l in {0.1,0.3,0.5,0.7,0.9,1.0}; do
		expstr="./train.sh -c config/$map.properties -d results --train_matches $1 \
		--decision_interval 10 -s all -e materialdistancehp -r winlossdraw \
		--td_lambda $l --checkpoint 10";
		
		echo $expstr
		
		for r in {0..$2}; do 
			echo $expstr
		done
	done
done
