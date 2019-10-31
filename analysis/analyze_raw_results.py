#!/usr/bin/python3

import pandas as pd
import numpy as np
import argparse
import sys
import os
import stats
sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'scripts')) # dirty trick to do the import below
import generate_experiments as experiments
import commandlog


def raw_analysis(params):
    
    for player in [0, 1]:

        mode = 'w' if params.overwrite else 'a'
        outstream = open('%s_p%d.csv' % (params.output, player), mode) if params.output is not None else sys.stdout

        outstream.write(
            'trainmatches,map,lambda,features,strategy,position,wins,draws,losses,matches,score,%score\n'
        )

        for mapname, interval, alpha, gamma, lamda, epsilon, strats, train_opp in experiments.cartesian_product(vars(params)):
            path = os.path.join(
                params.basedir, mapname, 'fmaterialdistancehp_s%s_rwinlossdraw' % strats,
                'm%d' % params.train_matches, 'd%d' % interval,
                'a%s_e%s_g%s_l%s' % (alpha, epsilon, gamma, lamda),
                'rep%d',  # this number will be set when building the file list
                'test-vs-A3N_p%d.csv' % player
            )

            file_list = [path % rep for rep in range(params.initial_rep, params.final_rep+1)]  # +1 to include the final rep

            # filters out non-existent files
            non_existent = []
            for f in file_list:
                if not os.path.exists(f):
                    print('WARNING: %s does not exist' % f)  # this one always go to stdout
                    non_existent.append(f)

            # does a set operation to remove the non existent files and then converts back to list
            file_list = list(set(file_list) - set(non_existent))

            if len(file_list) > 0:  # if there are remaining files after filtering, analyse them
                outstream.write('%d,%s,%s,materialdistancehp,%s,%d,%s\n' % (
                    params.train_matches, mapname, lamda, strats.replace(',', ' '), player,
                    ','.join([str(x) for x in stats.average_score(file_list, player)])
                ))

        if params.output is not None:  # prevents closing sys.stdout
            outstream.close()
    

if __name__ == '__main__':
    parser = experiments.arg_parser('Analyse raw results of test matches')
    args = parser.parse_args()

    if not args.silent:
        commandlog.log_command(' '.join(sys.argv), 'analysis')
    
    raw_analysis(args)
    
    if args.output is not None:  # also calls a3n-vs-a1n-table.generate_table if -q was omitted
        print('Results are in .csv files starting with %s' % args.output)

