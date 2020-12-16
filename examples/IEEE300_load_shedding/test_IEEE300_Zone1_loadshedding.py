from py4j.java_gateway import (JavaGateway, GatewayParameters)
from subprocess import call, Popen, PIPE

import os, time


java_port = 25338

jar_file = '/lib/RLGCJavaServer1.0.0_rc.jar'



repo_path =  os.path.abspath('.')

import sys
sys.path.insert(0, repo_path+'/src/environments')


jar_path = repo_path + jar_file


case_files_array = []


case_files_array.append(repo_path + '/testData/IEEE300/IEEE300Bus_modified_noHVDC_v2.raw')

case_files_array.append(repo_path + '/testData/IEEE300/IEEE300_dyn_cmld_zone1.dyr')

dyn_config_file = repo_path + '/testData/IEEE300/json/IEEE300_dyn_config.json'

rl_config_file = repo_path + '/testData/IEEE300/json/IEEE300_RL_loadShedding_zone1_continuous_LSTM_new_morefaultbuses_moreActionBuses_testing.json'

import os.path
import sys
 

from PowerDynSimEnvDef_v7 import PowerDynSimEnv
env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,jar_path,java_port,repo_path)

print("all base cases:", env.get_base_cases())

import time

tic = time.perf_counter()

env.reset()
done = False
actions = [-0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, 
                    -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, 
                    -0.2, -0.2, -0.2, -0.2, -0.2, -0.2]
while not done:
    states, rewards, done,_ = env.step(actions) # no action is applied
    #print('states =',states)
    #print('step reward =', rewards)
    

print('test completed')
toc = time.perf_counter()

print(f"Used time in {toc - tic:0.4f} seconds")

env.close_connection()
print('connection with Ipss Server is closed')

             