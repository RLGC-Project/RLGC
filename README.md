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
First launch java server which is used to simulate the power grid system. Then launch your training in the virtual environment. We provide two power grid systems in the examples. ```RLGCJavaServer0.72.jar``` is the latest release and can be used for the IEEE 39-bus system and ```RLGCJavaServerSimple.jar```is only used for the Kundur 2-area system.   

- To launch the java server, open a new terminal    

```
cd ~
cd RLGC/lib  
java -jar RLGCJavaServer0.72.jar 25001
```
The last parameter is the communication port number between grid system and the training agent. You can switch the port number if necessary.  


- To launch the training, you need first activate the virtual environment. Then run the following scripts. ```trainKundur2areaGenBrakingAgent.py``` is used for training the generator braking agent for the Kundur 2-area system and ```trainIEEE39LoadSheddingAgent.py``` is used for training an agent for regional load shedding in IEEE 39-bus system


```
source activate <your-env-name> 
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

