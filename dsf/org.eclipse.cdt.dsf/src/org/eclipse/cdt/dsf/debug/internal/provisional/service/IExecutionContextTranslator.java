/*******************************************************************************
 * Copyright (c) 2010, 2015 Texas Instruments, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Dobrin Alexiev (Texas Instruments) - initial API and implementation (bug 240208)
********************************************************************************/
package org.eclipse.cdt.dsf.debug.internal.provisional.service;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IContainerDMContext;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.service.IDsfService;

/**
 * EXPERIMENTAL. This class or interface has been added as part
 * of a work in progress. There is no guarantee that this API will work or that
 * it will remain the same.
 *
 * Interface for translating one model of execution context hierarchy to another.
 * As the grouping feature is added incrementally this interface will be defined properly.
 *
 * The reason this interface is proposed at the DSF level is to accommodate requirements from
 * multiple DSF debuggers.
 *
 * @since 2.2
 * @experimental
 */
public interface IExecutionContextTranslator extends IDsfService {

	/**
	 * Returns true if all specified execution contexts can be grouped together.
	 *
	 * @param contexts The list of execution contexts to be grouped
	 * @param rm Contains True if all specified contexts can be grouped together.
	 *           Contains False otherwise.
	 */
	void canGroup(IExecutionDMContext[] contexts, DataRequestMonitor<Boolean> rm);

	/**
	 * Returns true if all specified execution contexts are groups, that can be un-grouped.
	 *
	 * @param contexts The list of execution contexts to be un-grouped
	 * @param rm Contains True if all specified contexts can be un-grouped.
	 *           Contains False otherwise.
	 */
	void canUngroup(IExecutionDMContext[] contexts, DataRequestMonitor<Boolean> rm);

	//<CUSTOMISATION - ASHLING> Rename function on Group
	/**
	 * Returns true the specified context is a group.
	 *
	 * @param context The group context which has to renamed
	 * @param rm Contains True if the context can be renamed.
	 */
	void canRename(IExecutionDMContext context, DataRequestMonitor<Boolean> rm);
	//<CUSTOMISATION>
	/**
	 * Groups the specified execution contexts.
	 *
	 * @param contexts The list of contexts to group together, into a new group
	 * @param requestMonitor Returns the group created by the grouping action
	 * @since 2.7
	 */
	void group(IExecutionDMContext[] contexts, DataRequestMonitor<IContainerDMContext> rm);

	/**
	 * Un-groups the specified execution contexts.
	 *
	 * @param contexts The list of groups to un-group
	 */
	void ungroup(IExecutionDMContext[] contexts, RequestMonitor rm);
	
	//<CUSTOMISATION - ASHLING> Rename function on Group
	/**
	 * Rename the specified context
	 *
	 * @param context The group context to be renamed.
	 * @param newGroupName The new goup name.
	 * @param rm
	 */
	void rename(IExecutionDMContext context, String newGroupName, RequestMonitor rm);
	//<CUSTOMISATION>
}
