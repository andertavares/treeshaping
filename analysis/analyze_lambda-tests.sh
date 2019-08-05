#!/bin/bash

# executes analysis/statistics.py to the results of lambda tests if they follow a specific directory structure
# tests A3N either as player 0 or 1, according to what the user specified in parameter $1

if [ "$#" -lt 3 ]; then
    echo "Illegal number of parameters. "
    echo "Please specify: root results dir, train matches, repetition number, and player position in this order."
    exit;
fi
	
echo "trainmatches,map,lambda,features,strategy,position,wins,draws,losses,matches,score,%score"

for map in {basesWorkers8x8,NoWhereToRun9x8,TwoBasesBarracks16x16}; do 
	for l in {0.1,0.3,0.5,0.7,0.9,1.0}; do
		path="$1/$map/fmaterialdistancehp_sCC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M_rwinlossdraw/m$2/d10/a0.01_e0.1_g1.0_l$l/rep$3/test-vs-A3N_p$4.csv" 
		
		if [ -e $path ]; then
			echo "$2,$map,$l,materialdistancehp,all,$4,`python3 analysis/statistics.py $path -p $4`"
		else
			echo "$path does not exist"
		fi
	done
done
