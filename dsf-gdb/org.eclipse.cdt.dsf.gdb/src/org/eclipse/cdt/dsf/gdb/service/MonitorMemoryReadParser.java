/*******************************************************************************
 * Copyright (c) 2026 Ashling and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ashling - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.dsf.gdb.service;

import org.eclipse.cdt.dsf.mi.service.command.output.MIConsoleStreamOutput;
import org.eclipse.cdt.dsf.mi.service.command.output.MIOOBRecord;
import org.eclipse.cdt.dsf.mi.service.command.output.MITargetStreamOutput;
import org.eclipse.debug.core.model.MemoryByte;

/**
 * Parses the raw string output from a GDB {@code monitor memread} command and
 * converts it into a {@code MemoryByte[]} array suitable for the Memory View.
 *
 * <p>
 * Expected input format (one 32-bit word per line):
 * <pre>
 * 0x158c0000: 0x00000000
 * 0x158c0004: 0x12345678
 * 0x158c0008: 0xDEADBEEF
 * </pre>
 * Each line starts with a hex address followed by a colon, then a single
 * hex word value (with {@code 0x} prefix). Each word is 4 bytes and is
 * extracted in the byte order matching the target endianness: little-endian
 * targets store the least significant byte at the lowest address, while
 * big-endian targets store the most significant byte at the lowest address.
 * </p>
 *
 * <p>
 * If parsing fails, an "invalid" block (all flags cleared) is returned so the
 * Memory View can indicate the error gracefully.
 * </p>
 *
 * @since 6.6
 */
public class MonitorMemoryReadParser {

	/** Base flag bits for a valid, readable, writable byte (endianness flags added at parse time) */
	private static final byte BASE_VALID_FLAGS = (byte) (MemoryByte.READABLE | MemoryByte.WRITABLE | MemoryByte.ENDIANESS_KNOWN);

	/** Number of bytes in each 32-bit word returned by the monitor command */
	public static final int BYTES_PER_WORD = 4;

	/**
	 * Parses the raw monitor command output into a MemoryByte array.
	 *
	 * <p>Each line of the output is expected to be in the format:
	 * {@code 0xADDRESS: 0xVALUE} where VALUE is a 32-bit hex word.
	 * The word value is split into 4 bytes in the order determined by
	 * {@code bigEndian}: little-endian extracts LSB first (byte at lowest
	 * address = least significant), big-endian extracts MSB first (byte at
	 * lowest address = most significant).
	 *
	 * @param rawOutput
	 *            the raw string output from the monitor command; may contain
	 *            multiple lines
	 * @param expectedBytes
	 *            the number of bytes that were requested; used as the result
	 *            array size
	 * @param bigEndian
	 *            {@code true} if the target is big-endian; {@code false} for
	 *            little-endian
	 * @return a MemoryByte array of size {@code expectedBytes}, populated with
	 *         parsed data where available
	 */
	public static MemoryByte[] parse(String rawOutput, int expectedBytes, boolean bigEndian) {
		if (rawOutput == null || rawOutput.trim().isEmpty()) {
			return createInvalidBlock(expectedBytes);
		}

		// Build the per-byte flags: base flags + BIG_ENDIAN when appropriate
		byte validFlags = bigEndian
				? (byte) (BASE_VALID_FLAGS | MemoryByte.BIG_ENDIAN)
				: BASE_VALID_FLAGS;

		try {
			String[] lines = rawOutput.split("\\r?\\n"); //$NON-NLS-1$
			byte[] data = new byte[expectedBytes];
			int dataIndex = 0;

			for (String line : lines) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}

				// Each line: "0x158c0000: 0x00000000"
				// Split on the colon that separates address from value
				int colonIndex = line.indexOf(':');
				if (colonIndex < 0) {
					continue;
				}

				String valueStr = line.substring(colonIndex + 1).trim();
				if (valueStr.isEmpty()) {
					continue;
				}

				// Remove the "0x" or "0X" prefix if present
				if (valueStr.startsWith("0x") || valueStr.startsWith("0X")) { //$NON-NLS-1$ //$NON-NLS-2$
					valueStr = valueStr.substring(2);
				}

				// Parse the word value as a 32-bit unsigned integer
				long wordValue = Long.parseUnsignedLong(valueStr, 16);

				// Extract 4 bytes per word in the correct byte order.
				// Little-endian: LSB at lowest address → shift by 0, 8, 16, 24
				// Big-endian:    MSB at lowest address → shift by 24, 16, 8, 0
				for (int i = 0; i < BYTES_PER_WORD; i++) {
					if (dataIndex < data.length) {
						int shift = bigEndian
								? ((BYTES_PER_WORD - 1 - i) * 8)
								: (i * 8);
						data[dataIndex] = (byte) ((wordValue >>> shift) & 0xFF);
						dataIndex++;
					}
				}
			}

			MemoryByte[] result = new MemoryByte[expectedBytes];
			for (int i = 0; i < expectedBytes; i++) {
				if (i < dataIndex) {
					result[i] = new MemoryByte(data[i], validFlags);
				} else {
					result[i] = new MemoryByte((byte) 0, (byte) 0);
				}
			}
			return result;

		} catch (Exception e) {
			return createInvalidBlock(expectedBytes);
		}
	}

	/**
	 * Extracts the output text from MI OOB records by collecting all
	 * {@code MIConsoleStreamOutput} and {@code MITargetStreamOutput} records.
	 * GDB {@code monitor} commands may send their output via the target stream
	 * ({@code @} prefix) rather than the console stream ({@code ~} prefix),
	 * depending on the GDB server implementation.
	 *
	 * @param oobRecords
	 *            the OOB records from the MI output
	 * @return the concatenated stream text
	 */
	public static String extractStreamOutput(MIOOBRecord[] oobRecords) {
		if (oobRecords == null) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder sb = new StringBuilder();
		for (MIOOBRecord record : oobRecords) {
			if (record instanceof MIConsoleStreamOutput) {
				sb.append(((MIConsoleStreamOutput) record).getString());
			} else if (record instanceof MITargetStreamOutput) {
				sb.append(((MITargetStreamOutput) record).getString());
			}
		}
		return sb.toString();
	}

	/**
	 * Creates a block of invalid (unreadable) MemoryByte entries.
	 *
	 * @param size the number of bytes in the block
	 * @return MemoryByte array with all flags cleared
	 */
	public static MemoryByte[] createInvalidBlock(int size) {
		MemoryByte[] block = new MemoryByte[size];
		for (int i = 0; i < block.length; i++) {
			block[i] = new MemoryByte((byte) 0, (byte) 0);
		}
		return block;
	}

}
