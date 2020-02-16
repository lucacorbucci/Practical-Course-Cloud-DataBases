import matplotlib.pyplot as plt
import numpy as np
import sys

insertion = [1307, 1309, 1246, 1210, 1064]
full_retrieval = [669, 775, 680, 665, 754]

x = np.arange(5)



my_xticks = [1, 2, 4, 8, 16]
# my_yticks = y
plt.xticks(x, my_xticks)
# plt.yticks(y, my_yticks)

plt.plot(x, insertion, marker="o")
plt.plot(x, full_retrieval, marker="s")


plt.xlabel('Number of Servers')
plt.ylabel('Completion Time (in seconds)')

plt.title('Completion Time')
plt.tight_layout()

plt.legend(['Insertion of 50k keys', 'Retrieval of 50k keys'])

plt.savefig("1_Client_N_Server_Test")