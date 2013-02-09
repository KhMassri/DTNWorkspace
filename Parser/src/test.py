
from numpy.random import rand



''' how to build aniterator'''

class it:
    def __init__(self):
        #start at -1 so that we get 0 when we add 1 below.
        self.count = -1
    #the __iter__ method will be called once by the for loop.
    #the rest of the magic happens on the object returned by this method.
    #in this case it is the object itself.
    def __iter__(self):
        return self
    #the next method will be called repeatedly by the for loop
    #until it raises StopIteration.
    def next(self):
        self.count += 1
        if self.count < 4:
            return self.count
        else:
            #a StopIteration exception is raised
            #to signal that the iterator is done.
            #This is caught implicitly by the for loop.
            raise StopIteration 



def some_func():
    return it()

for i in some_func():
    print i

s=[1,2,3,4]

print id(s)
s+=[1,1,1,1]
print id(s)





'''
def some_function():
    
   
    for i in xrange(4):
        yield i
        yield [7,8,9]
        

for i in some_function():
    print i
    
    
for i, v in enumerate(['tic', 'tac', 'toe']):
    print i, v
    
'''