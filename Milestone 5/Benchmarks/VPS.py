import pandas
import matplotlib.pyplot as plt
import numpy as np

operations = ["5000 Insertion", "5000 Retrieval"]
vps = [88.253, 62.317]
vps_full = [403.088, 339.253]

operations.reverse()
vps.reverse()
vps_full.reverse()

ind = np.arange(len(vps))
width = 0.35

fig, ax = plt.subplots()
ax.barh(ind, vps, height=.35, label='Client, Server and ECS on the VPS')
ax.barh(ind + width, vps_full, height=.35, label='Client on a Local Machine, ECS and Server on VPS')

ax.set(yticks=ind + width, yticklabels=operations)
ax.legend()


plt.title('Communication Cost Analysis')
plt.tight_layout()

plt.show()

# https://matplotlib.org/gallery/statistics/barchart_demo.html

# Add X axes legend