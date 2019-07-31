#!/bin/bash

# zips trace (replay) files geenrated by A3N vs A1N tests to save disk space


for m in {TwoBasesBarracks16x16,basesWorkers16x16A,basesWorkers24x24A,basesWorkers32x32A,basesWorkers8x8A,BWDistantResources32x32,"(4)BloodBath.scmB",DoubleGame24x24,NoWhereToRun9x8}; do 
	for s in {CE,FC,FE,AV+,HP-,HP+}; do 
		for u in {0..3}; do 
			dir="results/$m/s$s-u$u/"
			if [ -d $dir ]; then
				zip -Tm  $dir/replays.zip $dir/*.trace #-m deletes the original and -T ensures this is done after testing the integrity of the .zip file
			fi
		done
	done
done