from py4j.java_gateway import (JavaGateway, GatewayParameters)
from subprocess import call, Popen, PIPE

import os, time


java_port = 25338

jar_file = "\lib\RLGCJavaServer0.91.jar"

a = os.path.abspath(os.path.dirname(__file__))

folder_dir = a[:-7]

jar_path = folder_dir + jar_file

case_files_array = []


case_files_array.append(folder_dir + '/testData/IEEE300/IEEE300Bus_modified_noHVDC_v2.raw')

case_files_array.append(folder_dir + '/testData/IEEE300/IEEE300_dyn_cmld_zone1.dyr')

dyn_config_file = folder_dir + '/testData/IEEE300/json/IEEE300_dyn_config.json'

rl_config_file = folder_dir + '/testData/IEEE300/json/IEEE300_RL_loadShedding_zone1_continuous_LSTM_new_morefaultbuses_moreActionBuses_testing.json'

import os.path
import sys
# This is to fix the issue of "ModuleNotFoundError" below
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))  

from PowerDynSimEnvDef_v7 import PowerDynSimEnv
env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,jar_path,java_port,folder_dir)

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

             