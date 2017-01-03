import sys

ipaddr = sys.argv[1]

ipaddr = ipaddr.split(";")[0] #Due to sometimes it comes as ipaddr;transport=udp


f = open("tetrispeerslist.txt","r")
peers = f.read().splitlines()
f.close()

with open("tetrispeerslist.txt","a") as f:
    if ipaddr not in set(peers):
        f.write(ipaddr + "\n")

f.close()

print ipaddr
