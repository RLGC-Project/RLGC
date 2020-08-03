'''
    Copyright (C) 2005-17 www.interpss.org
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
'''

import numpy as np

from py4j.java_gateway import JavaGateway

#
# define InterPSS train/test case service 
#
gateway = JavaGateway()
ipss_app = gateway.entry_point

#
# define configuration parameters
#
learning_rate = 0.001
train_steps = 1000

# 
# function to transfer data from a tensor array to a Java double array
#
def transfer2JavaDblAry(tArray, size):
    dblAry = gateway.new_array(gateway.jvm.double, size)
    i = 0
    for x in tArray:
        dblAry[i] = float(x)
        i = i + 1
    return dblAry

#
# function to transfer a Java [[x], [y]] to two Python arrays [x], [y]
#
def transfer2PyArrays(ary) :
    xSize = len(ary[0][0].split()) 
    ySize = len(ary[1][0].split())
    point = len(ary[0])
    x = np.zeros((point,xSize))
    y = np.zeros((point,ySize))
    for i in range(point):
        x[i] = np.array([ary[0][i].split()])
        y[i] = np.array([ary[1][i].split()])
    return x, y

def normalization(ary) :
    aver = (np.divide(np.sum(ary,axis=0),len(ary)))
    ran = (np.max(ary,axis=0)-np.min(ary,axis=0))
    for i in range(len(ran)):
        if(ran[i]==0):ran[i]=0.00001
    sub = np.subtract(ary,aver)
    ary = np.divide(sub,ran)
    return ary ,aver ,ran
#
# Output functions
#    
def printArray(ary, msg) :
    print(msg)
    for x in ary :
        print(x)
        
def print2DArray(ary2D, msg1, msg2) :
    print(msg1)
    for ary in ary2D :
        print(msg2)
        for x in ary :
            print(x)        