import os
import sys

from generate_experiments import arg_parser


def regen_missing(params):
    mode = 'w' if params['overwrite'] else 'a'
    outstream = open(params['output'], mode) if params['output'] is not None else sys.stdout

    for c in range(params['checkpoint'], params['train_matches'] + 1, params['checkpoint']):
        for rep in range(params['initial_rep'], params['final_rep'] + 1):
            incomplete = False  # flag activated when learning curve data is incomplete

            for position in [0, 1]:

                filename = os.path.join(
                    params['basedir'],
                    'rep%d' % rep,
                    'lcurve-vs-%s_p%d_m%d.csv' % (params['opponent'], position, c)
                )

                # learning curve data is incomplete if the file does not exist or
                # if the number of matches is less than the half lcurve_matches (as half is done for each position)
                if not os.path.exists(filename):
                    incomplete = True
                else:  # file exists, count number of non-empty lines
                    with open(filename) as f:
                        non_blank_lines = sum(not line.isspace() for line in f) - 1  # -1 to discount the header

                    if params['lcurve_matches'] // 2 > non_blank_lines:
                        incomplete = True

                if incomplete:
                    outstream.write('./learningcurve.sh -d %s --test_matches %d --checkpoint %d -i %d -f %d\n' % (
                        params['basedir'], params['lcurve_matches'], c, rep, rep
                    ))
                    break  # gets out of the position-traversing loop to avoid generating the same command twice

    # closes the outstream (if not sys.stdout)
    if params['output'] is not None:
        outstream.close()

# TODO accept basedir as the experiment root!
if __name__ == '__main__':
    parser = arg_parser(
        description='Finds incomplete and/or missing learning curve data '
                    'and generates the commands to create them. The basedir '
                    'parameter is the one containing rep0, rep1, ...'
    )

    parser.add_argument(
        '--opponent', default='A3N',
        help='Opponent used to generate the learning curves.'
    )
    args = parser.parse_args()

    if args.opponent is None:
        print("Missing required parameter 'opponent'")

    regen_missing(vars(args))
