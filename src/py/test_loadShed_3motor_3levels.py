from py4j.java_gateway import (JavaGateway, GatewayParameters)


import numpy as np

def manual_test():

    
    obs, done = env.validate(0,0,0.1,0.08), False
    episode_rew = 0
    actions = list()
    observations = list()
    cnt = 0
    while not done:
        action = [0,0,0]
        observations.append(obs)
        actions.append(action)
        obs, rew, done, _ = env.step(action)
        episode_rew += rew
        cnt += 1
    #print('obs=', obs)
    print("Episode reward", episode_rew)
    print('total cnt', cnt)

    return np.array(actions), np.array(observations)


import os, time


java_port = 25335

jar_file = "\lib\RLGCJavaServer0.83.jar"

a = os.path.abspath(os.path.dirname(__file__))

folder_dir = a[:-7]

jar_path = folder_dir + jar_file

case_files_array = []

###-------uncomment below four lines for linux-------------------------
# case_files_array[0] = folder_dir + '/testData/IEEE39/IEEE39bus_multiloads_xfmr4_smallX_v30.raw'
#
# case_files_array[1] = folder_dir + '/testData/IEEE39/IEEE39bus_3AC.dyr'
#
# dyn_config_file = folder_dir + '/testData/IEEE39/json/IEEE39_dyn_config.json'
#
# rl_config_file = folder_dir + '/testData/IEEE39/json/IEEE39_RL_loadShedding_3motor_continuous.json'

###-------uncomment below four lines for Windows -------------------------
case_files_array.append(folder_dir + '/testData/IEEE39/IEEE39bus_multiloads_xfmr4_smallX_v30.raw')

case_files_array.append(folder_dir + '/testData/IEEE39/IEEE39bus_3AC.dyr')

dyn_config_file = folder_dir + '/testData/IEEE39/json/IEEE39_dyn_config.json'

rl_config_file = folder_dir + '/testData/IEEE39/json/IEEE39_RL_loadShedding_3motor_continuous.json'

import os.path
import sys
# This is to fix the issue of "ModuleNotFoundError" below
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from PowerDynSimEnvDef_v5 import PowerDynSimEnv
env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,jar_path,java_port)


manual_test()
      
env.close_connection()



			 