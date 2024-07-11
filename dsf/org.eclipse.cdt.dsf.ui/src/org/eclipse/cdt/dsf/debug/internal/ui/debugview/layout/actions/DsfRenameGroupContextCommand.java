package org.eclipse.cdt.dsf.debug.internal.ui.debugview.layout.actions;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DsfRunnable;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.debug.internal.provisional.service.IExecutionContextTranslator;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;

public class DsfRenameGroupContextCommand extends DsfDebugViewLayoutCommand {

	private class PromptJob extends UIJob {

		private DataRequestMonitor<GroupRenameInfo> fRequestMonitor;

		private PromptJob(DataRequestMonitor<GroupRenameInfo> rm) {
			super("Renaming Group");
			fRequestMonitor = rm;
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			final GroupRenameInfo info = new GroupRenameInfo();
			RenameGroupDialog dialog = new RenameGroupDialog(Display.getDefault().getActiveShell(), info);
			final boolean canceled = dialog.open() == Window.CANCEL;
			fExecutor.execute(new DsfRunnable() {

				@Override
				public void run() {
					if (canceled)
						fRequestMonitor.cancel();
					else
						fRequestMonitor.setData(info);
					fRequestMonitor.done();
				}
			});
			return Status.OK_STATUS;
		}
	}

	public DsfRenameGroupContextCommand(DsfSession session) {
		super(session);
	}

	@Override
	void executeOnDsfThread(IExecutionContextTranslator translator, IExecutionDMContext[] contexts, RequestMonitor rm) {
		if (contexts.length > 0) {
			PromptJob job = new PromptJob(new DataRequestMonitor<GroupRenameInfo>(fExecutor, rm) {
				@Override
				protected void handleCancel() {
					rm.cancel();
					rm.done();
				}

				@Override
				protected void handleSuccess() {
					translator.rename(contexts[0], getData().getGroupName(), rm);
					rm.done();
				}
			});
			job.schedule();
			return;
		}
		rm.done();
	}

	@Override
	void canExecuteOnDsfThread(IExecutionContextTranslator translator, IExecutionDMContext[] contexts,
			DataRequestMonitor<Boolean> rm) {
		if (contexts.length > 0) {
			// send the first context for rename check
			translator.canRename(contexts[0], rm);
			return;
		}
		rm.done(false);
	}
}
