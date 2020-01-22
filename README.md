# RLGC
Repo of the Reinforcement Learning for Grid Control (RLGC) Project

We explore to use deep reinforcement learning methods for control and decision-making problems in power systems.

*NOTE:* RLGC is under active development and may change at any time. Feel free to provide feedback and comments.
    
--------------------------------------
### Environment setup  

   To run the training, you need python 3.5 or above and Java 8. Unix-based OS is recommended. We suggest using Anaconda to create virtual environment from the yaml file we provided.  

   - To clone our project  
     
     ```  
     git clone https://github.com/RLGC-Project/RLGC.git
     ```

   - To create the virtual environment   
     In case you like to use our development environment, we have provided environment. yml file
     
     ```
     cd RLGC
     conda env create -f environment. yml
     ``` 
     
     or you can create your own environment
     ```
     cd RLGC    
     conda env create --name <your-env-name>  
     ```

     If you get errors about OpenAI gym you probably need to install cmake and zlib1g-dev. For example on Ubuntu machine, do the following command.  

     ```  
     sudo apt-get upgrade
     sudo apt-get install cmake
     sudo apt-get install zlib1g-dev
     ```

     After creating environment **<your-env-name>**, you can activate the virtual environment and do development under this environment.  

   - To activate virtual environment  

     ```
     source activate <your-env-name>  
     ```

   - To deactivate virtual environment  

     ```
     source deactivate
     ```


### Training

- With the RLGCJavaSever version 0.80 or newer and 
grid environment definition version 5 (PowerDynSimEnvDef_v5.py)
 or newer, users don't need to start the java server explicitly. 
 The server will be started automatically when the grid environment
 ``PowerDynSimEnv`` is created.
- To launch the training, you need first activate the virtual 
environment. Then run the following scripts. 
```trainKundur2areaGenBrakingAgent.py``` is used for training 
the generator braking agent for the Kundur 2-area system and ```trainIEEE39LoadSheddingAgent_*.py``` is used for training an agent for regional load shedding in IEEE 39-bus system


```
source activate <your-env-name> 
cd RLGC/src/py  
python trainIEEE39LoadSheddingAgent_discrete_action.py 
```

During the training the screen will dump out the training log. After training, you can deactivate the virtual environment by  

```
source deactivate
```



###  Check training results and test trained model

Two Jupyter notebooks (with Linux and Windows versions-- directory paths are specified differently) are provided as examples for checking training results and testing trained RL model.



### Customize the grid environment for training and testing
If you want to develop a new grid environment for RL training or customize the existing grid environment (e.g. IEEE 39-bus system for load shedding), the simplest way is through providing 
your own cases and configuration files. 

When you open ``trainIEEE39LoadSheddingAgent_discrete_action.py `` you will notice the following
codes:

```
case_files_array =[]
case_files_array.append(repo_path + '/testData/IEEE39/IEEE39bus_multiloads_xfmr4_smallX_v30.raw')
case_files_array.append(repo_path + '/testData/IEEE39/IEEE39bus_3AC.dyr')

....
# configuration files for dynamic simulation and RL
dyn_config_file = repo_path + '/testData/IEEE39/json/IEEE39_dyn_config.json'
rl_config_file = repo_path + '/testData/IEEE39/json/IEEE39_RL_loadShedding_3motor_2levels.json'

env = PowerDynSimEnv(case_files_array,dyn_config_file,rl_config_file, jar_path, java_port)
```

They are to specify the cases and configuration files for dynamic simulation and RL training.
You can develop your environment by following these examples. Since ``PowerDynSimEnv`` is defined based on 
OpenAI Gym environment definition, once the environment is created, you can use it like other Gym environments,
and seamlessly interface it with RL algorithms provided in OpenAI baselines or Stable baselines 


--------------------------------------

### Citation

If you use this code please cite it as:

```
@article{huang2019adaptive,
  title={Adaptive Power System Emergency Control using Deep Reinforcement Learning},
  author={Huang, Qiuhua and Huang, Renke and Hao, Weituo and Tan, Jie and Fan, Rui and Huang, Zhenyu},
  journal={IEEE Transactions on Smart Grid},
  year={2019},
  publisher={IEEE}
}
```


--------------------------------------
#### Communication

If you spot a bug or have a problem running the code, please open an issue.

Please direct other correspondence to Qiuhua Huang: qiuhua DOT huang AT pnnl DOT gov

