
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

from datetime import datetime
import time



from baselines import deepq
from baselines import logger
import baselines.common.tf_util as U

np.random.seed(19)

# config the RLGC Java Sever
java_port = 25002
jar_file = '/lib/RLGCJavaServer1.0.0_rc.jar'

repo_path =  os.path.abspath('.')

import sys
sys.path.insert(0, repo_path +'/src/environments')

jar_path = repo_path + jar_file

case_files_array =[]

case_files_array.append(repo_path +'/testData/Kundur-2area/kunder_2area_ver30.raw')
case_files_array.append(repo_path+'/testData/Kundur-2area/kunder_2area.dyr')

dyn_config_file = repo_path+'/testData/Kundur-2area/json/kundur2area_dyn_config.json'

rl_config_file = repo_path+'/testData/Kundur-2area/json/kundur2area_RL_config_multiStepObsv.json'


current_folder = os.path.dirname(__file__)

stored_data_dir = current_folder+"/storedData"

saved_model_dir = current_folder+"/trainedModels"

if not os.path.exists(stored_data_dir):
    os.makedirs(stored_data_dir)

if not os.path.exists(saved_model_dir):
    os.makedirs(saved_model_dir)



def callback(lcl, glb):
    # lcl: locals() returns local veraibales 
    # glb: globals() returns all global variables
    if lcl['t'] > 0:
        step_rewards.append(lcl['episode_rewards'])
        step_actions.append(lcl['action'])
        step_observations.append(lcl['obs'])




def train(total_steps, learning_rate, env, model_path, final_model, checkpoint_folder):
    
    tf.reset_default_graph()    # to avoid the conflict the existnat parameters, but not suggested for reuse parameters
   
    #model = deepq.models.mlp([256,256])

    act = deepq.learn(
        env,
        network = 'mlp',
        lr=learning_rate,
        total_timesteps=total_steps, 
        buffer_size=50000,
        checkpoint_freq = 1000,
         checkpoint_path=checkpoint_folder,
        learning_starts = 1000,
        exploration_fraction=0.1,
        exploration_final_eps=0.01,
        print_freq=10,
        callback=callback,
        load_path=model_path
    )
    print("Saving final model to: ", final_model)
    act.save(final_model)



#tf.reset_default_graph()    # to avoid the conflict the existnat parameters, but not suggested for reuse parameters
step_rewards = list()
step_actions = list()
step_observations = list()


import time
start = time.time()

from PowerDynSimEnvDef_v7 import PowerDynSimEnv
env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file, jar_path, java_port)

model_name = "Kundur_2area_dyn_breaking_DQN"

#---------------------!!!!!!! NOTE !!!!!!!!!---------------------
# total_steps:  this small number (1500) is for functional testing, you should set it to 1000000 for training 
#---------------------------------------------------------------
total_steps = 1500

#for lr in [0.0001, 0.0005, 0.00005]:
for lr in [0.00005]:

    env.reset()

    model_path =  None 

    final_saved_model = saved_model_dir + "/" + model_name + "_lr_%s_totsteps_%d.pkl" % (str(lr),total_steps)

    train(total_steps, lr, env, model_path, final_saved_model,saved_model_dir)

    env.close_connection()

    taining_model_name = model_name + "_lr_%s_totsteps_%d.pkl" % (str(lr),total_steps)

    np.save(os.path.join(stored_data_dir, "step_rewards_" + taining_model_name), np.array(step_rewards))
    np.save(os.path.join(stored_data_dir, "step_actions_" + taining_model_name), np.array(step_actions))
    np.save(os.path.join(stored_data_dir, "step_observations_" + taining_model_name), np.array(step_observations))


end = time.time()

print("total running time is %s" % (str(end - start)))


print("Finished!!")

def test():
    act = deepq.load("power_model.pkl")
    done = False


    #for i in range(1):
    obs, done = env._validate(1,8,1.0,0.585), False
    episode_rew = 0
    actions = list()
    while not done:
        
        action = act(obs[None])[0]
        #obs, rew, done, _ = env.step(act(obs[None])[0])
        obs, rew, done, _ = env.step(action)
        episode_rew += rew
    print("Episode reward", episode_rew)

    return actions
