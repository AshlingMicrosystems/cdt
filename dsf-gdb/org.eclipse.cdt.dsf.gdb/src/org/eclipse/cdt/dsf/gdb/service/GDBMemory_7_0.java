/*******************************************************************************
 * Copyright (c) 2008, 2014 Ericsson and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ericsson - initial API and implementation
 *     Alvaro Sanchez-Leon (Ericsson AB) - [Memory] Support 16 bit addressable size (Bug 426730)
 *******************************************************************************/
package org.eclipse.cdt.dsf.gdb.service;

import java.util.Hashtable;

import org.eclipse.cdt.core.IAddress;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.ImmediateRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IMemory;
import org.eclipse.cdt.dsf.debug.service.IMemory.IMemoryDMContext;
import org.eclipse.cdt.dsf.debug.service.IMemoryAccessTypeDMContext;
import org.eclipse.cdt.dsf.debug.service.IMemorySpaces.IMemorySpaceDMContext;
import org.eclipse.cdt.dsf.debug.service.IRunControl;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlDMContext;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.cdt.dsf.gdb.internal.memory.GdbMemoryBlock.MemorySpaceDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIContainerDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.MIMemory;
import org.eclipse.cdt.dsf.mi.service.command.commands.CLICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.cdt.dsf.mi.service.command.output.MIOOBRecord;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.model.MemoryByte;

public class GDBMemory_7_0 extends GDBMemory {

	public GDBMemory_7_0(DsfSession session) {
		super(session);
	}

	@Override
	public void initialize(final RequestMonitor requestMonitor) {
		super.initialize(new ImmediateRequestMonitor(requestMonitor) {
			@Override
			public void handleSuccess() {
				doInitialize(requestMonitor);
			}
		});
	}

	private void doInitialize(final RequestMonitor requestMonitor) {
		register(
				new String[] { MIMemory.class.getName(), IMemory.class.getName(), IGDBMemory.class.getName(),
						IGDBMemory2.class.getName(), GDBMemory.class.getName(), GDBMemory_7_0.class.getName() },
				new Hashtable<>());

		requestMonitor.done();
	}

	@Override
	public void shutdown(final RequestMonitor requestMonitor) {
		unregister();
		super.shutdown(requestMonitor);
	}

	@Override
	protected void readMemoryBlock(IDMContext dmc, IAddress address, long offset, int word_size, int word_count,
			DataRequestMonitor<MemoryByte[]> drm) {

		// Check for a custom memory access type (e.g., APB, AXI).
		// When present and non-default, send a monitor memread command instead
		// of the standard -data-read-memory-bytes MI command.
		IMemoryAccessTypeDMContext accessTypeDmc = getAccessTypeDMContext(dmc);
		if (accessTypeDmc != null) {
			String accessTypeId = accessTypeDmc.getAccessTypeId();
			if (accessTypeId != null) { //$NON-NLS-1$
				// Compute the actual target address from the IAddress + offset.
				// The userExpression is the original address the user typed, but
				// the Memory View may request data at different addresses (scrolling,
				// cache misses, etc.), so we must use the real address here.
				String hexAddress = "0x" + address.add(offset).toString(16); //$NON-NLS-1$
				IMemoryDMContext memDmc = DMContexts.getAncestorOfType(dmc, IMemoryDMContext.class);
				boolean bigEndian = memDmc != null && isBigEndian(memDmc);
				readMemoryViaMonitor(accessTypeId, hexAddress, word_size, word_count, bigEndian, drm);
				return;
			}
		}

		IDMContext threadOrMemoryDmc = dmc;

		// A memory context is a container.  We have two limitations with GDB here:
		// 1- Before GDB 7.2, there is no way to specify a process for an MI command, so to work around
		// that, we need to specify a thread for the process we want to point to.  This is important
		// to support multi-process for targets that have that kind of support before GDB 7.2
		// 2- GDB cannot read memory when pointing to a thread that is running.  For non-stop mode
		// we can have some threads running with others stopped, so we need to choose a thread that is
		// actually stopped.
		IMIContainerDMContext containerCtx = DMContexts.getAncestorOfType(dmc, IMIContainerDMContext.class);
		if (containerCtx != null) {
			IGDBProcesses procService = getServicesTracker().getService(IGDBProcesses.class);
			IRunControl runControl = getServicesTracker().getService(IRunControl.class);

			if (procService != null && runControl != null) {
				IMIExecutionDMContext[] execCtxs = procService.getExecutionContexts(containerCtx);
				// Return any thread, as long as it is suspended.  This will allow GDB to read the memory
				// and it will be for the process we care about (since we choose a thread within it).
				if (execCtxs != null && execCtxs.length > 0) {
					for (IMIExecutionDMContext execCtx : execCtxs) {
						if (runControl.isSuspended(execCtx)) {
							threadOrMemoryDmc = execCtx;

							// Not so fast, Charlie. The context we were given may have
							// a memory space qualifier. We need to preserve it.
							if (dmc instanceof IMemorySpaceDMContext) {
								threadOrMemoryDmc = new MemorySpaceDMContext(getSession().getId(),
										((IMemorySpaceDMContext) dmc).getMemorySpaceId(), threadOrMemoryDmc);
							}
							break;
						}
					}
				}
			}
		}

		super.readMemoryBlock(threadOrMemoryDmc, address, offset, word_size, word_count, drm);
	}

	@Override
	protected void writeMemoryBlock(IDMContext dmc, IAddress address, long offset, int word_size, int word_count,
			byte[] buffer, RequestMonitor rm) {

		// Check for a custom memory access type (e.g., APB, AXI).
		// When present and non-default, send a monitor memwrite command instead
		// of the standard -data-write-memory-bytes MI command.
		IMemoryAccessTypeDMContext accessTypeDmc = getAccessTypeDMContext(dmc);
		if (accessTypeDmc != null) {
			String accessTypeId = accessTypeDmc.getAccessTypeId();
			if (accessTypeId != null) { //$NON-NLS-1$
				// Compute the actual target address from the IAddress + offset.
				// We must NOT use the original userExpression here because the
				// Memory View may write to any cell, not just the base address.
				String hexAddress = "0x" + address.add(offset).toString(16); //$NON-NLS-1$
				// Determine byte order so we can construct the word value correctly.
				IMemoryDMContext memDmc = DMContexts.getAncestorOfType(dmc, IMemoryDMContext.class);
				boolean bigEndian = memDmc != null && isBigEndian(memDmc);
				// The buffer contains bytes in address order. The monitor memwrite
				// command expects a 32-bit word value (MSB-first hex string).
				// Little-endian: buffer has LSB at lowest index → reverse each
				//   4-byte group to build MSB-first hex.
				// Big-endian: buffer already has MSB at lowest index → emit in
				//   forward order.
				int totalBytes = word_size * word_count;
				StringBuilder hexData = new StringBuilder();
				for (int i = 0; i < totalBytes && i < buffer.length; i += MonitorMemoryReadParser.BYTES_PER_WORD) {
					int end = Math.min(i + MonitorMemoryReadParser.BYTES_PER_WORD, Math.min(totalBytes, buffer.length));
					if (bigEndian) {
						for (int j = i; j < end; j++) {
							hexData.append(String.format("%02x", buffer[j])); //$NON-NLS-1$
						}
					} else {
						for (int j = end - 1; j >= i; j--) {
							hexData.append(String.format("%02x", buffer[j])); //$NON-NLS-1$
						}
					}
				}
				writeMemoryViaMonitor(accessTypeId, hexAddress, hexData.toString(), rm);
				return;
			}
		}

		IDMContext threadOrMemoryDmc = dmc;

		// A memory context is a container.  We have two limitations with GDB here:
		// 1- Before GDB 7.2, there is no way to specify a process for an MI command, so to work around
		// that, we need to specify a thread for the process we want to point to.  This is important
		// to support multi-process for targets that have that kind of support before GDB 7.2
		// 2- GDB cannot write memory when pointing to a thread that is running.  For non-stop mode
		// we can have some threads running with others stopped, so we need to choose a thread that is
		// actually stopped.
		IMIContainerDMContext containerCtx = DMContexts.getAncestorOfType(dmc, IMIContainerDMContext.class);
		if (containerCtx != null) {
			IGDBProcesses procService = getServicesTracker().getService(IGDBProcesses.class);
			IRunControl runControl = getServicesTracker().getService(IRunControl.class);

			if (procService != null && runControl != null) {
				IMIExecutionDMContext[] execCtxs = procService.getExecutionContexts(containerCtx);
				// Return any thread, as long as it is suspended.  This will allow GDB to read the memory
				// and it will be for the process we care about (since we choose a thread within it).
				if (execCtxs != null && execCtxs.length > 0) {
					for (IMIExecutionDMContext execCtx : execCtxs) {
						if (runControl.isSuspended(execCtx)) {
							threadOrMemoryDmc = execCtx;

							// Not so fast, Charlie. The context we were given may have
							// a memory space qualifier. We need to preserve it.
							if (dmc instanceof IMemorySpaceDMContext) {
								threadOrMemoryDmc = new MemorySpaceDMContext(getSession().getId(),
										((IMemorySpaceDMContext) dmc).getMemorySpaceId(), threadOrMemoryDmc);
							}
							break;
						}
					}
				}
			}
		}

		super.writeMemoryBlock(threadOrMemoryDmc, address, offset, word_size, word_count, buffer, rm);
	}

	// ========================================================================
	// Memory access type support — monitor command helpers
	// ========================================================================

	/**
	 * Extracts an {@link IMemoryAccessTypeDMContext} from the given context, if present.
	 */
	protected IMemoryAccessTypeDMContext getAccessTypeDMContext(IDMContext dmc) {
		if (dmc instanceof IMemoryAccessTypeDMContext) {
			return (IMemoryAccessTypeDMContext) dmc;
		}
		return DMContexts.getAncestorOfType(dmc, IMemoryAccessTypeDMContext.class);
	}

	/**
	 * Reads memory using a custom monitor memread command, parses the stream
	 * output, and returns MemoryByte[] via the data request monitor.
	 *
	 * <p>The command format is:
	 * {@code monitor memread <apb|axi>:<userExpression> <word_count>}
	 * where {@code userExpression} is the exact address string the user typed in the UI,
	 * and {@code word_count} is the number of 32-bit words to read.
	 *
	 * @param accessTypeId the access type (e.g., "APB", "AXI")
	 * @param userExpression the original address/expression exactly as the user typed it
	 * @param wordSize the size of each addressable unit in bytes (from DSF)
	 * @param wordCount the number of addressable units to read (from DSF)
	 * @param bigEndian {@code true} if the target is big-endian
	 * @param drm the data request monitor to return the result
	 */
	protected void readMemoryViaMonitor(String accessTypeId, String userExpression, int wordSize, int wordCount,
			boolean bigEndian, DataRequestMonitor<MemoryByte[]> drm) {
		// totalBytes is the number of bytes the Memory View expects
		int totalBytes = wordSize * wordCount;

		// The monitor command expects the count in 32-bit words, not bytes.
		// Each word returned = 4 bytes. Round up to ensure we get enough data.
		int monitorWordCount = (totalBytes + MonitorMemoryReadParser.BYTES_PER_WORD - 1)
				/ MonitorMemoryReadParser.BYTES_PER_WORD;

		ICommandControlService commandControl = getServicesTracker().getService(ICommandControlService.class);
		if (commandControl == null) {
			drm.setData(MonitorMemoryReadParser.createInvalidBlock(totalBytes));
			drm.done();
			return;
		}

		ICommandControlDMContext controlDmc = commandControl.getContext();
		// Build: monitor memread <apb|axi>:<userExpression> <monitorWordCount>
		// The userExpression is sent verbatim — exactly as the user typed it.
		// monitorWordCount = number of 32-bit words (not bytes).
		String command = "monitor memread " + accessTypeId //$NON-NLS-1$
				+ ":" + userExpression + " " + monitorWordCount; //$NON-NLS-1$ //$NON-NLS-2$

		commandControl.queueCommand(new CLICommand<>(controlDmc, command),
				new DataRequestMonitor<MIInfo>(getExecutor(), drm) {
					@Override
					protected void handleSuccess() {
						MIOOBRecord[] oobRecords = getData().getMIOutput().getMIOOBRecords();
						String rawOutput = MonitorMemoryReadParser.extractStreamOutput(oobRecords);
						MemoryByte[] memoryBytes = MonitorMemoryReadParser.parse(rawOutput, totalBytes, bigEndian);
						drm.setData(memoryBytes);
						drm.done();
					}

					@Override
					protected void handleFailure() {
						drm.setData(MonitorMemoryReadParser.createInvalidBlock(totalBytes));
						drm.done();
					}
				});
	}

	/**
	 * Writes memory using a custom monitor memwrite command.
	 *
	 * <p>The command format is:
	 * {@code monitor memwrite <APB|AXI>:<address> <updated_value>}
	 *
	 * @param accessTypeId the access type (e.g., "APB", "AXI")
	 * @param address the hex address to write to (e.g., "0x15c10030")
	 * @param updatedValue the new value as a hex string (e.g., "f4871269")
	 * @param rm the request monitor
	 */
	protected void writeMemoryViaMonitor(String accessTypeId, String address, String updatedValue, RequestMonitor rm) {
		ICommandControlService commandControl = getServicesTracker().getService(ICommandControlService.class);
		if (commandControl == null) {
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, "Command control service not available for memory write.")); //$NON-NLS-1$
			rm.done();
			return;
		}

		ICommandControlDMContext controlDmc = commandControl.getContext();
		// Build: monitor memwrite <APB|AXI>:<address> <updated_value>
		String command = "monitor memwrite " + accessTypeId //$NON-NLS-1$
				+ ":" + address + " 0x" + updatedValue.toUpperCase(); //$NON-NLS-1$ //$NON-NLS-2$

		commandControl.queueCommand(new CLICommand<>(controlDmc, command),
				new DataRequestMonitor<MIInfo>(getExecutor(), rm));
	}
}
