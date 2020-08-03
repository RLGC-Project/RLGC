
import os, time


java_port = 25337

jar_file = '/lib/RLGCJavaServer1.0.0_alpha.jar'

repo_path =  os.path.abspath('.')

jar_path = repo_path + jar_file

import sys
sys.path.insert(0, repo_path +'/src/environments')


jar_path = repo_path + jar_file

case_files_array = []


case_files_array.append(repo_path + '/testData/IEEE39/IEEE39bus_multiloads_xfmr4_smallX_v30.raw')

case_files_array.append(repo_path + '/testData/IEEE39/IEEE39bus_3AC.dyr')

dyn_config_file = repo_path + '/testData/IEEE39/json/IEEE39_dyn_config.json'

rl_config_file = repo_path + '/testData/IEEE39/json/IEEE39_RL_loadShedding_3motor_continuous.json'

import os.path
import sys
# This is to fix the issue of "ModuleNotFoundError" below
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))  

from PowerDynSimEnvDef_v7 import PowerDynSimEnv
env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,jar_path,java_port,repo_path)

print("all base cases:", env.get_base_cases())

assert len(env.get_base_cases())==13

print("observation names:",env.get_observation_names())

assert len(env.get_observation_names())==11

print("all generator active power:", env.get_all_generator_activePower())

assert len(env.get_all_generator_activePower())==10

print("all load active power:", env.get_all_load_activePower())

print("active power of all loads within action scope:", env.get_load_activePower_within_action_scope())

print("ids of all loads within action scope:", env.get_load_id_within_action_scope())

assert len(env.get_load_id_within_action_scope())==9

print("adjacency matrix:",env.get_adjacency_matrix())

# to show how to change branch status, this can be used to change topoloty  or introduce branch outages during training
env.set_branch_status(1,2,'1',0)

env.reset()
for i in range(15):
    results = env.step([-.5,-0.3,-0.1]) # no action is applied
    print('states =',results[0])
    print('step reward =', results[1])
    

print('test completed')

env.close_connection()
print('connection with Ipss Server is closed')

             