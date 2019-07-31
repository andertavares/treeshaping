#!/bin/bash

# executes analysis/statistics.py to the results of a3n-vs-a1n if they follow a specific directory structure


for m in {16x16/TwoBasesBarracks16x16.xml,16x16/basesWorkers16x16A.xml,24x24/basesWorkers24x24A.xml,32x32/basesWorkers32x32A.xml,8x8/basesWorkers8x8A.xml,BWDistantResources32x32.xml,BroodWar/"(4)BloodBath.scmB.xml",DoubleGame24x24.xml,NoWhereToRun9x8.xml}; do 
	for s in {CE,FC,FE,AV+,HP-,HP+}; do 
		for u in {0..3}; do 
			echo "$m,$s,$u,`python3 analysis/statistics.py results/$m/s$s-u$u/{A3N-vs-A1N,A1N-vs-A3N}.csv`"
		done
	done
done