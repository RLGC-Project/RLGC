from py4j.java_gateway import (JavaGateway, GatewayParameters)


import os
import os.path
import sys
import numpy as np

def manual_test():

    
    obs, done = env.validate(1,24,0.1,0.08), False
    episode_rew = 0
    actions = list()
    observations = list()
    cnt = 0
    while not done:
        
        action = 0
        observations.append(obs)
        actions.append(action)
        obs, rew, done, _ = env.step(action)
        episode_rew += rew
        cnt += 1
    #print('obs=', obs)
    print("Episode reward", episode_rew)
    print('total cnt', cnt)

    return np.array(actions), np.array(observations)


folder_dir = r'C:\Users\huan289\git\RLGC'
#os.chdir(folder_dir)

print(os.getcwd())

java_port = 25333
gateway = JavaGateway(
    gateway_parameters=GatewayParameters(port = java_port, auto_convert=True)
   )

ipss_app = gateway.entry_point
     
case_files_array = gateway.new_array(gateway.jvm.String, 2)
case_files_array[0] = folder_dir+'\\'+'testData\\IEEE39\\IEEE39bus_multiloads_xfmr4_smallX_v30.raw'
case_files_array[1] = folder_dir+'\\'+'testData\\IEEE39\\IEEE39bus_3AC.dyr'

dyn_config_file = folder_dir+'\\'+'testData\\IEEE39\\json\\IEEE39_dyn_config_v0.65.json'
rl_config_file = folder_dir+'\\'+'testData\\IEEE39\\json\\IEEE39_RL_loadShedding_3motor_2levels.json'

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
from PowerDynSimEnvDef_v3 import PowerDynSimEnv

actions = [3, 3, 3]
env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,cnts=actions)

manual_test()
      
env.close_connection()



			 