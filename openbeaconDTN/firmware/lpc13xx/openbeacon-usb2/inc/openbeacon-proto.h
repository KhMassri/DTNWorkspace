/****************************************************************************
 *
 * OpenBeacon.org - OnAir protocol specification and definition
 *
 * Copyright 2007 Milosch Meriac <meriac@openbeacon.de>
 *
 ****************************************************************************

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; version 2.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

 */

#ifndef __OPENBEACON_PROTO_H__
#define __OPENBEACON_PROTO_H__

#define CONFIG_BROADCAST_CHANNEL 81
#define CONFIG_UNICAST_CHANNEL 76

#define XXTEA_BLOCK_COUNT 4
#define MAX_POWER_LEVELS 4

#define RFBPROTO_BEACONTRACKER_OLD      16
#define RFBPROTO_READER_ANNOUNCE        22
#define RFBPROTO_BEACONTRACKER_OLD2     23
#define RFBPROTO_BEACONTRACKER          24
#define RFBPROTO_BEACONTRACKER_STRANGE  25
#define RFBPROTO_BEACONTRACKER_EXT      26
#define RFBPROTO_PROXTRACKER            42
#define RFBPROTO_PROXREPORT             69
#define RFBPROTO_PROXREPORT_EXT         70
/********************************************/

#define RFBPROTO_ND_REQ					101
#define RFBPROTO_ND_RES					102
#define RFBPROTO_DTN_MSG				103
/********************************************/

#define PROX_MAX 4
#define PROX_TAG_ID_BITS 12
#define PROX_TAG_COUNT_BITS 2
#define PROX_TAG_STRENGTH_BITS 2
#define PROX_TAG_ID_MASK ((1<<PROX_TAG_ID_BITS)-1)
#define PROX_TAG_COUNT_MASK ((1<<PROX_TAG_COUNT_BITS)-1)
#define PROX_TAG_STRENGTH_MASK ((1<<PROX_TAG_STRENGTH_BITS)-1)

#define PROX_MAX 4

#define RFBFLAGS_ACK			0x01
#define RFBFLAGS_SENSOR			0x02
#define RFBFLAGS_INFECTED		0x04
#define RFBFLAGS_MOVING			0x08
#define RFBFLAGS_SENSOR2		0x10

/* RFBPROTO_READER_COMMAND related opcodes */
#define READER_CMD_NOP			0x00
#define READER_CMD_RESET		0x01
#define READER_CMD_RESET_CONFIG		0x02
#define READER_CMD_RESET_FACTORY	0x03
#define READER_CMD_RESET_WIFI		0x04
#define READER_CMD_SET_OID		0x05
/* RFBPROTO_READER_COMMAND related results */
#define READ_RES__OK			0x00
#define READ_RES__DENIED		0x01
#define READ_RES__UNKNOWN_CMD		0xFF

#define LOGFLAG_PROXIMITY               0x10
#define LOGFLAG_BUTTON                  0x20

typedef struct
{
	uint8_t proto, proto2;
	uint8_t flags, strength;
	uint32_t seq;
	uint32_t oid;
	uint16_t reserved;
	uint16_t crc;
} PACKED TBeaconTrackerOld;

typedef struct
{
	uint8_t strength;
	uint16_t oid_last_seen;
	uint16_t time;
	uint8_t battery;
	uint32_t seq;
} PACKED TBeaconTracker;

typedef struct
{
	uint16_t oid_prox[PROX_MAX];
	uint16_t seq;
} PACKED TBeaconProx;

typedef struct
{
	uint8_t opcode, res;
	uint32_t data[2];
} PACKED TBeaconReaderCommand;

typedef struct
{
	uint8_t opcode, strength;
	uint32_t uptime, ip;
} PACKED TBeaconReaderAnnounce;

typedef union
{
	TBeaconProx prox;
	TBeaconTracker tracker;
	TBeaconReaderCommand reader_command;
	TBeaconReaderAnnounce reader_announce;
} PACKED TBeaconPayload;

typedef struct
{
	uint8_t proto;
	uint16_t oid;
	uint8_t flags;

	TBeaconPayload p;

	uint16_t crc;
} PACKED TBeaconWrapper;

typedef union
{
	uint8_t proto;
	TBeaconWrapper pkt;
	TBeaconTrackerOld old;
	uint32_t block[XXTEA_BLOCK_COUNT];
	uint8_t byte[XXTEA_BLOCK_COUNT * 4];
} PACKED TBeaconEnvelope;

/************************************************/
typedef struct
{
	uint8_t proto;
	uint32_t time, seq;
	//unsigned char from[NRF_MAX_MAC_SIZE];
	uint16_t from;
	uint16_t info;
	uint8_t temp;
	uint16_t crc;
} PACKED NDReq;

typedef struct
{
	uint8_t proto;
	uint32_t time, seq;
	unsigned char from[NRF_MAX_MAC_SIZE];
	//uint16_t from;
	//uint16_t info;
	//uint8_t temp;
	uint16_t crc;
} PACKED NDRes;

typedef struct
{
	uint8_t proto;
	uint32_t time;
	uint32_t seq;
	uint16_t from;
	uint16_t data;
	uint8_t prop;
	uint16_t crc;
} PACKED DTNMsg;


typedef union
{
	uint8_t proto;
	NDReq NDreq;
	NDRes NDres;
	DTNMsg msg;
	uint32_t block[XXTEA_BLOCK_COUNT];
	uint8_t byte[XXTEA_BLOCK_COUNT * 4];
} PACKED DTNMsgEnvelope;

typedef struct
{
	uint32_t time1,time2;
	uint32_t seq;
	uint16_t from;
	uint8_t prop;
	uint8_t  crc;
} PACKED TLogfileDTNMsg;


/**********************************************************/


typedef struct
{
	uint32_t time;
	uint32_t seq;
	uint16_t oid;
	uint8_t strength, crc;
} PACKED TLogfileBeaconPacket;
#endif/*__OPENBEACON_PROTO_H__*/
