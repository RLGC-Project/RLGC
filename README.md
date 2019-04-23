# RLGC
Repo of the Reinforcement Learning for Grid Control (RLGC) Project


  We explore to use deep reinforcement learning methods for emergency control in power grid system.  

### Environment setup  

   To run the training, you need python 3.5 or above and Java 8. Unix-based OS is required. We suggest using Anaconda 5.0 to create virtual environment from the yaml file we provided.  

   - To clone our project  
     
     ```  
     git clone https://github.com/RLGC-Project/RLGC.git
     ```

   - To create the virtual environment   

     ```
     cd RLGC    
     conda env create -f RLGC_environment.yml  
     ```

     If you get errors about OpenAI gym you probably need to install cmake and zlib1g-dev. For example on Ubuntu machine, do the following command.  

     ```  
     sudo apt-get upgrade
     sudo apt-get install cmake
     sudo apt-get install zlib1g-dev
     ```

     After creating environment **py3ml**, you can activate the virtual environment and do development under this environment.  

   - To activate virtual environment  

     ```
     source activate py3ml
     ```

   - To deactivate virtual environment  

     ```
     source deactivate
     ```

### Modification of Gym source code  

Note that we need to change the source code of OpenAI Gym Baseline models because To modify the source code,   

```  
cd your_path_to_anaconda_directory/envs/py3ml/lib/python3.6/site-packages/baselines/deepq
```
eg, mine will be  
```  
cd ~/anaconda/envs/py3ml/lib/python3.6/site-packages/baselines/deepq
```

Subsequently you need to modify the ```learn()``` function in ```simple.py``` script. 
First, add ```trained_model = none``` to the funtion argument list.  Then, replace lines of 229 to 255 by following codes.  

```
    episode_rewards = [0.0]
    saved_mean_reward = None
    obs, starttime,durationtime = env.reset()

    #i = 0
    #noise = 0.01 * np.random.randn(4,8,301)
    #np.save("./noise", noise)
    with tempfile.TemporaryDirectory() as td:
        model_saved = False
        model_file = os.path.join(td, "model")
        for t in range(max_timesteps):
            if callback is not None:
                if callback(locals(), globals()):
                    break
            # Take action and update exploration to the newest value
            action = act(np.array(obs)[None], update_eps=exploration.value(t))[0]
            new_obs, rew, done, _ = env.step(action)
            # Store transition in the replay buffer.
            replay_buffer.add(obs, action, rew, new_obs, float(done))
            obs = new_obs #* (1 + noise[:,:,i].flatten())
            #i += 1

            episode_rewards[-1] += rew
            if done:
                #i = 0
                obs, starttime, durationtime = env.reset()
                episode_rewards.append(0.0)
```

Alternatively, you can simply use the ```simple.py``` in the src/py/openAI_gym to replace the same file in the directory <our_path_to_anaconda_directory>/envs/py3ml/lib/python3.6/site-packages/baselines/deepq
But you will be at risk of potential compatibility issue with newer version of OpenAI Gym, as we may not test it against future versions of OpenAI Gym. If you have confront such an issue, please report to us.

Remember to remove the original complied cache file as follows: 

```
cd your\_path\_to\_simple.py\_file/__pycache__/
rm simple.cpython-36.pyc
```






### Training
First launch java server which is used to simulate the power grid system. Then launch your training in the virtual environment. We provide two power grid systems in the examples. ```RLGCJavaServer0.72.jar``` is the latest release and can be used for the IEEE 39-bus system and ```RLGCJavaServerSimple.jar```is only used for the Kundur 2-area system.   

- To launch the java server, open a new terminal    

```
cd ~
cd RLGC/lib  
java -jar RLGCJavaServer0.70.jar 25001
```
The last parameter is the communication port number between grid system and the training agent. You can switch the port number if necessary.  


- To launch the training, you need first activate the virtual environment. Then run the following scripts. ```trainKundur2areaGenBrakingAgent.py``` is used for training the generator braking agent for the Kundur 2-area system and ```trainIEEE39LoadSheddingAgent.py``` is used for training an agent for regional load shedding in IEEE 39-bus system


```
source activate py3ml
cd RLGC/src/py  
python trainIEEE39LoadSheddingAgent.py 
```
During the training the screen will dump out the training log. After training, you can deactivate the virtual environment by  

```
source deactivate
```



###  Check training results and test trained model

Two Jupyter notebooks (with Linux and Windows versions-- directory paths are specified differently) are provided as examples for checking training results and testing trained RL model.


--------------------------------------
#### Communication

If you spot a bug or have a problem running the code, please open an issue.

Please direct other correspondence to Qiuhua Huang: qiuhua DOT huang AT pnnl DOT gov

