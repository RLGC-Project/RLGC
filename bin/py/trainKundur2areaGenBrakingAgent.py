
from py4j.java_gateway import JavaGateway
from py4j.java_gateway import GatewayParameters

import os
import gym
import sys
import numpy as np
import tensorflow as tf
import matplotlib.pyplot as plt
from gym import wrappers
from datetime import datetime
import time
from q_learning_bins import plot_running_avg
#from PowerDynSimEnvDefX import PowerDynSimEnv
from PowerDynSimEnvDef2 import PowerDynSimEnv


from baselines import deepq
from baselines import logger
import baselines.common.tf_util as U

np.random.seed(19)

# create
java_port = 25002
#gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
gateway = JavaGateway(gateway_parameters=GatewayParameters(port = java_port, auto_convert=True))
ipss_app = gateway.entry_point


case_files_array = gateway.new_array(gateway.jvm.String, 2)
case_files_array[0] = 'testData/Kundur-2area/kunder_2area_ver30.raw'
#case_files_array[1] = 'testData/Kundur-2area/kunder_2area_full_tgov1.dyr'
case_files_array[1] = 'testData/Kundur-2area/kunder_2area.dyr'

dyn_config_file = 'testData/Kundur-2area/json/kundur2area_dyn_config.json'

#rl_config_file = 'testData/Kundur-2area/json/kundur2area_RL_config.json'
rl_config_file = '/home/haow889/launchJavaServer/javaServers_2/testData/Kundur-2area/json/kundur2area_RL_config_multiStepObsv.json'

ob_act_dim_ary = ipss_app.initStudyCase(case_files_array , dyn_config_file, rl_config_file)

#env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file)



#storedData = "./storedData583"
#storedData = "./storedData584"
storedData = "./storedData581to585"
#storedData = "./storedData498to508Noise"
#storedData = "./storedData400to766"
#storedData = "./storedData585"

savedModel= "./previous_model"
model_name = "power_model_multistep_" + storedData[-8:]

def callback(lcl, glb):
    # stop training if reward exceeds -30
    #is_solved = lcl['t'] > 100 and sum(lcl['episode_rewards'][-101:-1]) / 100 >= -30.0
    #return is_solved
    episodes = 0
    if lcl['t'] > 0:
        step_rewards.append(lcl['rew'])
        step_actions.append(lcl['action'])
        step_observations.append(lcl['obs'])
        step_status.append(lcl['done'])
        step_starttime.append(lcl['starttime'])
        step_durationtime.append(lcl['durationtime'])
        if lcl['t'] % 499 == 0:
            U.save_state(model_file)



def main(learning_rate):
   
    tf.reset_default_graph()    # to avoid the conflict with the existing parameters, but this is not suggested for reuse parameters
    env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file, java_port)
    model = deepq.models.mlp([128,128])

    act = deepq.learn(
        env,
        q_func=model,
        lr=learning_rate,
        max_timesteps=900000,
        buffer_size=50000,
        checkpoint_freq = 1000,
        exploration_fraction=0.1,
        exploration_final_eps=0.02,
        print_freq=10,
        callback=callback
    )
    print("Saving final model to power_model_multistep498_508_lr_%s_90w.pkl" % (str(learning_rate)))
    #act.save(savedModel + "/" + model_name + "_lr_%s_90w.pkl" % (str(learning_rate)))

#aa._act_params



#tf.reset_default_graph()    # to avoid the conflict the existnat parameters, but not suggested for reuse parameters
step_rewards = list()
step_actions = list()
step_observations = list()
step_status = list()
step_starttime = list()
step_durationtime = list()

check_pt_dir = "./PowerGridModels"
if not os.path.exists(check_pt_dir):
    os.makedirs(check_pt_dir)

model_file = os.path.join(check_pt_dir, "gridmodel")


import time
start = time.time()
dataname = "multistep_581to585_bus2_90w"
for ll in [0.0001]:
    step_rewards = list()
    step_actions = list()
    step_observations = list()
    step_status = list()
    step_starttime = list()
    step_durationtime = list()

    main(ll)

    np.save(os.path.join(storedData, "step_rewards_lr_%s_" % str(ll) + dataname), np.array(step_rewards))
    np.save(os.path.join(storedData, "step_actions_lr_%s_" % str(ll) + dataname), np.array(step_actions))
    np.save(os.path.join(storedData, "step_observations_lr_%s_" % str(ll) + dataname), np.array(step_observations))
    np.save(os.path.join(storedData, "step_status_lr_%s_" % str(ll) + dataname), np.array(step_status))
    np.save(os.path.join(storedData, "step_starttime_lr_%s_" % str(ll) + dataname), np.array(step_starttime))
    np.save(os.path.join(storedData, "step_durationtime_lr_%s_" % str(ll) + dataname), np.array(step_durationtime))
end = time.time()

print("total running time is %s" % (str(end - start)))


#np.save(os.path.join(storedData, "step_rewards_t"), np.array(step_rewards))
#np.save(os.path.join(storedData, "step_actions_t"), np.array(step_actions))
#np.save(os.path.join(storedData, "step_observations_t"), np.array(step_observations))
#np.save(os.path.join(storedData, "step_status_t"), np.array(step_status))
#np.save(os.path.join(storedData, "step_starttime_t"), np.array(step_starttime))
#np.save(os.path.join(storedData, "step_durationtime_t"), np.array(step_durationtime))

print("Finished!!")

def test():
    act = deepq.load("power_model.pkl")
    done = False


    #for i in range(1):
    obs, done = env._validate(1,8,1.0,0.585), False
    episode_rew = 0
    actions = list()
    while not done:
        #env.render()
        action = act(obs[None])[0]
        #obs, rew, done, _ = env.step(act(obs[None])[0])
        obs, rew, done, _ = env.step(action)
        episode_rew += rew
    print("Episode reward", episode_rew)

    return actions

