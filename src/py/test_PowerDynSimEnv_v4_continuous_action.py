from py4j.java_gateway import (JavaGateway, GatewayParameters)
from subprocess import call, Popen, PIPE

import os, time


java_port = 25335

jarfile = r"C:\Users\huan289\git\RLGC\lib\RLGCJavaServer0.81.jar"
P = Popen(["java", "-jar", jarfile, str(java_port)],close_fds=True)
print ("Java server started with PID:",P.pid)
s = True
while s:
    try:
        gateway = JavaGateway(gateway_parameters=GatewayParameters(port = java_port, auto_convert=True))
        ipss_app = gateway.entry_point

        s = False
    except:
        time.sleep(0.1)

a = os.path.abspath(os.path.dirname(__file__))

folder_dir = a[:-7]

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
case_files_array.append(folder_dir + '\\testData\\IEEE39\\IEEE39bus_multiloads_xfmr4_smallX_v30.raw')

case_files_array.append(folder_dir + '\\testData\\IEEE39\\IEEE39bus_3AC.dyr')

dyn_config_file = folder_dir + '\\testData\\IEEE39\\json\\IEEE39_dyn_config.json'

rl_config_file = folder_dir + '\\testData\\IEEE39\\json\\IEEE39_RL_loadShedding_3motor_continuous.json'

import os.path
import sys
# This is to fix the issue of "ModuleNotFoundError" below
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))  

from PowerDynSimEnvDef_v4 import PowerDynSimEnv
env = PowerDynSimEnv(None,dyn_config_file,rl_config_file,java_port)


for i in range(15):
    results = env.step([-.5,-0.3,-0.1]) # no action is applied
    #print('states =',results[0])
    print('step reward =', results[1])
    

print('test completed')

env.close_connection()
print('connection with Ipss Server is closed')

P.terminate()
             