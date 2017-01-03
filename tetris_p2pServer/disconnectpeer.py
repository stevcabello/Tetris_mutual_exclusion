import sys

ipaddr = sys.argv[1]

ipaddr = ipaddr.split(";")[0] #Due to sometimes it comes as ipaddr;transport=udp


f = open("tetrispeerslist.txt","r+")
peers = f.read().splitlines()

if ipaddr in set(peers):
	f.seek(0)
	for peer in peers:
	    if peer != ipaddr:
	        f.write(peer + "\n")
	f.truncate()
	f.close()

	print ipaddr + " removed from peers list"
else:
	print "Nothing to remove"
