import logging, time
import math
import gym
from gym import spaces
from gym.utils import seeding
import numpy as np
from py4j.java_gateway import (JavaGateway, GatewayParameters)
from subprocess import call, Popen, PIPE

import random
from py4j.tests.java_gateway_test import gateway



#CNTS = [2,2,2]

# logging
logger = logging.getLogger(__name__)

def scale_action(action_space, action):
    """
    Rescale the action from [low, high] to [-1, 1]
    (no need for symmetric action space)
    :param action_space: (gym.spaces.box.Box)
    :param action: (np.ndarray)
    :return: (np.ndarray)
    """
    low, high = action_space.low, action_space.high
    return 2.0 * ((action - low) / (high - low)) - 1.0

def unscale_action(action_space, scaled_action):
    """
    Rescale the action from [-1, 1] to [low, high]
    (no need for symmetric action space)
    :param action_space: (gym.spaces.box.Box)
    :param action: (np.ndarray)
    :return: (np.ndarray)
    """
    low, high = action_space.low, action_space.high
    return low + (0.5 * (scaled_action + 1.0) * (high - low))

def refer(val):
    temp = oct(val)[2:].zfill(4)
    result = [0] * 4
    for i, c in enumerate(temp):
        result[i] = int(c)
    return result


def refer_new(val, cnts):
    l = len(cnts)
    idx = l - 1

    result = [0] * l
    while val > 0:
        result[idx] = val % cnts[idx]

        val //= cnts[idx]
        idx -=1
    return result


def referback(actions, cnts):
    result = 0
    #actions = list(map(int, "".join(list(map(str, actions))).lstrip('0')))
    p = 0
    for i in range(len(actions))[::-1]:
        result += actions[i]* (cnts[i] ** p)
        p += 1

    return result







def transfer2JavaDblAry(gateway, pyArray, size):
    dblAry = gateway.new_array(gateway.jvm.double, size)
    i = 0
    for x in pyArray:
        dblAry[i] = float(x)
        i = i + 1
    return dblAry

def transfer2JavaStringAry(gateway, pyArray):
    
    strAry = gateway.new_array(gateway.jvm.String, len(pyArray))
    i = 0
    for x in pyArray:
        strAry[i] = str(x)
        i = i + 1
    return  strAry

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
    default_time_step = 0.1
    action_type = 'discrete'


    a_gateway = None
    # define InterPSS dynamic simulation service
    ipss_app = None

    server_process = None

    # to save the original action low and high values to help rescale them back before applying to the environment
    original_action_space = None
    is_action_space_scaled =False


    def __init__(self,case_files, dyn_sim_config_file,rl_config_file , jar_file, server_port_num = 25333,
                 force_symmetric_continuous_action=False, verbose=0):
        
        # change from global to class-level variable to support parallel process

        self.server_process = Popen(["java", "-jar", jar_file, str(server_port_num), str(verbose)], close_fds=True)
        print("IPSS-RL Java server lib path:", jar_file)
        print("Java server started with PID:", self.server_process.pid)
        time.sleep(5.0)

        s = True
        while s:
            try:
                # global gateway
                self.a_gateway = JavaGateway(
                    gateway_parameters=GatewayParameters(port=server_port_num, auto_convert=True))
                # global ipss_app

                self.ipss_app = self.a_gateway.entry_point

                s = False
            except:
                time.sleep(0.1)

        from gym import spaces

        if case_files is not None:
            _case_files = transfer2JavaStringAry(self.a_gateway,case_files)
        else:
            _case_files = None

        #initialize the power system simulation service


        #  {observation_history_length,observation_space_dim, action_location_num, action_level_num};
        dim_ary= self.ipss_app.initStudyCase(_case_files,dyn_sim_config_file,rl_config_file)


        observation_history_length = dim_ary[0]
        observation_space_dim = dim_ary[1]

        # set agent-environment interaction time step,
        self.time_step = self.ipss_app.getEnvTimeStep() if self.ipss_app.getEnvTimeStep() > 0 else self.default_time_step


        
        self.action_type = self.ipss_app.getActionSpaceType()

        print ('action type = ', self.action_space)
        
        # for discrete action space
              #define action and observation spaces
        """
        if(action_location_num == 1):
            self.action_space      = spaces.Discrete(action_level_num) # Discrete, 1-D dimension
        else:
            #print('N-D dimension Discrete Action space is not supported it yet...TODO')
            # the following is based on the latest  gym dev version
            # action_def_vector   = np.ones(action_location_num, dtype=np.int32)*action_level_num

            # for gym version 0.10.4, it is parametrized by passing an array of arrays containing [min, max] for each discrete action space
            # for exmaple,  MultiDiscrete([ [0,4], [0,1], [0,1] ])

            action_def_vector = np.ones((action_location_num,2),dtype=np.int32)
            action_def_vector[:,1] = action_level_num -1
            aa = np.asarray(action_def_vector, dtype=np.int32)

            self.action_space   = spaces.MultiDiscrete(action_def_vector) # Discrete, N-D dimension
        """
        if  self.action_type.lower() == 'discrete':
            print ('observation_history_length,observation_space_dim, action_location_num, action_level_num = ')
            print (dim_ary[0], dim_ary[1],dim_ary[2], dim_ary[3])

            action_location_num =  dim_ary[2]
            action_level_num = dim_ary[3]
            
            action_num = action_level_num ** action_location_num
            
            self.action_space = spaces.Discrete(action_num)
            
            self.cnts = np.ones(action_location_num)*action_level_num

        elif self.action_type.lower() == 'continuous':
            print ('observation_history_length,observation_space_dim, action_location_num = ')
            print (dim_ary[0], dim_ary[1],dim_ary[2])
            
            action_ranges = transfer2DJavaArray2NumpyArray(self.ipss_app.getActionValueRanges())
            print ('action value ranges  = ', action_ranges)
            
            low = action_ranges[:,0]
            high = action_ranges[:,1]
            
            print ('action range low =', low, 'action range high =', high)
            print ('low shape:', np.shape(low))

            self.action_space = spaces.Box(low, high, dtype=action_ranges.dtype)


            if force_symmetric_continuous_action:  # i.e., force np.abs(low) == high
                if not (np.abs(low) == high).all():
                    print('!!Warming: the original action space is non-symmetric, convert it to [-1,1] for each action')
                    self.original_action_space = spaces.Box(low, high, dtype=action_ranges.dtype)
                    ones = np.ones_like(low)
                    self.action_space = spaces.Box(-ones, ones, dtype=action_ranges.dtype)
                    self.is_action_space_scaled = True

                    #print (self.action_space)

        self.observation_space = spaces.Box(-999,999,shape=(observation_history_length * observation_space_dim,)) # Continuous

        #print ('obs shape[0]',self.observation_space.shape[0])
        self.seed()

        #TOOD get the initial states
        self.state = None

        self.steps_beyond_done = None
        self.restart_simulation = True

    def seed(self, seed=None):
        self.np_random, seed = seeding.np_random(seed)
        return [seed]

    def step(self, action):
        ## This is a tentative solution of the error of additional dimension is added to the returned
        # action in OpenAI Gym DDPG
        if not self.action_space.contains(action):
            action = action[0]
        assert self.action_space.contains(action), "%r (%s) invalid"%(action, type(action))

        # step-1 convert from Gym discrete actions into actions applied to power system simulator
        
        actionMapped = None
        if self.action_type == 'discrete':
            actionMapped = refer_new(action, self.cnts)
        elif self.action_type == 'continuous':
            actionMapped = np.asarray(action)
            
        #print("action from policy =", actionMapped)
        actionPyAry = np.asarray(actionMapped,dtype = np.float64)

        if self.is_action_space_scaled and self.original_action_space is not None:
            #Rescale the action from [-1, 1] to [low, high]
            actionPyAry = unscale_action(self.original_action_space, actionPyAry)

        # print(actionPyAry, 'len = ', actionPyAry.size)

        # np array size = number of elements in the array
        actionJavaAry = self.a_gateway.new_array(self.a_gateway.jvm.double, actionPyAry.size)

        if(actionPyAry.size ==1):
            actionJavaAry[0] = float(action)
        else:
            i = 0
            for x in actionPyAry:
                actionJavaAry[i] = x
                i = i + 1

        # step-2 apply the actions to the simulator and run the simulation for one interaction step forward
        self.ipss_app.nextStepDynSim(self.time_step, actionJavaAry, self.action_type)

        # step-3 retrieve the state from InterPSS simulation service

        # observations is a Java_Collections array
        observations = self.ipss_app.getEnvObservations()

        # convert it from Java_collections array to native Python array
        self.state = transfer2DJavaArray2NumpyArray(observations)
        
        #print('observation shape: ', np.shape(self.state))

        # step-4 check the states to see whether it go beyond the limits
        done = self.ipss_app.isSimulationDone()


        if not done:
            reward = self.ipss_app.getReward()

        elif self.steps_beyond_done is None:
            self.steps_beyond_done = 0
            reward = self.ipss_app.getReward() # even it is done, ipss_app would calculate and return a corresponding reward
        else:
            if self.steps_beyond_done == 0:
                logger.warning("You are calling 'step()' even though this environment has already returned done = True. You should always call 'reset()' once you receive 'done = True' -- any further steps are undefined behavior.")
            self.steps_beyond_done += 1

            reward = 0.0

        return np.array(self.state).ravel(), reward, done, {}

    def reset(self):

       

        # reset need to randomize the operation state and fault location, and fault time
        study_cases = self.ipss_app.getStudyCases()
        
        total_case_num = len(study_cases)
        if total_case_num == 0:
            total_case_num = 1
            
        case_Idx = np.random.randint(0,total_case_num) # an integer
        
        total_fault_buses = len(self.ipss_app.getFaultBusCandidates())

       
        fault_bus_idx = np.random.randint(0, total_fault_buses)# an integer, in the range of [0, total_bus_num-1]
        
        #fault_bus_idx = 3 # an integer, in the range of [0, total_bus_num-1]
        #fault_start_time =random.uniform(0.99, 1.01) # a double number, in the range of [0.2, 1]
        fault_start_time_ary = transfer1DJavaArray2NumpyArray(self.ipss_app.getFaultStartTimeCandidates())
        fault_start_time = fault_start_time_ary[np.random.randint(0, len(fault_start_time_ary))]
       
        ftd_candidates = transfer1DJavaArray2NumpyArray(self.ipss_app.getFaultDurationCandidates())
        
        fault_duration_time = ftd_candidates[np.random.randint(0, len(ftd_candidates))] # a double number, in the range of [0.08, 0.4]
  
  
        # reset initial state to states of time = 0, non-fault

        self.ipss_app.reset(case_Idx, fault_bus_idx, fault_start_time, fault_duration_time)

        #self.state = None

        # observations is a Java_Collections array
        observations = self.ipss_app.getEnvObservations();

        # convert it from Java_collections array to native Python array
        self.state = transfer2DJavaArray2NumpyArray(observations)

        #print(self.state)

        self.steps_beyond_done = None
        self.restart_simulation = True
        

        return np.array(self.state).ravel()

    # init the system with a specific state and fault
    def validate(self, case_Idx, fault_bus_idx, fault_start_time, fault_duation_time):

        self.ipss_app.reset(case_Idx,fault_bus_idx,fault_start_time,fault_duation_time)

        # observations is a Java_Collections array
        observations = self.ipss_app.getEnvObservations()

        # convert it from Java_collections array to native Python array
        self.state = transfer2DJavaArray2NumpyArray(observations)

        self.steps_beyond_done = None
        self.restart_simulation = True

        return np.array(self.state).ravel()
    
    def close_connection(self):
        self.a_gateway.shutdown()
        self.a_gateway.close(keep_callback_server=False, close_callback_server_connections=False)

        self.server_process.terminate()
        print("Java server terminated with PID:", self.server_process.pid)
        

    # def _render(self, mode='human', close=False):
