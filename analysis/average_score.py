import pandas as pd
import numpy as np
import argparse
import os


def score(filename, player_index):
    """
    Calculates the score (victories + 0.5*draws) from the results in a given .csv file
    Victories are matches with result 0, draws are matches with result -1 (losses have result 1)
    :param filename:
    :param player_index
    :return:
    """
    df = pd.read_csv(filename)
    # renames to access with df.result
    df.rename(columns={'#result': 'result'}, inplace=True)

    # score =  number of victories + 0.5 number of draws
    victories = df[df.result == player_index].count()['result']
    draws = df[df.result == -1].count()['result']
    return victories + 0.5 * draws


def average_score(basedir, initial_rep, final_rep, opponent, position):
    """
    Calculates the average score in a series of repeated test matches
    against an opponent in a given map
    :param basedir: base directory of the experiment (the one storing rep0,rep1,etc)
    :param initial_rep:
    :param final_rep:
    :param opponent:
    :param position: player position (0 or 1)
    :return:
    """
    filename = 'test-vs-%s_p%d.csv' % (opponent, position) if position is not None else 'test-vs-%s.csv' % opponent
    files = [os.path.join(basedir, 'rep%d' % rep, filename) for rep in range(initial_rep, final_rep + 1)]
    #print([score(f) for f in files])
    player_index = position if position is none else 0
    return np.mean([score(f, player_index) for f in files])


if __name__ == '__main__':

    parser = argparse.ArgumentParser(
        description='Calculates the average score in a series of repeated test matches '
                    'against an opponent in a given map.\n'
                    'In an experiment, the score is #victories + 0.5 * #draws. '
                    'The script looks in directories with the pattern:\n '
                    'basedir/selfplay-[map]/rep[%d]/test-vs-[opponent].csv, where %d is in [initial_rep, final_rep].\n'
                    'Usage example:\n'
                    'python3 analysis/average_score.py -b results -o LightRush -i 0 -f 9 -m basesWorkers8x8\n'
                    'For multiple opponents:\n'
                    'for o in {"HeavyRush","RangedRush","WorkerRush","LightRush"}; do echo $o; python3 analysis/average_score.py -b results -o $o -i 0 -f 9 -m basesWorkers8x8; done'
    )

    parser.add_argument(
        'basedir', help='base directory of the experiment (the one storing rep0,rep1,etc)'
    )

    parser.add_argument(
        '-i', '--initial-rep', type=int, required=True,
        help='Number of initial repetition.'
    )

    parser.add_argument(
        '-f', '--final-rep', type=int, required=True,
        help='Number of the final repetition.'
    )

    parser.add_argument(
        '-o', '--opponent', required=True,
        help='Opponent name.'
    )
    
    parser.add_argument(
        '-p', '--position', required=False, type=int, choices=[0,1],
        help='Learning agent position (0 or 1)'
    )

    args = parser.parse_args()

    print(average_score(args.basedir, args.initial_rep, args.final_rep, args.opponent, args.position))

