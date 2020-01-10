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

time_steps = 5000
ll = 5e-5

log_dir = "./logFiles"

results_plotter.plot_results([log_dir], time_steps, results_plotter.X_TIMESTEPS, "IEEE 39 Bus load shedding w/SAC")
plt.savefig(log_dir+'/IEEE_39Bus_loadshedding_SAC {}_{}.png'.format(str(time_steps),str(ll)))
plt.show()