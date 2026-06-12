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

package org.eclipse.cdt.debug.internal.ui.views.memory;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog CDT puts up when adding a memory monitor to the memory view for a
 * debug target that supports memory access types (e.g., APB, AXI).
 * <p>
 * It differs from the platform one in that you can enter an expression or
 * an address + memory access type pair.
 * <p>
 * This dialog is independent of {@link AddMemoryBlockDialog} (which handles
 * memory spaces). It follows the same UI pattern but for access types.
 */
public class AddMemoryAccessTypeBlockDialog extends TrayDialog implements ModifyListener, SelectionListener {

	private Combo fAddressInput;
	private Button fAddressRadio;
	private Combo fAccessTypeInput;
	private Combo fExpressionInput;
	private String fExpression;
	private Button fExpressionRadio;
	private String fAddress;
	private String fAccessType;
	private boolean fEnteredExpression; // which of the two radio buttons was selected when OK was hit
	// TODO: Re-enable secure/non-secure access when needed
	// private Button fSecureAccessRadio;
	// private Button fNonSecureAccessRadio;
	// private boolean fSecureAccess = true; // default to Secure access

	/** The memory access types to expose. Given to us at instantiation time. */
	final private String[] fAccessTypes;

	/** Tooltips for each access type (parallel to {@link #fAccessTypes}). */
	final private String[] fAccessTypeTooltips;

	/**
	 * For improved usability, we persist the access type selection from one
	 * invocation of the dialog to another, but we need not worry about
	 * persisting it from one instantiation of Eclipse to the next
	 */
	private static String fPreviousAccessTypeSelection;

	private static List<String> sAddressHistory = new ArrayList<>();
	private static List<String> sExpressionHistory = new ArrayList<>();

	private static boolean sDefaultToExpression = true;

	public AddMemoryAccessTypeBlockDialog(Shell parentShell, String[] accessTypes, String[] tooltips) {
		super(parentShell);

		setShellStyle(getShellStyle() | SWT.RESIZE);
		fAccessTypes = accessTypes;
		fAccessTypeTooltips = (tooltips != null && tooltips.length == accessTypes.length)
				? tooltips : new String[0];

		// We shouldn't be using this custom dialog if there are no access types available.
		assert accessTypes != null && accessTypes.length >= 1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
				IDebugUIConstants.PLUGIN_ID + ".MonitorMemoryBlockDialog_context"); //$NON-NLS-1$

		// The button bar will work better if we make the parent composite
		// a single column grid layout. For the widgets we add, we want a
		// a two-column grid, so we just create a sub composite for that.
		GridLayout gridLayout = new GridLayout();
		parent.setLayout(gridLayout);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		parent.setLayoutData(gridData);
		Composite composite = new Composite(parent, SWT.None);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);
		gridData = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gridData);
		parent = composite; // for all our widgets, the two-column composite is the real parent

		fExpressionRadio = new Button(parent, SWT.RADIO);
		final int radioButtonWidth = fExpressionRadio.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		fExpressionRadio.setText(Messages.AddMemBlockDlg_enterExpression);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		fExpressionRadio.setLayoutData(gridData);
		fExpressionRadio.addSelectionListener(this);

		fExpressionInput = new Combo(parent, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.horizontalIndent = radioButtonWidth;
		fExpressionInput.setLayoutData(gridData);

		fAddressRadio = new Button(parent, SWT.RADIO);
		fAddressRadio.setText(Messages.AddMemAccessTypeDlg_enterAddrAndAccessType);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		fAddressRadio.setLayoutData(gridData);
		fAddressRadio.addSelectionListener(this);

		// Sub-composite for [access type] [address] with no gap between them
		Composite accessAddrComposite = new Composite(parent, SWT.NONE);
		GridLayout accessAddrLayout = new GridLayout(2, false);
		accessAddrLayout.marginWidth = 0;
		accessAddrLayout.marginHeight = 0;
		accessAddrLayout.horizontalSpacing = 5;
		accessAddrComposite.setLayout(accessAddrLayout);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.horizontalIndent = radioButtonWidth;
		accessAddrComposite.setLayoutData(gridData);

		fAccessTypeInput = new Combo(accessAddrComposite, SWT.BORDER | SWT.READ_ONLY);
		gridData = new GridData();
		fAccessTypeInput.setLayoutData(gridData);
		fAccessTypeInput.addSelectionListener(this);

		fAccessTypeInput.setItems(fAccessTypes);

		// Try to persist the access type selection from one invocation of the
		// dialog to the next
		String accessTypeSelection = null;
		if (fPreviousAccessTypeSelection != null) {
			String[] items = fAccessTypeInput.getItems();
			for (String item : items) {
				if (item.equals(fPreviousAccessTypeSelection)) {
					accessTypeSelection = fPreviousAccessTypeSelection;
				}
			}
		}
		if (accessTypeSelection != null) {
			fAccessTypeInput.setText(accessTypeSelection);
		} else {
			fAccessTypeInput.select(0);
		}
		updateAccessTypeTooltip();

		fAddressInput = new Combo(accessAddrComposite, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		GC gc = new GC(fAddressInput);
		FontMetrics fm = gc.getFontMetrics();
		// Give enough room for a 64 bit hex address (25 is a guess at the combobox selector)
		gridData.minimumWidth = 18 * fm.getAverageCharWidth() + 25;
		gc.dispose();
		fAddressInput.setLayoutData(gridData);
		fAddressInput.addModifyListener(this);
		fAddressInput.addVerifyListener(new VerifyListener() {
			// limit entry to hex or decimal
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = false;
				final char c = e.character;
				if (Character.isDigit(c) || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F') || c == 'x'
						|| Character.isISOControl(e.character)) {
					e.doit = true;
				}
			}
		});

		// TODO: Re-enable secure/non-secure access when needed
		// Secure / Non-secure access radio buttons (sub-option of address path)
		// Aligned with the "Enter access type and address" radio button (no indent)
		// fSecureAccessRadio = new Button(parent, SWT.RADIO);
		// fSecureAccessRadio.setText("Secure access"); //$NON-NLS-1$
		// gridData = new GridData();
		// fSecureAccessRadio.setLayoutData(gridData);
		// fSecureAccessRadio.setSelection(true); // default

		// fNonSecureAccessRadio = new Button(parent, SWT.RADIO);
		// fNonSecureAccessRadio.setText("Non-secure access"); //$NON-NLS-1$
		// gridData = new GridData();
		// fNonSecureAccessRadio.setLayoutData(gridData);

		// add the history into the combo boxes
		String[] history = getHistory(sExpressionHistory);
		for (int i = 0; i < history.length; i++)
			fExpressionInput.add(history[i]);

		history = getHistory(sAddressHistory);
		for (int i = 0; i < history.length; i++)
			fAddressInput.add(history[i]);

		fExpressionInput.addModifyListener(this);

		if (sDefaultToExpression) {
			fExpressionRadio.setSelection(true);
			fAddressRadio.setSelection(false);
			fExpressionInput.setFocus();
		} else {
			fExpressionRadio.setSelection(false);
			fAddressRadio.setSelection(true);
			fAddressInput.setFocus();
		}

		updateControlEnablement();

		return parent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);

		// use the same title used by the platform dialog
		newShell.setText(Messages.AddMemBlockDlg_MonitorMemory);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		fExpression = fExpressionInput.getText();
		fAddress = fAddressInput.getText();
		fAccessType = fAccessTypeInput.getText();
		// TODO: Re-enable secure/non-secure access when needed
		// fSecureAccess = fSecureAccessRadio.getSelection();

		// add to HISTORY list
		if (fExpression.length() > 0)
			addHistory(sExpressionHistory, fExpression);
		if (fAddress.length() > 0)
			addHistory(sAddressHistory, fAddress);

		fEnteredExpression = fExpressionRadio.getSelection();

		fPreviousAccessTypeSelection = fAccessType;
		super.okPressed();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
	 */
	@Override
	public void modifyText(ModifyEvent e) {
		// if user enters text into either the address field or the expression one, automatically
		// select its associated radio button (and deselect the other, these are mutually exclusive)
		if (e.widget == fAddressInput || e.widget == fExpressionInput) {

			fAddressRadio.setSelection(e.widget != fExpressionInput);
			fExpressionRadio.setSelection(e.widget == fExpressionInput);
			sDefaultToExpression = (e.widget == fExpressionInput);
			updateControlEnablement();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TrayDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createButtonBar(Composite parent) {
		return super.createButtonBar(parent);
	}

	public String getExpression() {
		return fExpression;
	}

	public String getAddress() {
		return fAddress;
	}

	public String getAccessType() {
		return fAccessType;
	}

	// TODO: Re-enable secure/non-secure access when needed
	// public boolean isSecureAccess() {
	// 	return fSecureAccess;
	// }

	public boolean enteredExpression() {
		return fEnteredExpression;
	}

	private static void addHistory(List<String> list, String item) {
		if (!list.contains(item))
			list.add(0, item);

		if (list.size() > 5)
			list.remove(list.size() - 1);
	}

	private static String[] getHistory(List<String> list) {
		return list.toArray(new String[list.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// no-op
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(SelectionEvent e) {
		// if user selects a radio button, select its associated input and deselect the other
		if (e.widget == fExpressionRadio) {
			fExpressionRadio.setSelection(true);
			fAddressRadio.setSelection(false);
			fExpressionInput.setFocus();
			sDefaultToExpression = true;
		} else if (e.widget == fAddressRadio) {
			fExpressionRadio.setSelection(false);
			fAddressRadio.setSelection(true);
			fAddressInput.setFocus();
			sDefaultToExpression = false;
		} else if (e.widget == fAccessTypeInput) {
			updateAccessTypeTooltip();
		}
		updateControlEnablement();
	}

	/**
	 * Updates the tooltip on the access type combo to reflect the currently
	 * selected access type.
	 */
	private void updateAccessTypeTooltip() {
		int idx = fAccessTypeInput.getSelectionIndex();
		if (idx >= 0 && idx < fAccessTypeTooltips.length) {
			fAccessTypeInput.setToolTipText(fAccessTypeTooltips[idx]);
		} else {
			fAccessTypeInput.setToolTipText(null);
		}
	}

	private void updateControlEnablement() {
		boolean expressionSelected = fExpressionRadio.getSelection();
		fExpressionInput.setEnabled(expressionSelected);
		fAccessTypeInput.setEnabled(!expressionSelected);
		fAddressInput.setEnabled(!expressionSelected);
		// TODO: Re-enable secure/non-secure access when needed
		// fSecureAccessRadio.setEnabled(!expressionSelected);
		// fNonSecureAccessRadio.setEnabled(!expressionSelected);
	}
}
