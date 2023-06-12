import sys

name = sys.argv[1]
with open(name) as f:
    lines = [ line.strip() for line in f ]

lines.extend(['00000000'] * (8192 - len(lines)))
with open(sys.argv[2], 'w') as f:
    f.write('\n'.join(lines) + '\n')

