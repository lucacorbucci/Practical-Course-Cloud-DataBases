# Advanced Practical Course: Cloud Data Bases 🎓 👨🏻‍💻

## Milestone 1 ✅

The objective of this assignment is to implement a simple client program that is able to establish a TCP connection to a given server and exchange text messages with it. The client should provide a command line-based interface that captures the user’s input and controls the interaction with the server. Besides connection establishment and tear down, the user must be able to pass messages to the server. These messages are in turn echoed back to the client where they are displayed to the user. A sequence of interactions is shown below.

'''
EchoClient> connect clouddatabases.i13.in.tum.de 5153
EchoClient> Connection to MSRG Echo server established: /131.159.52.23 / 5153 EchoClient> send hello world
EchoClient> hello world
EchoClient> disconnect
EchoClient> Connection terminated: 131.159.52.23 / 5153
EchoClient> send hello again
EchoClient> Error! Not connected!
EchoClient> quit
EchoClient> Application exit!
'''

The assignment serves to refresh or establish basic knowledge of TCP-based network programming using Java stream sockets, mostly from the client’s perspective. This embodies concepts such as client/server architecture, network streams, and message serialization.

## Milestone 2 ✅

The objective of this assignment is to implement a simple storage server that persists data to disk (e.g., a file). In addition, a small fraction of the data, i.e., a configurable number of key- value pairs can be cached in the servers’ main memory. The storage server should provide the typical key-value query interface. To achieve this objective, the echo client from Assignment 1 should be extended to be able to communicate with the storage server and query it. These tasks require the development of an applicable communication protocol and a suitable message format.
In this assignment, a single storage server will serve requests from multiple clients, whereas in the next assignment, we’ll experiment with deploying a number of storage servers based on the artifacts developed in this and the previous assignment.
