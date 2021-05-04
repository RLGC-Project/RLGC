
from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import sys
import numpy as np
import tensorflow as tf
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from datetime import datetime
import time



import os
from stable_baselines.bench import Monitor
from stable_baselines.results_plotter import load_results, ts2xy
from stable_baselines import results_plotter
from stable_baselines.sac.policies import MlpPolicy
from stable_baselines.sac.policies import FeedForwardPolicy
from stable_baselines import SAC


np.random.seed(19)

# config the RLGC Java Sever
java_port = 25034
jar_file = '/lib/RLGCJavaServer1.0.0_rc.jar'



repo_path =  os.path.abspath('.')

import sys
sys.path.insert(0, repo_path+'/src/environments')


jar_path = repo_path + jar_file

case_files_array =list()

case_files_array.append(repo_path + '/testData/IEEE39/IEEE39bus_multiloads_xfmr4_smallX_v30.raw')
case_files_array.append(repo_path + '/testData/IEEE39/IEEE39bus_3AC.dyr')

dyn_config_file = repo_path + '/testData/IEEE39/json/IEEE39_dyn_config.json'
# use the configuration file for continuous action space
rl_config_file = repo_path + '/testData/IEEE39/json/IEEE39_RL_loadShedding_3motor_continuous.json'


# Create log dir
log_dir = "./logFiles"
os.makedirs(log_dir, exist_ok=True)


best_mean_reward, n_steps = -np.inf, 0

def callback(_locals, _globals):
    """
    Callback called at each step (for DQN an others) or after n steps (see ACER or PPO2)
    :param _locals: (dict)
    :param _globals: (dict)
    """
    global n_steps, best_mean_reward
    # Print stats every 1000 calls
    if (n_steps + 1) % 1000 == 0:
        # Evaluate policy training performance
        x, y = ts2xy(load_results(log_dir), 'timesteps')
        if len(x) > 0:
            mean_reward = np.mean(y[-100:])
            print(x[-1], 'timesteps')
            print("Best mean reward: {:.2f} - Last mean reward per episode: {:.2f}".format(best_mean_reward, mean_reward))

            # New best model, you could save the agent here
            if mean_reward > best_mean_reward:
                best_mean_reward = mean_reward
                # Example for saving best model
                print("Saving new best model")
                _locals['self'].save(log_dir + 'best_model.pkl')
    n_steps += 1


# Custom MLP policy of two layers of size 256 each
class CustomSACPolicy(FeedForwardPolicy):
    def __init__(self, *args, **kwargs):
        super(CustomSACPolicy, self).__init__(*args, **kwargs,
                                           layers=[256, 256],
                                           layer_norm=False,
                                           feature_extraction="mlp")



def train(learning_rate, time_steps, env, model_path):
    
    tf.reset_default_graph()    # to avoid the conflict the existnat parameters, but not suggested for reuse parameters


    # default policy is MlpPolicy
    model = SAC(CustomSACPolicy, env, verbose=1,seed=10, n_cpu_tf_sess=16)
    model.learn(total_timesteps=int(time_steps), log_interval=1000, callback=callback)
    model.save(model_path)

    #print("Saving final model to power_model_multistep_581_585_lr_%s.pkl" % (str(learning_rate)))
    #ddpg.save(savedModel + "/" + model_name + "_lr_%s_100w.pkl" % (str(learning_rate)))
#aa._act_params


#tf.reset_default_graph()    # to avoid the conflict the existnat parameters, but not suggested for reuse parameters
# step_rewards = list()
# step_actions = list()
# step_observations = list()
# step_status = list()
# step_starttime = list()
# step_durationtime = list()

check_pt_dir = "./TrainedModels"
if not os.path.exists(check_pt_dir):
    os.makedirs(check_pt_dir)

model_file = os.path.join(check_pt_dir, "sac_ieee39_loadshedding")


import time
start = time.time()


from PowerDynSimEnvDef_v7 import PowerDynSimEnv
env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,jar_path,java_port,verbose=0),


env = Monitor(env, log_dir, allow_early_resets=True)

time_steps = 500000
#for ll in [0.0001, 0.0005, 0.00005]:

for ll in [0.00005]:
    # step_rewards = list()
    # step_actions = list()
    # step_observations = list()
    # step_status = list()
    # step_starttime = list()
    # step_durationtime = list()

    env.reset()

    train(ll, time_steps, env, model_file)

    env.close_connection()

    # np.save(os.path.join(storedData, "step_rewards_lr_%s_" % str(ll) + dataname), np.array(step_rewards))
    # np.save(os.path.join(storedData, "step_actions_lr_%s_" % str(ll) + dataname), np.array(step_actions))
    # np.save(os.path.join(storedData, "step_observations_lr_%s_" % str(ll) + dataname), np.array(step_observations))
    # np.save(os.path.join(storedData, "step_status_lr_%s_" % str(ll) + dataname), np.array(step_status))
    # np.save(os.path.join(storedData, "step_starttime_lr_%s_" % str(ll) + dataname), np.array(step_starttime))
    # np.save(os.path.join(storedData, "step_durationtime_lr_%s_" % str(ll) + dataname), np.array(step_durationtime))


end = time.time()

print("total running time is %s" % (str(end - start)))


#np.save(os.path.join(storedData, "step_rewards_t"), np.array(step_rewards))
#np.save(os.path.join(storedData, "step_actions_t"), np.array(step_actions))
#np.save(os.path.join(storedData, "step_observations_t"), np.array(step_observations))
#np.save(os.path.join(storedData, "step_status_t"), np.array(step_status))
#np.save(os.path.join(storedData, "step_starttime_t"), np.array(step_starttime))
#np.save(os.path.join(storedData, "step_durationtime_t"), np.array(step_durationtime))

print("Finished!!")

results_plotter.plot_results([log_dir], time_steps, results_plotter.X_TIMESTEPS, "IEEE 39 Bus load shedding w/SAC")
plt.savefig(log_dir+'/IEEE_39Bus_loadshedding_SAC {}_{}.png'.format(str(time_steps),str(ll)))
plt.show()

def test_single_episode(model):
    act = SAC.load(model)
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
    print("Episode total reward", episode_rew)

    return episode_rew

def evaluate(model, num_steps=1000):
  """
  Evaluate a RL agent
  :param model: (BaseRLModel object) the RL Agent
  :param num_steps: (int) number of timesteps to evaluate it
  :return: (float) Mean reward for the last 100 episodes
  """
  episode_rewards = [0.0]
  obs = env.reset()
  for i in range(num_steps):
      # _states are only useful when using LSTM policies
      action, _states = model.predict(obs)

      obs, reward, done, info = env.step(action)
      
      # Stats
      episode_rewards[-1] += reward
      if done:
          obs = env.reset()
          episode_rewards.append(0.0)
  # Compute mean reward for the last 100 episodes
  mean_100ep_reward = round(np.mean(episode_rewards[-100:]), 1)
  print("Mean reward:", mean_100ep_reward, "Num episodes:", len(episode_rewards))
  
  return mean_100ep_reward