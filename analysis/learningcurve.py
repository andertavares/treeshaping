import numpy as np
import matplotlib.pyplot as plt
import argparse
import os
import stats


def file_sequence(basedir, checkpoint, train_matches, position, opponent='A3N'):
    """
    Returns a list of file names with the following pattern:
    [basedir]/rep%d/lcurve-vs-[opponent]_p[position]_m[M].csv, where
    basedir, opponent, position are given and M is a number ranging from the
    first to the last checkpoint. Note that the repetition number (rep%d) is not
    resolved here.

    :param basedir: directory where experiment repetitions are stored
    :param checkpoint: learning curve matches were generated every 'checkpoint' matches (e.g. every 10 matches)
    :param train_matches: number of train matches
    :param position: will plot learning curves for player 0 or 1?
    :param opponent: will plot learning curves generated against which opponent?
    :return:
    """

    # generates a list of numbers from the first to the last checkpoint
    checkpoints = list(range(checkpoint, train_matches + 1, checkpoint))

    return [os.path.join(basedir, 'rep%d', 'lcurve-vs-%s_p%d_m%d.csv' % (opponent, position, point)) for point in checkpoints]


def reps_ok(path_template, args):
    """
    Returns true if all files exist in all repetitions for the given file template
    :param path_template:
    :return: a path with all but rep%d filled (I'll test if the files exist for all rep numbers)
    """
    all_ok = True
    for rep in range(args.initial_rep, args.final_rep + 1):
        if not os.path.exists(path_template % rep):
            print('File not found: "%s"' % path_template % rep)
            all_ok = False

    return all_ok


def plot_learning_curve(args):
    """
    Plots the learning curve for the given parameters
    :param args: a namespace, as generated by argparse (must contain basedir, checkpoint, train_matches, etc...)
    :return:
    """

    # generator of repetition numbers
    repetitions = range(args.initial_rep, args.final_rep + 1)

    # metrics found in the learning curve .csv files (in this order)
    metrics = ['wins', 'draws', 'losses', 'matches', 'score', 'percent_score']

    plt.title('Learning curves')

    for position in args.positions:
        # sequence of files: one for each checkpoint, but without resolving the repetition number
        file_seq = file_sequence(args.basedir, args.checkpoint, args.train_matches, position, args.opponent)

        # points has 6 cols: wins, draws, losses, matches, score, percent_score
        # each column has the average metric through the experiment repetitions (NAN if a repetition misses a file)
        points = np.array([
            stats.average_score([f % rep for rep in repetitions], position) if reps_ok(f, args) else np.full(6, np.nan) for f in file_seq
        ])

        # x axis has the checkpoints, y axis the average on the desired metric
        line, = plt.plot(range(args.checkpoint, args.train_matches + 1, args.checkpoint), points[:, metrics.index(args.metric)])
        line.set_label('Player %d' % position)

    plt.xlabel("checkpoint")
    plt.ylabel(args.metric)
    plt.legend()

    if args.output:
        plt.savefig(args.output)
    else:
        plt.show()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Plots learning curves for the desired experiments'
    )

    parser.add_argument(
        'basedir', help='Base directory of results',
    )

    parser.add_argument(
        '-t', '--train-matches', required=True, type=int,
        help='Number of train matches'
    )

    parser.add_argument(
        '-c', '--checkpoint', type=int, default=10,
        help='Weights were evaluated every "checkpoint" matches'
    )

    parser.add_argument(
        '-i', '--initial-rep', required=True, type=int,
        help='Initial repetition'
    )

    parser.add_argument(
        '-f', '--final-rep', required=True, type=int,
        help='Final repetition'
    )

    parser.add_argument(
        '-m', '--metric', default='percent_score',
        choices=['wins', 'draws', 'losses', 'score', 'percent_score'],
        help='Metric to plot'
    )

    parser.add_argument(
        '-p', '--positions', default=[0, 1], type=int, nargs='+',
        help='Plot learning curve of which player? (default=both)'
    )

    parser.add_argument(
        '--opponent', default='A3N',
        help='Opponent used to generate the learning curves.'
    )

    parser.add_argument(
        '-o', '--output',
        help='File to save the generated image (plots to screen if omitted)'
    )

    plot_learning_curve(parser.parse_args())

