#!/bin/bash

# outputs the necessary commands to run the training  experiments
# parameter: r

if [ "$#" -lt 5 ]; then
    echo "Illegal number of parameters. "
    echo "Please specify: root results dir, train matches, test matches, initial and final repetitions, in this order."
    exit;
fi

for map in {basesWorkers8x8,NoWhereToRun9x8,TwoBasesBarracks16x16}; do

	for l in {0.1,0.3,0.5,0.7,0.9,1.0}; do
		expstr="./test.sh --test_matches $3 -i $4 -f $5 -d \
		$1/$map/fmaterialdistancehp_sCC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M_rwinlossdraw/m$2/d10/a0.01_e0.1_g1.0_l$l" 
		
		echo $expstr
		
	done
done
