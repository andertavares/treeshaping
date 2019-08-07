#!/usr/bin/python3

import pandas as pd
import numpy as np
import argparse
import sys
import os
import statistics
from a3n_vs_a1n_table import generate_table

# TODO: generate the 'average' table as well

def parse_args():
    parser = argparse.ArgumentParser(
        description='Analyses the results of A3N vs A1N'
    )

    parser.add_argument(
        'basedir', help='Base directory or results',
    )

    parser.add_argument(
        '-q', '--stdout', action='store_true', 
        default=False,
        help='Quiet: output to stdout rather than to a file',
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
        '--metric', required=False, choices=['wins','draws','losses','matches','score','%score'], 
        default='score', 
        help='Which metric should appear in the table output (only works if -q is omitted)'
    )
    
    return parser.parse_args()
    
def raw_analysis(basedir, maps, strategies, stdout):
    
    for player in [0, 1]:
        outfile = os.path.join(basedir, 'A3N_p%d.csv' % player)
        
        outstream = open(outfile, 'w') if not stdout else sys.stdout 
        outstream.write('map,strategy,units,wins,draws,losses,matches,score,%score\n')
        
        for m in maps:
            for s in strategies:
                for u in range(0, 4):
                    filename = 'A3N-vs-A1N.csv' if player == 0 else 'A1N-vs-A3N.csv'
                    path = os.path.join(basedir, m, 's%s-u%d' % (s, u), filename)
                    
                    if os.path.exists(path):
                        outstream.write('%s,%s,%d,%s\n' % (
                            m, s, u, ','.join([str(x) for x in statistics.average_score([path], player)])    
                        ))
                    else:
                        print('%s does not exist' % path) # this one always go to stdout
        
        if not sys.stdout: # prevents closing sys.stdout
            outstream.close()
    

if __name__ == '__main__':
    args = parse_args()

    raw_analysis(args.basedir, args.maps, args.strategies, args.stdout)
    
    if not args.stdout: # also calls a3n-vs-a1n-table.generate_table if -q was omitted
        out_table_format = os.path.join(args.basedir, 'A3N_p%d_table.csv')
        
        for player in [0, 1]:
            infile = os.path.join(args.basedir, 'A3N_p%d.csv' % player)
            outfile = out_table_format % player
            generate_table(infile, outfile, args.metric)
            
        if args.metric != '%score':
            # adds the two tables into one
            df1 = pd.read_csv(out_table_format % 0)
            df2 = pd.read_csv(out_table_format % 1)
            
            # replaces columns 2:6 on df1 with the sum of these columns on both dataframes
            # TODO it is generating a first column with unnecessary integer indices
            df1.loc[:, 2:6] = df1.iloc[:, 2:6] + df2.iloc[:, 2:6]
            df1.to_csv(os.path.join(args.basedir, 'A3N_table_sum.csv'))
            
        print('Results are in .csv files at %s' % args.basedir)

