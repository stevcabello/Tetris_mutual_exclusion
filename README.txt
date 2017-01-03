# MUTUAL EXCLUSION TETRIS

This version of Tetris relies on a Peer to Peer network for the communication
between peers. However, it uses a local server to connect each peer into the
network and also provides information about the list of peers connected at the
moment the peers are ready to play, since it was difficult to find an ad-hoc
setup.

If one peer starts the game, automatically all of the other peers will start
the game as well with the algorithm selected by the initiator.

At most one player is able to move the block at a time. This mutual exclusion
behaviour is controlled by the algorithm.


# Server Setup
The Server is inside tetris_p2pServer folder. The server is based on Node.js
in a file named tetris_server.js

In order to run the server follow this steps:

1) Install node.js (in case you don't have it)
2) open Command Prompt
3) cd to the folder tetris_p2pServer
4) in the command prompt write: node tetris_server.js
5) the server will start listen on port 3000 (make sure this port isn't being used)


# Game Setup
Inside the Globals.java file change the IP_SERVER constant value with the IP
address of your machine, this will act as the local server.


#Demo
A video of the application functionality --> https://youtu.be/UC7zSaU3n2s

#Project important sources
- The game engine is based on:
    https://github.com/JLLLinn/Tetris-Game

- The p2p network communicaton libraries are from:
    https://github.com/dsg-unipr/sip2peer/tree/master/android
