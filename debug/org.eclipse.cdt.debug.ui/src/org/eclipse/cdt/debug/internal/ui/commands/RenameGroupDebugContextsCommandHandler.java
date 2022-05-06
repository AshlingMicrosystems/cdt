package org.eclipse.cdt.debug.internal.ui.commands;

import org.eclipse.cdt.debug.core.model.IRenameDebugContextHandler;
import org.eclipse.debug.ui.actions.DebugCommandHandler;

public class RenameGroupDebugContextsCommandHandler extends DebugCommandHandler {

	@Override
	protected Class<?> getCommandType() {
		return IRenameDebugContextHandler.class;
	}
}
