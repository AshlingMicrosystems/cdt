/*******************************************************************************
 * Copyright (c) 2010, 2012 Broadcom Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alex Collins (Broadcom Corp.) - Initial implementation
 *     ASHLING - Added support for variables button
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.preferences;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.cdt.internal.ui.buildconsole.BuildConsoleManager;
import org.eclipse.cdt.internal.ui.buildconsole.GlobalBuildConsoleManager;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.ListDialog;

//<CUSTOMISATION> ASHLING - Added support for variables button, git-lab#201, JIRA-SOCVE-155
/**
 * Preference page for build logging options, such as whether the
 * global build console should be logged and, if so, where.
 */
public class GlobalBuildLogPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	BooleanFieldEditor keepLog;
	FilePathEditor logLocation;

	public GlobalBuildLogPreferencePage() {
		super(GRID);
		setPreferenceStore(GlobalBuildConsoleManager.getBuildLogPreferenceStore());
	}

	/**
	 * A file path field with choose button that does not require the chosen file to exist.
	 */
	static private class FilePathEditor extends StringButtonFieldEditor {
		//<CUSTOMISATION> ASHLING
		Button variablesButton;
		GlobalBuildLogPreferencePage preferancePage;
		String variableButtonText;
		//</CUSTOMISATION>

		public FilePathEditor(GlobalBuildLogPreferencePage preferancePage, String name, String label,
				Composite parent) {
			super(name, label, parent);
			this.preferancePage = preferancePage;
		}

		@Override
		protected String changePressed() {
			FileDialog dialog = new FileDialog(getShell(), SWT.NONE);
			dialog.setText(getLabelText());
			String fileName = super.oldValue;
			IPath logFolder = new Path(fileName).removeLastSegments(1);
			dialog.setFilterPath(logFolder.toOSString());
			return dialog.open();
		}

		//<CUSTOMISATION> ASHLING
		@Override
		protected void doFillIntoGrid(Composite parent, int numColumns) {
			super.doFillIntoGrid(parent, numColumns);
			variablesButton = getVariableControl(parent);
			GridData gd = new GridData();
			gd.horizontalAlignment = GridData.FILL;
			int widthHint = convertHorizontalDLUsToPixels(variablesButton, IDialogConstants.BUTTON_WIDTH);
			gd.widthHint = Math.max(widthHint, variablesButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			variablesButton.setLayoutData(gd);
		}

		private Button getVariableControl(Composite parent) {

			if (variablesButton == null) {
				variablesButton = new Button(parent, SWT.PUSH);
				if (variableButtonText == null) {
					variableButtonText = "Variables..."; //$NON-NLS-1$
				}
				variablesButton.setText(variableButtonText);
				variablesButton.setFont(parent.getFont());
				variablesButton.addSelectionListener(widgetSelectedAdapter(evt -> {
					preferancePage.variablesButtonSelected(preferancePage.logLocation.getTextControl());
				}));
				variablesButton.addDisposeListener(event -> variablesButton = null);
			} else {
				checkParent(variablesButton, parent);
			}
			return variablesButton;
		}

		@Override
		public int getNumberOfControls() {
			return 4;
		}

		@Override
		public void setEnabled(boolean enabled, Composite parent) {
			super.setEnabled(enabled, parent);
			if (null != variablesButton) {
				variablesButton.setEnabled(enabled);
			}
		}

		@Override
		protected void adjustForNumColumns(int numColumns) {
			((GridData) getTextControl().getLayoutData()).horizontalSpan = numColumns - 3;
		}
		//</CUSTOMISATION>
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		keepLog = new BooleanFieldEditor(BuildConsoleManager.KEY_KEEP_LOG,
				PreferencesMessages.GlobalBuildLogPreferencePage_EnableLogging, parent);
		addField(keepLog);
		logLocation = new FilePathEditor(this, BuildConsoleManager.KEY_LOG_LOCATION,
				PreferencesMessages.GlobalBuildLogPreferencePage_LogLocation, parent);
		addField(logLocation);
	}

	@Override
	public void init(IWorkbench workbench) {
		initDefaults(GlobalBuildConsoleManager.getBuildLogPreferenceStore());
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		if (event.getProperty().equals(FieldEditor.VALUE) && event.getNewValue() instanceof Boolean) {
			logLocation.setEnabled(((Boolean) event.getNewValue()).booleanValue(), getFieldEditorParent());
		}
	}

	public static void initDefaults(IPreferenceStore prefs) {
		prefs.setDefault(BuildConsoleManager.KEY_KEEP_LOG, BuildConsoleManager.CONSOLE_KEEP_LOG_DEFAULT);
		prefs.setDefault(BuildConsoleManager.KEY_LOG_LOCATION,
				GlobalBuildConsoleManager.getDefaultConsoleLogLocation());
	}

	//<CUSTOMISATION> ASHLING
	private void variablesButtonSelected(Text text) {
		String[] variableList = { "eclipse_home", "workspace_loc" }; //$NON-NLS-1$ //$NON-NLS-2$
		ListDialog variableDialog = new ListDialog(getShell());
		variableDialog.setAddCancelButton(true);
		variableDialog.setContentProvider(new ArrayContentProvider());
		variableDialog.setLabelProvider(new LabelProvider());
		variableDialog.setInput(variableList);
		variableDialog.setTitle("Choose Variable"); //$NON-NLS-1$
		variableDialog.setInitialSelections(variableList[0]);
		if (variableDialog.open() == ListDialog.OK) {
			// Using [0] as only one selection is allowed,getResult() returns array of selected items.
			text.insert("${" + variableDialog.getResult()[0] + "}");//$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	//</CUSTOMISATION>
}
