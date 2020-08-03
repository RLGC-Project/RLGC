import os
a = os.path.abspath(os.path.dirname(__file__))
print(a[:-7])
import sys
sys.path.insert(0, './src/environments')

import PowerDynSimEnvDef_v7

