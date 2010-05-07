package org.jboss.tools.ws.ui.views;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.internet.monitor.core.internal.provisional.IMonitorWorkingCopy;
import org.eclipse.wst.internet.monitor.ui.internal.MonitorDialog;

@SuppressWarnings("restriction")
public class AddMonitorDialog extends MonitorDialog {

	public AddMonitorDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public IMonitorWorkingCopy getMonitor() {
		return this.monitor;
	}

}
