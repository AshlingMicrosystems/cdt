package org.eclipse.cdt.dsf.debug.internal.ui.debugview.layout.actions;

/**
 * This class acts as a data communicator between DsfRenameGroupContextCommand and RenameGroupDialog
 */
public class GroupRenameInfo {

	private String groupName;

	public GroupRenameInfo() {
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

}