import logging
import math
import gym
from gym import spaces
from gym.utils import seeding
import numpy as np
from py4j.java_gateway import (JavaGateway, GatewayParameters)

import random



# logging
logger = logging.getLogger(__name__)

#
# function to transfer data from Java_Collections array to python Numpy array
#
def transfer1DJavaArray2NumpyArray(ary) :
    size = len(ary)
    np_ary = np.zeros(size)
    for i in range(size):
        np_ary[i] = ary[i]
    return np_ary

def transfer2DJavaArray2NumpyArray(ary) :
    size1 = len(ary)
    size2 = len(ary[0])
    np_ary = np.zeros((size1,size2))
    for i in range(size1):
        for j in range(size2):
            np_ary[i,j] = ary[i][j]
    return np_ary

# A power system dynamic simulation environment implementation by extending the Gym Env class defined in core.py, which is available in
# https://github.com/openai/gym/blob/master/gym/core.py

class PowerDynSimEnv(gym.Env):
    metadata = {

    }

    _case_files =""
    _dyn_sim_config_file =""
    _rl_config_file =""
    step_time = 0.1
    action_type = 'discrete'

    # define InterPSS dynamic simulation service
    #ipss_app = None


    def __init__(self,case_files, dyn_sim_config_file,rl_config_file , server_port_num = 25333):

        gateway = JavaGateway(gateway_parameters=GatewayParameters(port = server_port_num,auto_convert=True))
        global ipss_app
        ipss_app = gateway.entry_point

        from gym import spaces

        _case_files = case_files
        _dyn_sim_config_file = dyn_sim_config_file
        _rl_config_file = rl_config_file

        #initialize the power system simulation service
        dim_ary= ipss_app.initStudyCase(case_files,dyn_sim_config_file,rl_config_file)

        print(len(dim_ary))
        print (dim_ary[0], dim_ary[1],dim_ary[2])

        observation_history_length = dim_ary[0]
        observation_space_dim =  dim_ary[1]
        action_space_dim = dim_ary[2] + 1


        #define action and observation spaces
        self.action_space      = spaces.Discrete(action_space_dim) # Continuous
        self.observation_space = spaces.Box(-999,999,shape=(observation_history_length * observation_space_dim,)) # Continuous

        self._seed()

        #TOOD get the initial states
        self.state = None

        self.steps_beyond_done = None
        self.restart_simulation = True

    def _seed(self, seed=None):
        self.np_random, seed = seeding.np_random(seed)
        return [seed]

    def _step(self, action):

        assert self.action_space.contains(action), "%r (%s) invalid"%(action, type(action))

        #print(type(action))
        # need to convert action into one accepted by InterPSS

        #TODO need first convert action to String;
        actionStr = str(action);

        ipss_app.nextStepDynSim(self.step_time,actionStr, self.action_type)

        # retrieve the state from InterPSS simulation service

        # observations is a Java_Collections array
        observations = ipss_app.getEnvironmentObversations();

        # convert it from Java_collections array to native Python array
        self.state = transfer2DJavaArray2NumpyArray(observations)

        #check the states to see whether it go beyond the limits
        done = ipss_app.isSimulationDone();


        if not done:
            reward = ipss_app.getReward()

        elif self.steps_beyond_done is None:
            self.steps_beyond_done = 0
            reward = ipss_app.getReward() # even it is done, ipss_app would calculate and return a corresponding reward
        else:
            if self.steps_beyond_done == 0:
                logger.warning("You are calling 'step()' even though this environment has already returned done = True. You should always call 'reset()' once you receive 'done = True' -- any further steps are undefined behavior.")
            self.steps_beyond_done += 1

            reward = 0.0

        return np.array(self.state).ravel(), reward, done, {}

    def _reset(self):

        total_bus_num = ipss_app.getTotalBusNum()

        # reset need to randomize the operation state and fault location, and fault time


        case_Idx = np.random.randint(0,10) # an integer, in the range of [0, 9]
        #fault_bus_idx = np.random.randint(0, total_bus_num) # an integer, in the range of [0, total_bus_num-1]
        #fault_bus_idx = np.random.randint(6, 10) # an integer, in the range of [0, total_bus_num-1]
        fault_bus_idx =  2 # an integer, in the range of [0, total_bus_num-1]
        #fault_bus_idx = 8 # an integer, in the range of [0, total_bus_num-1]
        #fault_start_time =random.uniform(0.99, 1.01) # a double number, in the range of [0.2, 1]
        fault_start_time = 1.0 # a double number, in the range of [0.2, 1]
        #0.8 1.0
        fault_duation_time = random.uniform(0.498, 0.508)  # a double number, in the range of [0.08, 0.4]
        #fault_duation_time = 0.585  # a double number, in the range of [0.08, 0.4]
        # 0.4-0.588

        # self.np_random


        # reset initial state to states of time = 0, non-fault

        ipss_app.Reset(case_Idx,fault_bus_idx,fault_start_time,fault_duation_time)

        #self.state = None

        # observations is a Java_Collections array
        observations = ipss_app.getEnvironmentObversations();

        # convert it from Java_collections array to native Python array
        self.state = transfer2DJavaArray2NumpyArray(observations)

        self.steps_beyond_done = None
        self.restart_simulation = True

        return np.array(self.state).ravel(),fault_start_time,fault_duation_time

    # init the system with a specific state and fault
    def _validate(self, case_Idx, fault_bus_idx, fault_start_time, fault_duation_time):

        total_bus_num = ipss_app.getTotalBusNum()

        ipss_app.Reset(case_Idx,fault_bus_idx,fault_start_time,fault_duation_time)

        # observations is a Java_Collections array
        observations = ipss_app.getEnvironmentObversations();

        # convert it from Java_collections array to native Python array
        self.state = transfer2DJavaArray2NumpyArray(observations)

        self.steps_beyond_done = None
        self.restart_simulation = True

        return np.array(self.state).ravel()

    # def _render(self, mode='human', close=False):
