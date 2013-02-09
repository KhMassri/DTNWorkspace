
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
    logfile = open("../data/LOG-"+tag+".CSV").readlines()
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

#remsgs.sort()
#avghpcnt.sort()
print remsgs
print ['%0.2f' %i for i in avghpcnt]


sinked = []
TAGS = tags+emptags
perNodeSinked = zeros(len(TAGS), int)        # Create empty array ready to receive result
for i in range(0, len(TAGS)):
    perNodeSinked[i] = 0
shpcnt = 0.0
delay = 0.0
s = 0
lst=[]
for tag in sinks:
    logfile = open("../data/LOG-"+tag+".CSV").readlines()
    for line in logfile:
        lst.append(line.split(','))

lst.sort(key=lambda x: x[4],reverse=True)
for l in lst:
    if not(l[2] in sinked):
        sinked.append(l[2])
        delay=delay+(int(l[1])-int(l[3]))
        s=s+1
        shpcnt=shpcnt+int(l[4])
        if '35F8' not in (l[2])[0:4]:
            perNodeSinked[TAGS.index((l[2])[0:4])] = perNodeSinked[TAGS.index((l[2])[0:4])] +1  
    
print 'Total sinked = %.2f ' %(len(sinked))
#print 'Total sinked = %.2f ' %(len(sinked)/(20*4500/30.0))
print 'sinked HopCount Avg = %.2f ' %(shpcnt/s+1)
print 'sinked delay Avg = %.2f ' %(delay/s)
print perNodeSinked 


#matrix.sort(key=lambda x: x[0],reverse=True)
#print matrix
#str(lst[2])[0:4]==emptags[0] or str(lst[2])[0:4]==emptags[1] or str(lst[2])[0:4]==emptags[2] or str(lst[2])[0:4]==emptags[3]
#con.view('i8,i8,i8,i8').sort(order=['f0'], axis=0)
#con = zeros((10000,4), dtype=str)
#out.write(line[0]+line[1]+line[2]+'\n')


