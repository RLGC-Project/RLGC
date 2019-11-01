from py4j.java_gateway import (JavaGateway, GatewayParameters)


import os


java_port = 25333

a = os.path.abspath(os.path.dirname(__file__))

folder_dir = a[:-7]

case_files_array = []

case_files_array.append(folder_dir +'\\testData\\Kundur-2area\\kunder_2area_ver30.raw')
case_files_array.append(folder_dir+'\\testData\\Kundur-2area\\kunder_2area.dyr')

dyn_config_file = folder_dir+'\\testData\\Kundur-2area\\json\\kundur2area_dyn_config.json'

# rl_config_file = 'testData\\Kundur-2area\\json\\kundur2area_RL_config.json'
rl_config_file = folder_dir+'\\testData\\Kundur-2area\\json\\kundur2area_RL_config_multiStepObsv.json'




import os.path
import sys
# This is to fix the issue of "ModuleNotFoundError" below
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))  

from PowerDynSimEnvDef_v4 import PowerDynSimEnv
env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,java_port)


for i in range(5):
    results = env.step(1)
    print('states =',results[0])
    print('step reward =', results[1])
    

print('test completed')

env.close_connection()
print('connection with Ipss Server is closed')


''' The test should give the following output: 
------------------------------------------------------------------------------------------------
observation_history_length,observation_space_dim, action_location_num, action_level_num =
4 8 1 2
states = [1.         1.         1.         1.         0.55793684 0.91728261
 0.65307907 0.45873957 1.         1.         1.         1.
 0.55793684 0.91728261 0.65307907 0.45873957 1.         1.
 1.         1.         0.55793684 0.91728261 0.65307907 0.45873957
 1.         1.         1.         1.         0.55793684 0.91728261
 0.65307907 0.45873957]
step reward = -2.0000000000000107
states = [1.         1.         1.         1.         0.55793684 0.91728261
 0.65307907 0.45873957 1.         1.         1.         1.
 0.55793684 0.91728261 0.65307907 0.45873957 1.         1.
 1.         1.         0.55793684 0.91728261 0.65307907 0.45873957
 1.         1.         1.         1.         0.55793684 0.91728261
 0.65307907 0.45873957]
step reward = -2.0000000000000293
states = [1.         1.         1.         1.         0.55793684 0.91728261
 0.65307907 0.45873957 1.         1.         1.         1.
 0.55793684 0.91728261 0.65307907 0.45873957 1.         1.
 1.         1.         0.55793684 0.91728261 0.65307907 0.45873957
 1.         1.         1.         1.         0.55793684 0.91728261
 0.65307907 0.45873957]
step reward = -2.000000000000094
states = [1.         1.         1.         1.         0.55793684 0.91728261
 0.65307907 0.45873957 1.         1.         1.         1.
 0.55793684 0.91728261 0.65307907 0.45873957 1.         1.
 1.         1.         0.55793684 0.91728261 0.65307907 0.45873957
 1.         1.         1.         1.         0.55793684 0.91728261
 0.65307907 0.45873957]
step reward = -2.0000000000001723
states = [1.         1.         1.         1.         0.55793684 0.91728261
 0.65307907 0.45873957 1.         1.         1.         1.
 0.55793684 0.91728261 0.65307907 0.45873957 1.         1.
 1.         1.         0.55793684 0.91728261 0.65307907 0.45873957
 1.         1.         1.         1.         0.55793684 0.91728261
 0.65307907 0.45873957]
step reward = -2.000000000000246


'''

             