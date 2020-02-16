import matplotlib.pyplot as plt
import numpy as np
import sys

FIFO = [136, 131, 127, 86, 17]
LFU = [129, 130, 125, 93, 30]
LRU = [133, 125, 119, 82, 17]

x = np.arange(5)

my_xticks = [1, 50, 100, 500, 1000]
# my_yticks = y
plt.xticks(x, my_xticks)
# plt.yticks(y, my_yticks)

plt.plot(x, FIFO, marker="o")
plt.plot(x, LFU, marker="s")
plt.plot(x, LRU, marker="*")


plt.xlabel('Cache Size')
plt.ylabel('Completion Time (in seconds)')

plt.title('Completion Time with different cache managment')
plt.tight_layout()

plt.legend(['FIFO', 'LFU', 'LRU'])

plt.savefig("Cache")