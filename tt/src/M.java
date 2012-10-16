
public class M {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		/* accelerometer readings fifo */
		TFifoEntry acc_lowpass;
		TFifoEntry fifo_buf[FIFO_DEPTH];
		int fifo_pos;
		TFifoEntry *fifo;

		uint32_t SSPdiv, seq, oid;
		uint16_t crc, oid_last_seen;
		uint8_t flags, status;
		uint8_t cmd_buffer[64], cmd_pos, c;
		uint8_t volatile *uart;
		int x, y, z, firstrun_done, moving;
		volatile int t;
		int i;

		/* wait on boot - debounce */
		for (t = 0; t < 2000000; t++);

		/* Initialize GPIO (sets up clock) */
		GPIOInit ();

		/* initialize pins */
		pin_init ();

		/* fire up LED 1 */
		GPIOSetValue (1, 1, 1);

		/* initialize SPI */
		spi_init ();

		/* read device UUID */
		bzero (&device_uuid, sizeof (device_uuid));
		iap_read_uid (&device_uuid);
		tag_id = crc16 ((uint8_t *) & device_uuid, sizeof (device_uuid));
		random_seed =
				device_uuid[0] ^ device_uuid[1] ^ device_uuid[2] ^ device_uuid[3];

		/************ IF Plugged to computer upon reset ? ******************/
		if (GPIOGetValue (0, 3))
		{
			/* wait some time till Bluetooth is off */
			for (t = 0; t < 2000000; t++);

			/* Init 3D acceleration sensor */
			acc_init (1);
			/* Init Flash Storage with USB */
			storage_init (TRUE, tag_id);
			g_storage_items = storage_items ();

			/* Init Bluetooth */
			bt_init (TRUE, tag_id);

			/* switch to LED 2 */
			GPIOSetValue (1, 1, 0);
			GPIOSetValue (1, 2, 1);

			/* set command buffer to empty */
			cmd_pos = 0;

			/* spin in loop */
			while (1)
			{
				/* reset after USB unplug */
				if (!GPIOGetValue (0, 3))
					NVIC_SystemReset ();

				/* if UART rx send to menue */
				if (UARTCount)
				{
					/* blink LED1 upon Bluetooth command */
					GPIOSetValue (1, 1, 1);
					/* execute menue command with last character received */

					/* scan through whole UART buffer */
					uart = UARTBuffer;
					for (i = UARTCount; i > 0; i--)
					{
						UARTCount--;
						c = *uart++;
						if ((c < ' ') && cmd_pos)
						{
							/* if one-character command - execute */
							if (cmd_pos == 1)
								main_menue (cmd_buffer[0]);
							else
							{
								cmd_buffer[cmd_pos] = 0;
								debug_printf
								("Unknown command '%s' - please press H+[Enter] for help\n# ",
										cmd_buffer);
							}

							/* set command buffer to empty */
							cmd_pos = 0;
						}
						else if (cmd_pos < (sizeof (cmd_buffer) - 2))
							cmd_buffer[cmd_pos++] = c;
					}

					/* reset UART buffer */
					UARTCount = 0;
					/* un-blink LED1 */
					GPIOSetValue (1, 1, 0);
				}
			}
		} /* End of if plugged to computer*/


		/***************** IF UNPLUGGED TO PC ........********/

		/* Init Bluetooth */
		bt_init (FALSE, tag_id);

		/* shut down up LED 1 */
		GPIOSetValue (1, 1, 0);

		/* Init Flash Storage without USB */
		storage_init (FALSE, tag_id);

		/* get current FLASH storage write postition */
		g_storage_items = storage_items ();

		/* initialize power management */
		pmu_init ();

		/* blink once to show initialized flash */
		blink (1);

		/* Init 3D acceleration sensor */
		acc_init (0);
		blink (2);

		/* Initialize OpenBeacon nRF24L01 interface */
		if (!nRFAPI_Init(CONFIG_TRACKER_CHANNEL, broadcast_mac, sizeof (broadcast_mac), 0))
			for (;;)
			{
				GPIOSetValue (1, 2, 1);
				pmu_sleep_ms (500);
				GPIOSetValue (1, 2, 0);
				pmu_sleep_ms (500);
			}
		/* set tx power power to high */
		nRFCMD_Power (1);

		/* blink three times to show flash initialized RF interface */
		blink (3);

		/* blink LED for 1s to show readyness */
		GPIOSetValue (1, 1, 0);
		GPIOSetValue (1, 2, 1);
		pmu_sleep_ms (1000);
		GPIOSetValue (1, 2, 0);

		/* disable unused jobs */
		SSPdiv = LPC_SYSCON->SSPCLKDIV;
		i = 0;
		oid_last_seen = 0;

		/* reset proximity buffer */
		prox_head = prox_tail = 0;
		bzero (&prox, sizeof (prox));

		/*initialize FIFO */
		fifo_pos = 0;
		bzero (&acc_lowpass, sizeof (acc_lowpass));
		bzero (&fifo_buf, sizeof (fifo_buf));
		firstrun_done = 0;
		moving = 0;
		g_sequence = 0;

		while (1)
		{
			
			pmu_sleep_ms (500);

			LPC_SYSCON->SSPCLKDIV = SSPdiv;
			acc_power (1);
			pmu_sleep_ms (20);
			acc_xyz_read (&x, &y, &z);
			acc_power (0);

			fifo = &fifo_buf[fifo_pos];
			if (fifo_pos >= (FIFO_DEPTH - 1))
				fifo_pos = 0;
			else
				fifo_pos++;

			acc_lowpass.x += x - fifo->x;
			fifo->x = x;
			acc_lowpass.y += y - fifo->y;
			fifo->y = y;
			acc_lowpass.z += z - fifo->z;
			fifo->z = z;

			/******** when moving and there is incomming packet recieve it *******/
			nRFAPI_SetRxMode (0);

			bzero (&g_Beacon, sizeof (g_Beacon));
			g_Beacon.pkt.proto = RFBPROTO_BEACONTRACKER_EXT;
			g_Beacon.pkt.flags = moving ? RFBFLAGS_MOVING : 0;
			g_Beacon.pkt.oid = htons (tag_id);
			g_Beacon.pkt.p.tracker.strength = (i & 1) + TX_STRENGTH_OFFSET;
			g_Beacon.pkt.p.tracker.seq = htonl (LPC_TMR32B0->TC);
			g_Beacon.pkt.p.tracker.oid_last_seen = oid_last_seen;
			g_Beacon.pkt.p.tracker.time = htons ((uint16_t)g_sequence++);
			g_Beacon.pkt.p.tracker.battery = 0;
			g_Beacon.pkt.crc = htons (
					crc16(g_Beacon.byte, sizeof (g_Beacon) - sizeof (g_Beacon.pkt.crc))
					);

			nRFCMD_Power (0);
			nRF_tx (g_Beacon.pkt.p.tracker.strength);
			nRFCMD_Power (1);
			nRFAPI_PowerDown ();
			LPC_SYSCON->SSPCLKDIV = 0x00;
		}
		

	}
	return 0;



}		




}
