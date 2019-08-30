#!/usr/bin/python3

import pandas as pd
import numpy as np
import argparse
import sys
import os
import statistics
sys.path.append('scripts') # dirty trick to do the import below
import generate_experiments as experiments
import commandlog

'''
def parse_args():
    parser = argparse.ArgumentParser(
        description='Analyses the results of lambda tests'
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
        '-t', '--train-matches', required=True, type=int, 
        help='Number of train matches'
    )
    
    parser.add_argument(
        '-l', '--lambdas', help='List of lambda values', nargs='+',
        default=[0.1, 0.3, 0.5, 0.7, 0.9]
    )
    
    parser.add_argument(
        '-s', '--strategies', help='List of strategies (comma-separated list without spaces)', 
        default='CC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M'
    )
    
    parser.add_argument(
        '-r', '--rep', help='Repetition number to analyse', type=int,
        default=0
    )
    
    parser.add_argument(
        '--silent', help='Does not log command to the log file', action='store_true',
    )
    
    parser.add_argument(
        '-m', '--maps', help='List of maps', nargs='+',
        default=['basesWorkers8x8', 'NoWhereToRun9x8', 'TwoBasesBarracks16x16']
    )

    return parser.parse_args()
'''


def raw_analysis(params): #(basedir, initial_rep, final_rep, trainmatches, maps, strategies, lambdas, stdout):
    
    for player in [0, 1]:

        mode = 'w' if params.overwrite else 'a'
        outstream = open('%s_p%d.csv' % (params.output, player), mode) if params.output is not None else sys.stdout

        outstream.write(
            'trainmatches,map,lambda,features,strategy,position,wins,draws,losses,matches,score,%score\n'
        )

        for m in params.maps:
            for lam in params.lambdas:
                for strat in params.strategies:
                    path = os.path.join(
                        params.basedir, m, 'fmaterialdistancehp_s%s_rwinlossdraw' % strat,
                        'm%d' % params.train_matches, 'd10', 'a0.01_e0.1_g1.0_l%s' % lam,  # TODO allow custom alpha-epsilon-gamma
                        'rep%d',  # this number will be set when building the file list
                        'test-vs-A3N_p%d.csv' % player
                    )

                    file_list = [path % rep for rep in range(params.initial_rep, params.final_rep+1)]  # +1 to include the final rep

                    # filters out non-existent files
                    for f in file_list:
                        if not os.path.exists(f):
                            print('%s does not exist' % f)  # this one always go to stdout
                            file_list.remove(f)

                    if len(file_list) > 0:  # if there are remaining files after filtering, analyse them
                        outstream.write('%d,%s,%s,materialdistancehp,%s,%d,%s\n' % (
                            params.train_matches, m, lam, strat.replace(',', ' '), player,
                            ','.join([str(x) for x in statistics.average_score(file_list, player)])
                        ))

        if params.output is not None: # prevents closing sys.stdout
            outstream.close()
    

if __name__ == '__main__':
    parser = experiments.arg_parser('Analyse raw results of test matches')
    args = parser.parse_args()

    if not args.silent:
        commandlog.log_command(' '.join(sys.argv), 'analysis')
    
    raw_analysis(args)
    
    if args.output is not None:  # also calls a3n-vs-a1n-table.generate_table if -q was omitted
        print('Results are in .csv files starting with %s' % args.output)

