import os.path, time, sys
from py4j.java_gateway import (JavaGateway, GatewayParameters)
from subprocess import call, Popen, PIPE
java_port = 25336


# config the RLGC Java Sever
java_port = 25003
jar_file = '/lib/RLGCJavaServer1.0.0_alpha.jar'

repo_path =  os.path.abspath('.')

jar_path = repo_path + jar_file

import sys
sys.path.insert(0, repo_path +'/src/environments')


#case_files_array[0] = folder_dir + '/testData/IEEE39/IEEE39bus_multiloads_xfmr4_smallX_v30.raw'
#case_files_array[1] = folder_dir + '/testData/IEEE39/IEEE39bus_3AC.dyr'
dyn_config_file = repo_path + '\\testData\\IEEE39\\json\\IEEE39_dyn_config.json'

rl_config_file = repo_path + '\\testData\\IEEE39\\json\\IEEE39_RL_loadShedding_3motor_continuous.json'


from PowerDynSimEnvDef_v7 import PowerDynSimEnv
env = PowerDynSimEnv(None,dyn_config_file,rl_config_file,jar_path,java_port)



for i in range(15):
    results = env.step([-.5,-0.3,-0.1]) # no action is applied
    print('step reward =', results[1])

print('test completed')

env.close_connection()
print('connection with Ipss Server is closed')



