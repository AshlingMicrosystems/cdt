package org.eclipse.cdt.dsf.debug.internal.ui.debugview.layout;

import org.eclipse.cdt.dsf.debug.ui.IDsfDebugUIConstants;
import org.eclipse.cdt.dsf.internal.ui.DsfUIPlugin;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.dsf.ui.viewmodel.datamodel.IDMVMContext;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Platform;

/**
 * Property tester for debug view related commands - Rename Group.
 *
 * @since 2.2
 */

public class RenameGroupPropertyTester extends PropertyTester {

	public RenameGroupPropertyTester() {
	}

	protected static final String IS_RENAME_GROUP_VISIBLE = "isRenameGroupDebugContextsVisible"; //$NON-NLS-1$

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		boolean showGroupCommands = Platform.getPreferencesService().getBoolean(DsfUIPlugin.PLUGIN_ID,
				IDsfDebugUIConstants.PREF_GROUP_CMDS_ENABLE, false, null);
		if (showGroupCommands && (IS_RENAME_GROUP_VISIBLE.equals(property))) {
			if (receiver instanceof IDMVMContext) {
				return test((IDMVMContext) receiver);
			}
		}
		return false;
	}

	private boolean test(IDMVMContext dmContext) {
		String sessionId = dmContext.getDMContext().getSessionId();
		return DsfSession.isSessionActive(sessionId);
	}
}
