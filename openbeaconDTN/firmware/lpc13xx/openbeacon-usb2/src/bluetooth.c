/***************************************************************
 *
 * OpenBeacon.org - Bluetooth related functions
 *
 * Copyright 2010 Milosch Meriac <meriac@openbeacon.de>
 *
 ***************************************************************

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
#include <openbeacon.h>
#include "bluetooth.h"
#ifdef  ENABLE_BLUETOOTH

#define CPU_WAKEUP_BLT_PORT 1
#define CPU_WAKEUP_BLT_PIN 3
#define CPU_BLT_WAKEUP_PORT 2
#define CPU_BLT_WAKEUP_PIN 0
#define CPU_ON_OFF_BLT_PORT 1
#define CPU_ON_OFF_BLT_PIN 0

static char bt_device_id_string[]="SLN=19,OpenBeacon Tag 0000";

static const char *bt_init_strings[] = {
  "SEC=3,2,2,04,0000",
  bt_device_id_string,
  "RLS=1101,13,Debug Console,01,000000",
  "DIS=3",
  "AAC=1",
  "SCR"
};

#define BT_INIT_STRINGS_COUNT ((int)(sizeof(bt_init_strings)/sizeof(bt_init_strings[0])))
#define BT_ID_POS (sizeof(bt_device_id_string)-5)

void
bt_init (uint8_t enabled, uint16_t device_id)
{
  int bt_init_pos = 0;

  /* Init UART for Bluetooth module without RTS/CTS */
  UARTInit (115200, 0);

  /* fake CTS for now */
  LPC_IOCON->PIO1_5 = 0;
  GPIOSetDir (1, 5, 1);
  GPIOSetValue (1, 5, 0);

  /* Set CPU_WAKEUP_BLT port pin to output */
  LPC_IOCON->ARM_SWDIO_PIO1_3 = 0x81;
  GPIOSetDir (CPU_WAKEUP_BLT_PORT, CPU_WAKEUP_BLT_PIN, 1);
  GPIOSetValue (CPU_WAKEUP_BLT_PORT, CPU_WAKEUP_BLT_PIN, enabled ? 1 : 0);

  /* Set CPU_BLT_WAKEUP port pin to input */
  LPC_IOCON->PIO2_0 = 0;
  GPIOSetDir (CPU_BLT_WAKEUP_PORT, CPU_BLT_WAKEUP_PIN, 0);
  GPIOSetValue (CPU_BLT_WAKEUP_PORT, CPU_BLT_WAKEUP_PIN, 0);

  /* Set CPU_ON-OFF_BLT port pin to output */
  LPC_IOCON->JTAG_TMS_PIO1_0 = 0x81;
  GPIOSetDir (CPU_ON_OFF_BLT_PORT, CPU_ON_OFF_BLT_PIN, 1);
  GPIOSetValue (CPU_ON_OFF_BLT_PORT, CPU_ON_OFF_BLT_PIN, enabled ? 1 : 0);

  /* update string device id */
  bt_device_id_string[BT_ID_POS+0] = hex_char ( device_id >> 12 );
  bt_device_id_string[BT_ID_POS+1] = hex_char ( device_id >>  8 );
  bt_device_id_string[BT_ID_POS+2] = hex_char ( device_id >>  4 );
  bt_device_id_string[BT_ID_POS+3] = hex_char ( device_id >>  0 );

  /* iterate through all bt_init_strings if activated */
  if (enabled)
    while (bt_init_pos <= BT_INIT_STRINGS_COUNT)
      {
	/* wait for CR */
	while (UARTCount)
	  if (UARTBuffer[--UARTCount] == '\n')
	    {
	      /* emmpty buffers */
	      UARTCount = 0;
	      /* output next init string */
	      if (bt_init_pos < BT_INIT_STRINGS_COUNT)
		debug_printf ("AT+J%s\n", bt_init_strings[bt_init_pos]);
	      bt_init_pos++;
	    }
      }
}
#endif/*ENABLE_BLUETOOTH*/
