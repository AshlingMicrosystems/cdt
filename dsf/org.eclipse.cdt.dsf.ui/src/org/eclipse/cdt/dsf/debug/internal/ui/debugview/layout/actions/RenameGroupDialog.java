package org.eclipse.cdt.dsf.debug.internal.ui.debugview.layout.actions;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class RenameGroupDialog extends Dialog {

	private GroupRenameInfo fInfo = null;

	private Text groupName;

	public RenameGroupDialog(Shell parentShell, GroupRenameInfo info) {
		super(parentShell);
		assert info != null;
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fInfo = info;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("Rename Group");
		Composite control = (Composite) super.createDialogArea(parent);
		Composite comp = new Composite(control, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		GridLayout layout = new GridLayout(3, false);
		comp.setLayout(layout);
		comp.setLayoutData(gd);
		groupName = new Text(comp, SWT.BORDER);
		groupName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return control;
	}

	@Override
	protected void okPressed() {
		fInfo.setGroupName(groupName.getText().trim());
		super.okPressed();
	}

}