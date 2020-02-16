import matplotlib.pyplot as plt
import numpy as np
import sys

insertion = [1307, 1255, 1235, 1219, 1217]
full_retrieval = [669, 996, 968, 1187, 1252]

x = np.arange(5)



my_xticks = [1, 2, 4, 8, 16]
# my_yticks = y
plt.xticks(x, my_xticks)
# plt.yticks(y, my_yticks)

plt.plot(x, insertion, marker="o")
plt.plot(x, full_retrieval, marker="s")


plt.xlabel('Number of Clients')
plt.ylabel('Completion Time (in seconds)')

plt.title('Completion Time')
plt.tight_layout()

plt.legend(['Insertion of 50k keys', 'Retrieval of 50k keys'])

plt.savefig("N_Client_1_Server_Test")