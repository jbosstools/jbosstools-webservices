package org.jboss.tools.ws.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

public class JBossWSGenerateSampleClassWizardPage extends WizardPage {
	private JBossWSGenerateWizard wizard;
	private Text packageName;
	private Text className;
	private Button checkDefault;

	protected JBossWSGenerateSampleClassWizardPage(String pageName) {
		super(pageName);
		this
				.setTitle(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateSampleClassPage_Title);
		this
				.setDescription(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateSampleClassPage_Description);
	}

	public void createControl(Composite parent) {
		Composite composite = createDialogArea(parent);
		this.wizard = (JBossWSGenerateWizard) this.getWizard();
		new Label(composite, SWT.NONE).setText(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateSampleClassPage_Package_Label);
		packageName = new Text(composite, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		packageName.setLayoutData(gd);
		packageName.setText(wizard.getPackageName());
		packageName.setEnabled(!wizard.isUseDefaultClassName());
		packageName.addModifyListener(new ModifyListener(){

			public void modifyText(ModifyEvent e) {
				if (!"".equals(packageName.getText())&&!"".equals(className.getText())){ //$NON-NLS-1$ //$NON-NLS-2$
					setPageComplete(true);
				}else {
					setPageComplete(false);
				}
			}
			
		});
		
		new Label(composite, SWT.NONE).setText(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateSampleClassPage_ClassName_Label);
		className = new Text(composite, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		className.setLayoutData(gd);
		className.setText(wizard.getClassName());
		className.setEnabled(!wizard.isUseDefaultClassName());
		className.addModifyListener(new ModifyListener(){

			public void modifyText(ModifyEvent e) {
				if (!"".equals(packageName.getText())&&!"".equals(className.getText())){ //$NON-NLS-1$ //$NON-NLS-2$
					setPageComplete(true);
				}else {
					setPageComplete(false);
				}
			}
			
		});

		checkDefault = new Button(composite, SWT.CHECK);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		checkDefault.setLayoutData(gd);
		checkDefault.setSelection(wizard.isUseDefaultClassName());
		checkDefault.setText(JBossWSUIMessages.JBossWS_GenerateWizard_WizardPage_CheckButton_Label);
		checkDefault.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				if (checkDefault.getSelection()) {
					checkDefault.setSelection(true);
					packageName.setText(wizard.PACKAGEDEFAULT);
					className.setText(wizard.CLASSDEFAULT);
				} else {
					checkDefault.setSelection(false);
				}
				packageName.setEnabled(!checkDefault.getSelection());
				className.setEnabled(!checkDefault.getSelection());
				wizard.setUseDefaultClassName(!checkDefault.getSelection());
			}

		});
		setControl(composite);

	}

	public boolean isPageComplete() {
        if(!"".equals(packageName.getText())&&!"".equals(className.getText())){  //$NON-NLS-1$//$NON-NLS-2$
    		wizard.setPackageName(packageName.getText());
    		wizard.setClassName(className.getText());
    		return true;
        }

		return false;
	}

	private Composite createDialogArea(Composite parent) {
		// create a composite with standard margins and spacing
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 7;
		layout.marginWidth = 7;
		layout.verticalSpacing = 4;
		layout.horizontalSpacing = 4;
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		return composite;
	}
}
