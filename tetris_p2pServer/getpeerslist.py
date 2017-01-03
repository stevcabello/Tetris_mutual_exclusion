f = open("tetrispeerslist.txt","r")

peers = f.read().splitlines()

f.close()

print peers
