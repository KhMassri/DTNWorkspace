
/**

Written by: Khalil Massri
DTN Implementation
 */


#include"dtn_queue.h"



uint8_t IsEmpty(QueueRecord* Q) {
	return Q->Size == 0;
}

uint8_t IsFull(QueueRecord* Q) {
	return Q->Size == CAPACITY;
}

void MakeEmpty(QueueRecord* Q) {

	Q->Size = 0;
	Q->Front = 1;
	Q->Rear = 0;

}

// if you wanna make it static then capacity musbe be queue field and queue sent as para
uint8_t Succ(uint8_t Value) {
	if (++Value == CAPACITY) {
		Value = 0;
	}
	return Value;
}

/*If the queue is full then evict the front (the first was enqueued) */
void Enqueue(DTNMsg X, QueueRecord* Q) {

	if (IsFull(Q)) {
		Dequeue(Q);
	} else {
		Q->Size++;
		Q->Rear = Succ(Q->Rear);
		Q->Array[Q->Rear] = X;
	}

}


DTNMsg* Front(QueueRecord* Q) {

	return &(Q->Array[Q->Front]);
}




void Dequeue(QueueRecord* Q) {

	if (IsEmpty(Q)) {
		return;
	} else {
		Q->Size--;
		Q->Front = Succ(Q->Front);
	}

}

DTNMsg FrontAndDequeue(QueueRecord* Q) {

	DTNMsg temp = {0,0,0,0,0,0,0};

	if (IsEmpty(Q))
		return temp;

	else {
		Q->Size--;
		temp = Q->Array[Q->Front];
		Q->Front = Succ(Q->Front);
	}
	return temp;

}
/*
 * Msg with least prob will be at Rear
 * */

void SortQueue(QueueRecord* Q){
	if(IsEmpty(Q))
		return;
	DTNMsg temp;
	uint8_t i,j;

	for(i=Q->Front;i!=Q->Rear;i=Succ(i))
	{
		j=Succ(i);
		while(1){
			if((Q->Array[j]).prop > (Q->Array[i]).prop)
			{
				temp = Q->Array[i];
				Q->Array[i]=Q->Array[j];
				Q->Array[j]=temp;
			}
			if(j==Q->Rear)
				break;
			j=Succ(j);
		}
	}
}

void RotQueue(QueueRecord* Q){
	if(IsEmpty(Q))
			return;
	DTNMsg temp = FrontAndDequeue(Q);
	Enqueue(temp,Q);

}


uint8_t Contains(QueueRecord* Q, uint32_t Id){
	if(IsEmpty(Q))
		return 0;
	uint8_t i = Q->Front;
	do{
		if((Q->Array[i]).seq == Id)
			return 1;
		if(i == Q->Rear)
			break;
		i=Succ(i);
	}while(i!=Q->Front);

	return 0;


}



