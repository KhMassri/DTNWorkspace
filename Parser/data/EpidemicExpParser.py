
from numpy import *


out  = open("results.txt","w")
tags = ['1ADF','2B53','3AB0','6BF7','26F7','51F7','72C2','79CA','485B','784A','1376','BAD3','C31E','D758','ED09','9268']
emptags = ['3A28','5F34','EDB1','6EB2']
sinks = ['1FBF','89B0']
li = []



index = 0
remsgs=[]
avghpcnt =[]
for tag in tags:
	logfile = open("LOG-"+tag+".CSV").readlines()
	hcnts=0.0
	re=0
	# each list is:  
	# device,recvTime,MsgSeq,crtTime,hpcnt
	for line in logfile:
		lst=line.split(',')
		if not (int(lst[1])==int(lst[3])):
			re =re+1
			hcnts = hcnts+int(lst[4])

	avghpcnt.append(hcnts/re)
	remsgs.append(re)

remsgs.sort()
avghpcnt.sort()
print remsgs
print ['%0.2f' %i for i in avghpcnt]


delivered = []
shpcnt = 0.0
s = 0
lst=[]
for tag in sinks:
	logfile = open("LOG-"+tag+".CSV").readlines()
	for line in logfile:
		lst.append(line.split(','))

	
lst.sort(key=lambda x: x[4],reverse=True)
for l in lst:
	if not(l[2] in delivered):
		delivered.append(l[2])
	s=s+1
	shpcnt=shpcnt+int(l[4])
print 'Total delivered = %.2f ' %(len(delivered)/(20*4500/30.0))
print 'sinked HopCount Avg = %.2f ' %(shpcnt/s)


#print map(lambda x:x**2,range(1,10))
#matrix.sort(key=lambda x: x[0],reverse=True)
#print matrix
#str(lst[2])[0:4]==emptags[0] or str(lst[2])[0:4]==emptags[1] or str(lst[2])[0:4]==emptags[2] or str(lst[2])[0:4]==emptags[3]
#con.view('i8,i8,i8,i8').sort(order=['f0'], axis=0)
#con = zeros((10000,4), dtype=str)
#out.write(line[0]+line[1]+line[2]+'\n')



