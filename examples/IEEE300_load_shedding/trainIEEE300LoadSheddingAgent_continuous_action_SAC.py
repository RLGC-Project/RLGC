
from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import sys
import numpy as np
import tensorflow as tf
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from gym import wrappers
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
java_port = 25035
jar_file = '/lib/RLGCJavaServer1.0.0_alpha.jar'


repo_path = os.path.abspath('.')


import sys
sys.path.insert(0, repo_path+'/src/environments')


jar_path = repo_path + jar_file

case_files_array =list()

case_files_array.append(repo_path + '/testData/IEEE300/IEEE300Bus_modified_noHVDC_v2.raw')
case_files_array.append(repo_path + '/testData/IEEE300/IEEE300_dyn_cmld_zone1.dyr')

dyn_config_file = repo_path + '/testData/IEEE300/json/IEEE300_dyn_config.json'
rl_config_file = repo_path + '/testData/IEEE300/json/IEEE300_RL_loadShedding_zone1_continuous.json'


# Create log dir
current_folder = os.path.dirname(__file__)


log_dir = current_folder+"/logFiles"
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
                _locals['self'].save(log_dir + '/best_model.pkl')
    n_steps += 1
    return True

# Custom MLP policy of two layers of size 256 each
class CustomSACPolicy(FeedForwardPolicy):
    def __init__(self, *args, **kwargs):
        super(CustomSACPolicy, self).__init__(*args, **kwargs,
                                           layers=[64, 64],
                                           layer_norm=False,
                                           feature_extraction="mlp")



def train(learning_rate, time_steps, env, model_path):
    
    tf.reset_default_graph()    # to avoid the conflict the existnat parameters, but not suggested for reuse parameters


    # default policy is MlpPolicy
    model = SAC(CustomSACPolicy, env, verbose=1,seed=10, n_cpu_tf_sess=16)
    model.learn(total_timesteps=int(time_steps), log_interval=1000, callback=callback)
    model.save("sac_ieee300_zone1_loadshedding")



import time
start = time.time()

from PowerDynSimEnvDef_v7 import PowerDynSimEnv
env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file,jar_path,java_port)


env = Monitor(env, log_dir, allow_early_resets=True)

#------------------------------------------
# total_steps:  this small number is for functional testing, you should set it to at least 500000 for training 
#------------------------------------------
time_steps = 1500
#for ll in [0.0001, 0.0005, 0.00005]:

for lr in [0.00005]:


    env.reset()

    model_path = None

    train(lr, time_steps, env, model_path)

    env.close_connection()


end = time.time()

print("total running time is %s" % (str(end - start)))


print("Finished!!")

results_plotter.plot_results([log_dir], time_steps, results_plotter.X_TIMESTEPS, "IEEE 39 Bus load shedding w/SAC")
plt.savefig(log_dir+'/IEEE_300Bus_loadshedding_Zone1_SAC_totalsteps_{}_lr_{}.png'.format(str(time_steps),str(lr)))
plt.show()
