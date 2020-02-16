# Advanced Practical Course: Cloud Data Bases 🎓 👨🏻‍💻

This repository contains all the milestones implemented during the course "Advanced Practical Course: Cloud Data Bases" @ Technische Universität München.

## Milestone 1 ✅

The objective of this assignment is to implement a simple client program that is able to establish a TCP connection to a given server and exchange text messages with it. The client should provide a command line-based interface that captures the user’s input and controls the interaction with the server. Besides connection establishment and tear down, the user must be able to pass messages to the server. These messages are in turn echoed back to the client where they are displayed to the user. A sequence of interactions is shown below.

```
EchoClient> connect clouddatabases.i13.in.tum.de 5153
EchoClient> Connection to MSRG Echo server established: /ddd.aaa.dd.xx / 5153 EchoClient> send hello world
EchoClient> hello world
EchoClient> disconnect
EchoClient> Connection terminated: ddd.aaa.dd.xx  / 5153
EchoClient> send hello again
EchoClient> Error! Not connected!
EchoClient> quit
EchoClient> Application exit!
```

The assignment serves to refresh or establish basic knowledge of TCP-based network programming using Java stream sockets, mostly from the client’s perspective. This embodies concepts such as client/server architecture, network streams, and message serialization.

## Milestone 2 ✅

The objective of this assignment is to implement a simple storage server that persists data to disk (e.g., a file). In addition, a small fraction of the data, i.e., a configurable number of key- value pairs can be cached in the servers’ main memory. The storage server should provide the typical key-value query interface. To achieve this objective, the echo client from Assignment 1 should be extended to be able to communicate with the storage server and query it. These tasks require the development of an applicable communication protocol and a suitable message format.
In this assignment, a single storage server will serve requests from multiple clients, whereas in the next assignment, we’ll experiment with deploying a number of storage servers based on the artifacts developed in this and the previous assignment.

## Milestone 3 ✅

The objective of this assignment is to extend the storage server architecture from Milestone 2 into a elastic, scalable storage service (cf. the figure below). Data records (i.e., key-value pairs) are distributed over a number of storage servers by exploiting the capabilities of consistent hashing. A single storage server is only responsible for a subset of the whole data space (i.e., a range of successive hash values.) The hash function is used to determine the location of particular tuples (i.e., hash values of the associated keys).


## Milestone 4 ✅

The objective of this assignment is to extend the storage service from Milestone 3 by means of replication. Data records (i.e., key-value pairs) are still distributed over a bunch of storage servers via consistent hashing. However, the storage servers are no longer just responsible for their own subset of the data, but also serve as replicas for data items of other servers. In this sense, a storage server assumes different roles for different tuples. A storage server may be a
- Coordinator node, if it is directly responsible for the tuple, following the concept of consistent hashing. This means it has a position in the ring, such that it is the closest successor server in the ring topology according to the tuple position (cf. Milestone 3).
- Or a replica node, if the tuple is coordinated by another storage node and the data is just replicated on this node. It is either replica_1 if it is first successor of the coordinator or replica_2 if it is the second successor.
Each data item should be replicated exactly on the two storage servers that are following the coordinator node in the ring topology. Replication is invoked and managed by the coordinator node. This means that at least 3 active and responsive storage servers are needed. As long as just 1 or 2 nodes are in the system, no replication is used.
The focus of this milestone is to implement a replication strategy that guarantees eventual consistency. It should also provide a reconciliation mechanism for topology changes. So far (as completed in Milestone 3) the key ranges are rebalanced. In this milestone als the replication is rebalanced when the topology changes.


## Milestone 5 ✅

The objective of this assignment is to extend the distributed database that we wrote in the previous milestone adding new features. I added the possibility to protect a key,value pair using a password. All the previously developed API (PUT, GET, DELETE) now supports the password protection.
