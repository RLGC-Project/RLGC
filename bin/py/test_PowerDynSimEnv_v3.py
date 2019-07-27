from py4j.java_gateway import (JavaGateway, GatewayParameters)


import os
folder_dir = 'C:\\Users\huan289\\git\\deepgrid\\DeepGrid'
os.chdir(folder_dir)

print(os.getcwd())

java_port = 25334
# gateway = JavaGateway(
#     gateway_parameters=GatewayParameters(port = java_port, auto_convert=True)
#     )



# case_files = ['testData\\Kundur-2area\\kunder_2area_ver30.raw','testData\\Kundur-2area\\kunder_2area.dyr']
# Need to use the following way to define a String array in Python for Py4J
# case_files_array = gateway.new_array(gateway.jvm.String, 2)
case_files_array = []

case_files_array.append(folder_dir +'\\testData\\Kundur-2area\\kunder_2area_ver30.raw')
case_files_array.append(folder_dir+'\\testData\\Kundur-2area\\kunder_2area.dyr')

dyn_config_file = folder_dir+'\\testData\\Kundur-2area\\json\\kundur2area_dyn_config.json'

# rl_config_file = 'testData\\Kundur-2area\\json\\kundur2area_RL_config.json'
rl_config_file = folder_dir+'\\testData\\Kundur-2area\\json\\kundur2area_RL_config_multiStepObsv.json'



action_levels = [2]

from PowerDynSimEnvDef_v3 import PowerDynSimEnv
env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,java_port,action_levels)


for i in range(5):
    env.reset()
    results = env.step(1)
    print('states =',results[0])
    print('step reward =', results[1])
    

print('test completed')

             