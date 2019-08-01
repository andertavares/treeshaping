#!/bin/bash

# executes analysis/statistics.py to the results of a3n-vs-a1n if they follow a specific directory structure

# tests A3N either as player 0 or 1, according to what the user specified in parameter $1

file="A3N-vs-A1N.csv" # assumes A3N as player 0
position=0
if [[ $1 = "1" ]]; then
	file="A1N-vs-A3N.csv"
	position=1
fi
	
echo "map,strategy,units,wins,draws,losses,matches,score,%score"

for m in {TwoBasesBarracks16x16,basesWorkers16x16A,basesWorkers24x24A,basesWorkers32x32A,basesWorkers8x8A,BWDistantResources32x32,"(4)BloodBath.scmB",DoubleGame24x24,NoWhereToRun9x8}; do 
	for s in {CE,FC,FE,AV+,HP-,HP+}; do 
		for u in {0..3}; do 
			path=results/$m/s$s-u$u/$file
			if [ -f $path ]; then
				echo "$m,$s,$u,`python3 analysis/statistics.py $path -p $position`"
			fi
		done
	done
done
