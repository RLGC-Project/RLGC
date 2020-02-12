
from py4j.java_gateway import JavaGateway
from py4j.java_gateway import GatewayParameters

import os
import gym
import sys
import numpy as np
import tensorflow as tf
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from gym import wrappers
from datetime import datetime
import time
from PowerDynSimEnvDef_v5 import PowerDynSimEnv


from baselines import deepq
from baselines import logger
import baselines.common.tf_util as U

np.random.seed(19)

# config the RLGC Java Sever
java_port = 25002
jar_file = '/lib/RLGCJavaServer0.86.jar'

a = os.path.abspath(os.path.dirname(__file__))
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

repo_path = a[:-7]
jar_path = repo_path + jar_file

case_files_array =[]

case_files_array.append(repo_path + '/testData/IEEE39/IEEE39bus_multiloads_xfmr4_smallX_v30.raw')
case_files_array.append(repo_path + '/testData/IEEE39/IEEE39bus_3AC.dyr')

# configuration files for dynamic simulation and RL
dyn_config_file = repo_path + '/testData/IEEE39/json/IEEE39_dyn_config.json'
rl_config_file = repo_path + '/testData/IEEE39/json/IEEE39_RL_loadShedding_3motor_2levels.json'


storedData = "./storedData"

savedModel= "./trainedModels"
model_name = "IEEE39_multistep_obs11_randftd3_randbus3_3motor2action_prenull"

def callback(lcl, glb):

    if lcl['t'] > 0:
        step_rewards.append(lcl['episode_rewards'])
     #  step_actions.append(lcl['action'])
     #   step_observations.append(lcl['obs'])
     #   step_status.append(lcl['done'])
     #   step_starttime.append(lcl['starttime'])
     #   step_durationtime.append(lcl['durationtime'])
        if lcl['t'] % 499 == 0:
            U.save_state(model_file)



def train(learning_rate, env, model_path):
    
    tf.reset_default_graph()    # to avoid the conflict the existnat parameters, but not suggested for reuse parameters
   
    #model = deepq.models.mlp([256,256])

    act = deepq.learn(
        env,
        network = 'mlp',
        lr=learning_rate,
        total_timesteps=20,
        buffer_size=50000,
        checkpoint_freq = 1000,
        learning_starts = 1000,
        exploration_fraction=0.1,
        exploration_final_eps=0.01,
        print_freq=10,
        callback=callback,
        load_path=model_path
    )
    print("Saving final model to: ",savedModel + "/" + model_name + "_lr_%s_100w.pkl" % (str(learning_rate)))
    act.save(savedModel + "/" + model_name + "_lr_%s_100w.pkl" % (str(learning_rate)))
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
dataname = "multistep_obs11_randftd3_randbus3_3motor2action_prenull_100w"

env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file, jar_path, java_port)

#for ll in [0.0001, 0.0005, 0.00005]:
for ll in [0.00005]:
    step_rewards = list()
    step_actions = list()
    step_observations = list()
    step_status = list()
    step_starttime = list()
    step_durationtime = list()

    env.reset()

    #model_path = "./previous_model/IEEE39_multistep_p150_3motor3action_prenull_008_lr_0.0001_30w.pkl"
    model_path = None

    train(ll, env, model_path)

    env.close_connection()

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
