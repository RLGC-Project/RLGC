from py4j.java_gateway import (JavaGateway, GatewayParameters)


import os
import os.path
import sys


def manual_test():

    
    obs, done = env.validate(1,24,0.05,0.08), False
    episode_rew = 0
    actions = list()
    observations = list()
    cnt = 0
    while not done:
  
        #action = act(obs[None])[0]
        #action_lst = [3,3,0,0]
        #action = refer_back(action_lst, cnts)
        
        action = 0
        
        #if cnt <= 10:
        #    action_lst = [2,2,2]
        #    action = referback(action_lst, cnts)            
        
        #if 1 <= cnt <= 3: #or cnt == 12: # or cnt == 13:
        #    action_lst = [1,1,1]
        #    action = referback(action_lst, cnts)        
        #if cnt == 4: #or cnt == 15:
        #    action_lst = [0,1,1]
        #    action = referback(action_lst, cnts)
        #if cnt >= 16:# or cnt == 16:
        #    action_lst = [2,2,2]
        #    action = referback(action_lst, cnts)
        
        #print(action)
        observations.append(obs)
        actions.append(action)
        obs, rew, done, _ = env.step(action)
        episode_rew += rew
        cnt += 1
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




# case_files = ['testData\\Kundur-2area\\kunder_2area_ver30.raw','testData\\Kundur-2area\\kunder_2area.dyr']
# Need to use the following way to define a String array in Python for Py4J

                
case_files_array = gateway.new_array(gateway.jvm.String, 2)
case_files_array[0] = folder_dir+'\\'+'testData\\IEEE39\\IEEE39bus_multiloads_xfmr4_smallX_v30.raw'
case_files_array[1] = folder_dir+'\\'+'testData\\IEEE39\\IEEE39bus_3AC.dyr'

dyn_config_file = folder_dir+'\\'+'testData\\IEEE39\\json\\IEEE39_dyn_config_v0.65.json'
rl_config_file = folder_dir+'\\'+'testData\\IEEE39\\json\\IEEE39_RL_loadShedding_3motor_3levels_v0.65.json'

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
from PowerDynSimEnvDef_v3 import PowerDynSimEnv
actions = [3, 3, 3]
env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,cnts=actions)

import numpy as np

manual_test()
      
env.close_connection()

print('test completed')

			 