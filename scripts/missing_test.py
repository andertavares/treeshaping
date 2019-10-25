# lists all unfinished train experiments from a root dir
import os
import sys
import argparse
from itertools import product

def check_unfinished(params):
    """
    Traverses the specified basedir recursively. When a leaf dir is reached, checks if
    it contains the test-vs-[opponent]_p[position].csv file. If not, it is reported as unfinished 
    to the specified output
    :param params: a dict with the required arguments
    :return:
    """
    outstream = open(params['output'], 'w') if params['output'] is not None else sys.stdout

    # traverse root directory, and list directories as dirs and files as files
    for root, dirs, files in os.walk(params['basedir']):
        # if not dirs, then root is a leaf directory
        # checks if the file '.finished' is there
        if not dirs:
            # checks if the leaf dir is an experiment repetition and if .finished is missing
            if 'rep' in root:
                
                for opponent, position in product(params['opponent'], params['position']):
                    target_name = 'test-vs-%s_p%d.csv' % (opponent, position) 
                    if target_name not in files:
                        outstream.write('%s/%s\n' % (root, target_name))

    # closes the outstream (if not sys.stdout)
    if params['output'] is not None:
        outstream.close()


if __name__ == '__main__':

    parser = argparse.ArgumentParser(
        description='Lists all unfinished test experiments from a root dir'
    )

    parser.add_argument(
        'basedir', help='Root directory of results (will look recursively from there)',
    )

    parser.add_argument(
        '-o', '--output', help='Writes output to this file (if omitted, outputs to stdout)',
    )
    
    parser.add_argument(
        '-p', '--position', help='List of test positions (0, 1 or both)', type=int, nargs='+', default=[0, 1]
    )
    
    parser.add_argument(
        '-t', '--opponent', help='List of opponents', nargs='+', required=True
    )
    

    args = parser.parse_args()
    check_unfinished(vars(args))  # to access as dict
