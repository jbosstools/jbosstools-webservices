package org.jboss.tools.ws.ui.preferences;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Label;
import org.jboss.tools.ws.core.classpath.JBossWSRuntime;

public class WSRuntimeSelectionChangedListener implements
		ISelectionChangedListener {
	
	private Label impl;
	private Label vdetail;
	
	public WSRuntimeSelectionChangedListener(Label impl, Label vdetail) {
		this.impl = impl;
		this.vdetail = vdetail;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof StructuredSelection) {
			JBossWSRuntime runtime = (JBossWSRuntime)((StructuredSelection)selection).getFirstElement();
			if (runtime == null) {
				return;
			}
			impl.setText(runtime.getImpl());
			vdetail.setText(runtime.getVersionDetail());
		}
	}

}
