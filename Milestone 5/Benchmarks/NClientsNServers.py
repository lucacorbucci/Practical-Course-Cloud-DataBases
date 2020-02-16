import matplotlib.pyplot as plt
import numpy as np
import sys

insertion = [1307, 1199, 645, 535, 756]
full_retrieval = [669, 941, 414, 514, 802]

x = np.arange(5)



my_xticks = [1, 2, 4, 8, 16]
# my_yticks = y
plt.xticks(x, my_xticks)
# plt.yticks(y, my_yticks)

plt.plot(x, insertion, marker="o")
plt.plot(x, full_retrieval, marker="s")


plt.xlabel('Number of Servers and Clients')
plt.ylabel('Completion Time (in seconds)')

plt.title('Completion Time')
plt.tight_layout()

plt.legend(['Insertion of 50k keys', 'Retrieval of 50k keys'])

plt.savefig("N_Clients_N_Servers_Test.jpg")