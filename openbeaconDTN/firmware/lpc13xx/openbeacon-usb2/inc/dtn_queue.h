

/**

Written by: Khalil Massri
DTN Implementation
*/


#ifndef __DTN_QUEUE_H__
#define __DTN_QUEUE_H__

#define CAPACITY 100

#include <openbeacon.h>
#include "openbeacon-proto.h"

typedef struct {
  uint8_t Front;
  uint8_t Rear;
  uint8_t Size;
  DTNMsg Array[CAPACITY];
} PACKED QueueRecord;


extern uint8_t IsEmpty(QueueRecord* Q);

extern uint8_t IsFull(QueueRecord* Q);

extern void MakeEmpty(QueueRecord* Q);
extern uint8_t Succ(uint8_t Value);
extern void Enqueue(DTNMsg X, QueueRecord* Q);
extern DTNMsg* Front(QueueRecord* Q);
extern void Dequeue(QueueRecord* Q);
extern DTNMsg FrontAndDequeue(QueueRecord* Q);
extern void SortQueue(QueueRecord* Q);
extern uint8_t Contains(QueueRecord* Q, uint32_t Id);
extern void RotQueue(QueueRecord* Q);

#endif/*__DTN_QUEUE_H__*/
