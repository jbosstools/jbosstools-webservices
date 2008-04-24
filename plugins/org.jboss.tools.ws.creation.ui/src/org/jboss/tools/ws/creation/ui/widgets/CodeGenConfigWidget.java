package org.jboss.tools.ws.creation.ui.widgets;



import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.command.internal.env.ui.widgets.SimpleWidgetDataContributor;
import org.eclipse.wst.command.internal.env.ui.widgets.WidgetDataEvents;
import org.jboos.tools.ws.creation.core.data.ServiceModel;

public class CodeGenConfigWidget extends SimpleWidgetDataContributor {
	
	private ServiceModel model;

	public CodeGenConfigWidget(ServiceModel model){
		this.model = model;
	}
	
	public WidgetDataEvents addControls( Composite parent, Listener statusListener){
		
		Composite configCom = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);		
		configCom.setLayout(layout);
		configCom.setLayoutData(new GridData(GridData.FILL_BOTH));
		Label lblCustomPakage = new Label(configCom, SWT.NONE);
		lblCustomPakage.setText("Custom package name:");
		final Text txtCustomPkgName = new Text(configCom, SWT.NONE);
		txtCustomPkgName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtCustomPkgName.addModifyListener(new ModifyListener(){

			public void modifyText(ModifyEvent e) {
				model.setCustomPackage(txtCustomPkgName.getText());
			}});
		
		new Label(configCom, SWT.NONE).setText("Binding file:");
		Composite fileSelection = new Composite(configCom, SWT.NONE);
		fileSelection.setLayout(new GridLayout(2, false));
		fileSelection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		final Text txtFiles = new Text(fileSelection, SWT.NONE);
		txtFiles.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Button btnSelect = new Button(fileSelection, SWT.NONE);
		btnSelect.setText("...");
		btnSelect.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				String fileLocation = new FileDialog(Display.getCurrent().getActiveShell(), SWT.NONE).open();
				txtFiles.setText(fileLocation);
				model.setBindingFileLcation(fileLocation);
			}
		});
		
		return this;
	}
}
