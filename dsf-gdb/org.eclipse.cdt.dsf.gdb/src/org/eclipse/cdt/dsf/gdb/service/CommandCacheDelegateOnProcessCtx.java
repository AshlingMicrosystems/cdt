package org.eclipse.cdt.dsf.gdb.service;

import java.util.Hashtable;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.ICachingService;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IContainerDMContext;
import org.eclipse.cdt.dsf.debug.service.IRunControl.ISuspendedDMEvent;
import org.eclipse.cdt.dsf.debug.service.command.CommandCache;
import org.eclipse.cdt.dsf.debug.service.command.ICommand;
import org.eclipse.cdt.dsf.debug.service.command.ICommandResult;
import org.eclipse.cdt.dsf.debug.service.command.IEventListener;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.service.AbstractDsfService;
import org.eclipse.cdt.dsf.service.DsfServiceEventHandler;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.osgi.framework.BundleContext;

/**
 * This service could be used for executing commands with command cache at the process/container context.
 *
 */
public class CommandCacheDelegateOnProcessCtx extends AbstractDsfService
		implements ICommandCacheDelegateOnProcessCtx, ICachingService, IEventListener {

	private CommandCache fCommandCache;

	public CommandCacheDelegateOnProcessCtx(DsfSession session) {
		super(session);
	}

	@Override
	public void initialize(final RequestMonitor rm) {
		super.initialize(new RequestMonitor(getExecutor(), rm) {
			@Override
			protected void handleSuccess() {
				doInitialize(rm);
			}
		});
	}

	private void doInitialize(RequestMonitor rm) {
		// Register this service to DSF.
		// For completeness, use both the interface and the class name.
		register(new String[] { ICommandCacheDelegateOnProcessCtx.class.getName(), this.getClass().getName() },
				new Hashtable());
		IGDBControl gdbControl = getServicesTracker().getService(IGDBControl.class);
		fCommandCache = new CommandCache(getSession(), gdbControl);
		fCommandCache.setContextAvailable(gdbControl.getContext(), true);
		getSession().addServiceEventListener(this, null);
		rm.done();
	}

	@Override
	protected BundleContext getBundleContext() {
		return GdbPlugin.getBundleContext();
	}

	@Override
	public <V extends ICommandResult> void queueCommand(ICommand<V> command, DataRequestMonitor<V> rm) {
		DataRequestMonitor<V> drm = new DataRequestMonitor<>(getExecutor(), rm) {
			@Override
			protected void handleCompleted() {
				rm.setStatus(getStatus());
				rm.setData(getData());
				super.handleCompleted();
			}

		};
		fCommandCache.execute(command, drm);
	}

	@Override
	public void flushCache(IDMContext context) {
		fCommandCache.reset(context);
	}

	@Override
	public void eventReceived(Object output) {

	}

	@DsfServiceEventHandler
	public void eventDispatched(ISuspendedDMEvent event) {
		IContainerDMContext containerCtx = DMContexts.getAncestorOfType(event.getDMContext(),
				IContainerDMContext.class);
		if (containerCtx != null) {
			fCommandCache.reset(containerCtx);
		}
	}
}
