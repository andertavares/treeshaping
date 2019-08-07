#!/usr/bin/python3

import pandas as pd
import numpy as np
import argparse
import sys
import os
import statistics

def parse_args():
    parser = argparse.ArgumentParser(
        description='Analyses the results of A3N vs A1N'
    )

    parser.add_argument(
        'basedir', help='Base directory or results',
    )

    parser.add_argument(
        '-m', '--maps', help='List of maps where the games were played', nargs='+',
        default=[
            'TwoBasesBarracks16x16', 'basesWorkers16x16A', 'basesWorkers24x24A', 
            'basesWorkers8x8A', 'BWDistantResources32x32', 
            'DoubleGame24x24', 'NoWhereToRun9x8'
        ]
    )
    
    parser.add_argument(
        '-s', '--strategies', nargs='+', 
        help='List of strategies that were tested by A3N', 
        default=['CE', 'FC', 'FE', 'AV+', 'HP-', 'HP+']
    )
    
    parser.add_argument(
        '-p', '--position', type=int, default=0, help='A3N player position.',
        choices=[0, 1]
    )
    
    return parser.parse_args()
    
def run(basedir, maps, strategies, position):
    filename = 'A3N-vs-A1N.csv' if position == 0 else 'A1N-vs-A3N.csv'
    
    print('map,strategy,units,wins,draws,losses,matches,score,%score')
    
    for m in maps:
        for s in strategies:
            for u in range(0, 4):
                path = os.path.join(basedir, m, 's%s-u%d' % (s, u), filename)
                
                if os.path.exists(path):
                    print('%s,%s,%d,%s' % (
                        m, s, u, ','.join([str(x) for x in statistics.average_score([path], position)])    
                    ))
                else:
                    print('%s does not exist' % path)
    

if __name__ == '__main__':
    args = parse_args()
    run(args.basedir, args.maps, args.strategies, args.position)

