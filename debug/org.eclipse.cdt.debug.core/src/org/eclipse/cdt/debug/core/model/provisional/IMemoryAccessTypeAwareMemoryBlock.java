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

import org.eclipse.debug.core.model.IMemoryBlockExtension;

/**
 * An extension of IMemoryBlockExtension that supports memory access types. An
 * instance of this is returned from
 * {@link IMemoryAccessTypeAwareMemoryBlockRetrieval#getMemoryBlockForAccessType(String, Object, String)}.
 * The memory access type interface is provisional, thus this class cannot yet be API.
 */
public interface IMemoryAccessTypeAwareMemoryBlock extends IMemoryBlockExtension {

	/**
	 * Returns the memory access type associated with this block.
	 *
	 * @return a memory access type ID, or null if the block was created without
	 *         a memory access type identifier. These IDs originate from the backend.
	 *         See {@link IMemoryAccessTypeAwareMemoryBlockRetrieval#getMemoryAccessTypes(Object,
	 *         IMemoryAccessTypeAwareMemoryBlockRetrieval.GetMemoryAccessTypesRequest)}
	 */
	String getMemoryAccessTypeID();
}
