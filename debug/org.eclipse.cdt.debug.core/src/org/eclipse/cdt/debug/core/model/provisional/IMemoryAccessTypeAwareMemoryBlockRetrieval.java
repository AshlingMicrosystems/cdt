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
package org.eclipse.cdt.debug.core.model.provisional;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.IMemoryBlockRetrievalExtension;

/**
 * An extension of the IMemoryBlockRetrievalExtension interface that supports
 * memory access types (e.g., APB, AXI). When a specific access type is selected,
 * the backend sends a custom monitor command instead of the default
 * {@code -data-read-memory-bytes} MI command. The memory access type interface
 * is provisional, thus this class cannot yet be API.
 *
 * <p>
 * This interface is independent of {@link IMemorySpaceAwareMemoryBlockRetrieval}
 * and does not modify or extend the memory space mechanism.
 * </p>
 */
public interface IMemoryAccessTypeAwareMemoryBlockRetrieval extends IMemoryBlockRetrievalExtension {

	/**
	 * Caller to {@link #getMemoryAccessTypes(Object, GetMemoryAccessTypesRequest)}
	 * provides one of these, as that method may need to consult the debugger
	 * backend, and thus needs to be asynchronous.
	 */
	interface GetMemoryAccessTypesRequest extends IRequest {
		/**
		 * Returns the available memory access types.
		 *
		 * @return an array of memory access type identifiers
		 */
		String[] getMemoryAccessTypes();

		/**
		 * Sets the available memory access types.
		 *
		 * @param accessTypeIds the memory access type identifiers
		 */
		void setMemoryAccessTypes(String[] accessTypeIds);

		/**
		 * Returns the tooltips for the available memory access types.
		 * The returned array is parallel to {@link #getMemoryAccessTypes()}.
		 *
		 * @return an array of tooltip strings, or an empty array if not set
		 */
		default String[] getMemoryAccessTypeTooltips() {
			return new String[0];
		}

		/**
		 * Sets the tooltips for the available memory access types.
		 * The array must be parallel to the one set via {@link #setMemoryAccessTypes(String[])}.
		 *
		 * @param tooltips the tooltip strings
		 */
		default void setMemoryAccessTypeTooltips(String[] tooltips) {
			// default no-op for backward compatibility
		}
	}

	/**
	 * Provides the memory access types available for the given debug context.
	 *
	 * @param context
	 *            a debug context
	 * @param request
	 *            the asynchronous data request object
	 */
	void getMemoryAccessTypes(Object context, GetMemoryAccessTypesRequest request);

	/**
	 * Retrieves and returns a memory block for the given access type.
	 *
	 * @param expression
	 *            expression to be evaluated to an address
	 * @param context
	 *            a debug context
	 * @param accessTypeId
	 *            the memory access type the block is being requested for, or
	 *            null if n/a (i.e., use default access type)
	 * @return a memory block extension based on the given parameters
	 * @throws DebugException
	 *             if unable to retrieve the specified memory
	 */
	IMemoryBlockExtension getMemoryBlockForAccessType(String expression, Object context, String accessTypeId)
			throws DebugException;
}
