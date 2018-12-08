from py4j.java_gateway import (
    JavaGateway, CallbackServerParameters, GatewayParameters,
    launch_gateway)


import os
folder_dir = 'C:\\Users\huan289\\git\\deepgrid\\DeepGrid'
os.chdir(folder_dir)

print(os.getcwd())

#-----------------------------------------------------------------------------------
## first training environment, connected to a server at port = 25331
#-----------------------------------------------------------------------------------

java_port = 25331

gateway = JavaGateway(gateway_parameters=GatewayParameters(port = java_port,auto_convert=True))



dyn_config_file = 'C:\\Users\\huan289\\git\\deepgrid\\DeepGrid\\testData\\Kundur-2area\\json\\kundur2area_dyn_config.json'

rl_config_file = 'C:\\Users\\huan289\\git\\deepgrid\\DeepGrid\\testData\\Kundur-2area\\json\\kundur2area_RL_config.json'


case_files_array = gateway.new_array(gateway.jvm.String, 2)
case_files_array[0] = 'C:\\Users\\huan289\\git\\deepgrid\\DeepGrid\\testData\\Kundur-2area\\kunder_2area_ver30.raw'
case_files_array[1] = 'C:\\Users\\huan289\\git\\deepgrid\\DeepGrid\\testData\\Kundur-2area\\kunder_2area.dyr'


from PowerDynSimEnvDef import PowerDynSimEnv
env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,java_port)
env.reset()

#-----------------------------------------------------------------------------------
## second training environment, connected to a server at port = 25333
#-----------------------------------------------------------------------------------

java_port2 = 25333

gateway2 = JavaGateway(gateway_parameters=GatewayParameters(port = java_port2,auto_convert=True))

case_files_array = gateway2.new_array(gateway.jvm.String, 2)
case_files_array[0] = 'C:\\Users\\huan289\\git\\deepgrid\\DeepGrid\\testData\\Kundur-2area\\kunder_2area_ver30.raw'
case_files_array[1] = 'C:\\Users\\huan289\\git\\deepgrid\\DeepGrid\\testData\\Kundur-2area\\kunder_2area.dyr'

print(case_files_array,dyn_config_file,rl_config_file)

env2 = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,java_port2)
env2.reset()

print ('start the for loop')
for i in range(10):
    env.reset()
    env2.reset()
print('two tests completed')

			 