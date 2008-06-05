package org.jboss.tools.ws.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jboss.tools.ws.ui.preferences.ButtonFieldEditor.ButtonPressedAction;

/**
 * @author eskimo
 *
 */
public class PushButtonField extends BaseField {
	
	Button button;
	
	/**
	 * 
	 */
	@Override
	public Control getControl() {
		return button;
	}


	public PushButtonField(Composite composite, ButtonPressedAction listener) {
		button = new Button(composite,SWT.PUSH);
		button.setText(listener.getText());
		button.addSelectionListener(listener);
	}
}
