import os
import sys

from generate_experiments import arg_parser


def regen_missing(params):
    mode = 'w' if params['overwrite'] else 'a'
    outstream = open(params['output'], mode) if params['output'] is not None else sys.stdout

    for c in range(params['checkpoint'], params['train_matches'] + 1, params['checkpoint']):
        for rep in range(params['initial_rep'], params['final_rep'] + 1):
            generated = False  # used to avoid duplicate command generation

            for position in [0, 1]:

                # skips generating for position 1 if already generated for 0
                # (the data generation program takes care of filling data for both positions)
                if generated:
                    continue

                filename = os.path.join(
                    params['basedir'],
                    'rep%d' % rep,
                    'lcurve-vs-%s_p%d_m%d.csv' % (params['opponent'], position, c)
                )

                if not os.path.exists(filename):
                    outstream.write('./learningcurve.sh -d %s --test_matches %d --checkpoint %d -i %d -f %d\n' % (
                        params['basedir'], params['lcurve_matches'], c, rep, rep
                    ))
                    generated = True


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
