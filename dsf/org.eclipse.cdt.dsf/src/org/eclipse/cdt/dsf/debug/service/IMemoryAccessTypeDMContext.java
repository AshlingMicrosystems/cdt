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
package org.eclipse.cdt.dsf.debug.service;

import org.eclipse.cdt.dsf.debug.service.IMemory.IMemoryDMContext;

/**
 * A context that represents a particular memory access type (e.g., APB, AXI).
 * When a non-default access type is selected, the memory service uses a custom
 * monitor command instead of the standard {@code -data-read-memory-bytes} MI
 * command.
 *
 * <p>
 * This context is independent of {@link IMemorySpaces.IMemorySpaceDMContext}
 * and does not modify or extend the memory space mechanism.
 * </p>
 */
public interface IMemoryAccessTypeDMContext extends IMemoryDMContext {

	/**
	 * Returns the memory access type identifier associated with this context.
	 *
	 * @return the access type ID (e.g., "APB", "AXI"), or {@code null} if
	 *         the default access type is used
	 */
	String getAccessTypeId();

	/**
	 * Returns the original expression string exactly as the user typed it in the UI.
	 * This is used for monitor commands so that the address is sent verbatim
	 * (e.g., "0x158c0000") without any reformatting or normalization.
	 *
	 * @return the original user expression, or {@code null} if not available
	 */
	String getUserExpression();
}
