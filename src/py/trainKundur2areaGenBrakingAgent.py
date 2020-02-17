
from py4j.java_gateway import JavaGateway
from py4j.java_gateway import GatewayParameters

import os
import os.path
import gym
import sys
import numpy as np
import tensorflow as tf
import matplotlib.pyplot as plt
from gym import wrappers
from datetime import datetime
import time

from PowerDynSimEnvDef_v5 import PowerDynSimEnv


from baselines import deepq
from baselines.common import models
from baselines import logger
import baselines.common.tf_util as U

np.random.seed(19)

# config the RLGC Java Sever
java_port = 25003
jar_file = '/lib/RLGCJavaServer0.87.jar'

# This is to fix the issue of "ModuleNotFoundError" below
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))  


a = os.path.abspath(os.path.dirname(__file__))
folder_dir = a[:-7]

jar_path = folder_dir + jar_file

case_files_array = []

case_files_array.append(folder_dir +'/testData/Kundur-2area/kunder_2area_ver30.raw')
case_files_array.append(folder_dir+'/testData/Kundur-2area/kunder_2area.dyr')

dyn_config_file = folder_dir+'/testData/Kundur-2area/json/kundur2area_dyn_config.json'

rl_config_file = folder_dir+'/testData/Kundur-2area/json/kundur2area_RL_config_multiStepObsv.json'


env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,jar_path,java_port)




storedData = "./storedData"


savedModel= "./previous_model"
model_name = "kundur2area_multistep_581to585_bus2_90w"

def callback(lcl, glb):
    # stop training if reward exceeds -30
    is_solved = lcl['t'] > 100 and sum(lcl['episode_rewards'][-101:-1]) / 100 >= -30.0
    return is_solved
#    episodes = 0
#    if lcl['t'] > 0:
#        step_rewards.append(lcl['rew'])
#        step_actions.append(lcl['action'])
#        step_observations.append(lcl['obs'])
#        step_status.append(lcl['done'])
#        step_starttime.append(lcl['starttime'])
#        step_durationtime.append(lcl['durationtime'])
#        if lcl['t'] % 499 == 0:
#            U.save_state(model_file)



def main(learning_rate):
   
    tf.reset_default_graph()    # to avoid the conflict with the existing parameters, but this is not suggested for reuse parameters
    graph = tf.get_default_graph()
    #print(graph.get_operations())
    env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,jar_path, java_port)


    act = deepq.learn(
        env,
        network=models.mlp(num_layers=2, num_hidden=128,activation=tf.nn.relu),
        lr=learning_rate,
        total_timesteps=900,  ## this small number is for testing/demo only, please increase this number to 900000 for real training.
        buffer_size=50000,
        checkpoint_freq = 1000,
        exploration_fraction=0.1,
        exploration_final_eps=0.02,
        print_freq=10,
        callback=callback
    )
    print("Saving final model to: "+savedModel + "/" + model_name + "_lr_%s_90w.pkl" % (str(learning_rate)))
    act.save(savedModel + "/" + model_name + "_lr_%s_90w.pkl" % (str(learning_rate)))

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


print("Finished!!")

def test():
    act = deepq.load("power_model.pkl")
    done = False

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

