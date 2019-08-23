#!/usr/bin/python3
import itertools

import argparse
import sys
import commandlog


def train_args(description='Generates commands to run experiments'):
    parser = argparse.ArgumentParser(
        description=description
    )

    parser.add_argument(
        'basedir', help='Base directory of results',
    )

    parser.add_argument(
        '-t', '--train-matches', required=True, type=int,
        help='Number of train matches'
    )

    '''parser.add_argument(
        '-r', '--repetitions', required=True, type=int,
        help='Number of repetitions'
    )'''

    parser.add_argument(
        '-i', '--initial-rep', required=True, type=int,
        help='Initial repetition'
    )

    parser.add_argument(
        '-f', '--final-rep', required=True, type=int,
        help='Final repetition'
    )

    parser.add_argument(
        '-m', '--maps', help='List of maps', nargs='+',
        default=['basesWorkers8x8', 'NoWhereToRun9x8', 'TwoBasesBarracks16x16']
    )

    parser.add_argument(
        '-l', '--lambdas', help='List of lambda values', nargs='+',
        #default=[0.0, 0.1, 0.3, 0.5, 0.7, 0.9]
        default=[0.3]
    )

    parser.add_argument(
        '-e', '--epsilons', help='List of exploration rates', nargs='+',
        default=[0.1]
        #default=[0.0, 0.05, 0.1, 0.15, 0.2, 0.3]
    )

    parser.add_argument(
        '-a', '--alphas', help='List of alphas to test', nargs='+',
        #default=[0.001, 0.01, 0.1, 0.3, 0.5]
        default=[0.01]
    )

    parser.add_argument(
        '-g', '--gammas', help='List of gammas to test', nargs='+',
        #default=[0.5, 0.7, 0.9, 0.99, 0.999, 1.0]
        default=[0.9]
    )

    parser.add_argument(
        '-d', '--decision-intervals', help='List of decision intervals', nargs='+',
        default=[10]
        # default=[0.0, 0.05, 0.1, 0.15, 0.2, 0.3]
    )

    parser.add_argument(
        '-s', '--strategies', help='List of sets of strategies (each set is a comma-separated string without spaces)',
        nargs='+',
        default=['CC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M']
    )

    parser.add_argument(
        '--train-opponents', help='Training opponents', nargs='+', default=['selfplay']
    )

    parser.add_argument(
        '--silent', help='Does not log command to the log file', action='store_true',
    )

    parser.add_argument(
        '-o', '--output', help='Appends the generated commands to this file (if omitted, outputs to stdout)',
    )

    parser.add_argument(
        '--overwrite', help='Overwrite rather than append the commands to the output file.',
        action='store_true'
    )

    return parser


def generate_commands(params, silent=False):
    if not silent:
        commandlog.log_command(' '.join(sys.argv), 'experiment')

    mode = 'w' if params['overwrite'] else 'a'
    outstream = open(params['output'], mode) if params['output'] is not None else sys.stdout

    params_list = [
        params[attr] for attr in ['maps', 'decision_intervals', 'alphas', 'gammas', 'lambdas', 'epsilons',
                                  'strategies', 'train_opponents']
    ]
    for mapname, interval, alpha, gamma, lamda, epsilon, strats, train_opp in itertools.product(*params_list):
        command = './train.sh -c config/%s.properties -d %s --train_matches %s --decision_interval %s ' \
                  '--train_opponent %s -s %s -e materialdistancehp -r winlossdraw ' \
                  '--td_alpha_initial %s --td_gamma %s --td_epsilon_initial %s --td_lambda %s ' \
                  '--checkpoint 10' % \
                  (mapname, params['basedir'], params['train_matches'], interval,
                   train_opp, strats, alpha, gamma, epsilon, lamda)

        for rep in range(params['initial_rep'], params['final_rep']+1):
            outstream.write('%s\n' % command)

    if params['output'] is not None:
        outstream.close()


if __name__ == '__main__':

    args = train_args().parse_args()
    
    generate_commands(vars(args), args.silent)

