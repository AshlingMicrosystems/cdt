package org.eclipse.cdt.dsf.gdb.service;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.debug.service.command.ICommand;
import org.eclipse.cdt.dsf.debug.service.command.ICommandResult;
import org.eclipse.cdt.dsf.service.IDsfService;

/**
 * This service could be used for executing commands with command cache at the process/container context.
 *
 */
public interface ICommandCacheDelegateOnProcessCtx extends IDsfService {

	<V extends ICommandResult> void queueCommand(ICommand<V> command, DataRequestMonitor<V> rm);
}
