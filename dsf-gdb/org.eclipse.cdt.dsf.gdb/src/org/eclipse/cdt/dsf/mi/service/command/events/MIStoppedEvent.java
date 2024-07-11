/*******************************************************************************
 * Copyright (c) 2009 QNX Software Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Wind River Systems   - Modified for new DSF Reference Implementation
 *******************************************************************************/

package org.eclipse.cdt.dsf.mi.service.command.events;

import java.util.Optional;

import org.eclipse.cdt.dsf.concurrent.Immutable;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.command.output.MIConst;
import org.eclipse.cdt.dsf.mi.service.command.output.MIFrame;
import org.eclipse.cdt.dsf.mi.service.command.output.MIResult;
import org.eclipse.cdt.dsf.mi.service.command.output.MITuple;
import org.eclipse.cdt.dsf.mi.service.command.output.MIValue;

/**
 *  *stopped
 *
 */
@Immutable
public class MIStoppedEvent extends MIEvent<IExecutionDMContext> {

	final private MIFrame frame;
	// <Ashling customization>
	private String threadno = null;
	// <Ashling customization>

	protected MIStoppedEvent(IExecutionDMContext ctx, int token, MIResult[] results, MIFrame frame) {
		super(ctx, token, results);
		this.frame = frame;
	}

	// <Ashling customization>
	protected MIStoppedEvent(IExecutionDMContext ctx, int token, MIResult[] results, MIFrame frame, String threadno) {
		super(ctx, token, results);
		this.frame = frame;
		this.threadno = threadno;
	}

	// <Ashling customization>

	public MIFrame getFrame() {
		return frame;
	}

	// <Ashling customization>
	public Optional<String> getThreadNumber() {
		return Optional.ofNullable(threadno);
	}
	// <Ashling customization>

	/**
	 * @since 1.1
	 */
	public static MIStoppedEvent parse(IExecutionDMContext dmc, int token, MIResult[] results) {
		MIFrame frame = null;
		String threadno = "";
		for (int i = 0; i < results.length; i++) {
			String var = results[i].getVariable();
			MIValue value = results[i].getMIValue();
			String str = "";
			if (var.equals("frame")) { //$NON-NLS-1$
				if (value instanceof MITuple) {
					frame = new MIFrame((MITuple) value);
				}
			}
			// <Ashling customization>
			if (value != null && value instanceof MIConst) {
				str = ((MIConst) value).getString();
			}
			if (var.equals("thread-id")) { //$NON-NLS-1$
				threadno = str.trim();
			}
		}
		return new MIStoppedEvent(dmc, token, results, frame, threadno);
		// <Ashling customization>
	}
}
