package org.jboss.tools.ws.creation.ui.widgets;

import javax.wsdl.Service;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.wst.command.internal.env.ui.widgets.SimpleWidgetDataContributor;
import org.eclipse.wst.command.internal.env.ui.widgets.WidgetDataEvents;
import org.eclipse.wst.ws.internal.wsrt.WebServiceScenario;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.commands.WSDL2JavaHelpOptionCommand;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;
import org.jboss.tools.ws.creation.ui.utils.JBossCreationUIUtils;
import org.jboss.tools.ws.ui.utils.JBossWSUIUtils;

@SuppressWarnings("restriction")
public class WSDL2JavaCodeGenConfigWidget extends SimpleWidgetDataContributor {

	private ServiceModel model;
	private IStatus status = null;
	private Button removeButton;
	private Button updateWebxmlButton;
	private Button genDefaultButton;
	private Button extensionButton;
	private Button customPacButton;
	private Button catalogButton;
	private Button additionalButton;
	private Combo serviceCombo = null;
	private Combo sourceCombo = null;
	private Combo targetCombo = null;
	private Text customPacText;
	private Text catalogText;
	private Text additionalText;
	private List bindingList;
	private boolean isOK;

	public WSDL2JavaCodeGenConfigWidget(ServiceModel model) {
		this.model = model;
	}

	public WidgetDataEvents addControls(Composite parent,
			final Listener statusListener) {

		Composite configCom = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		configCom.setLayout(layout);
		configCom.setLayoutData(new GridData(GridData.FILL_BOTH));

		// services list
		serviceCombo = JBossCreationUIUtils.createComboItem(configCom, model,
				JBossWSCreationCoreMessages.Label_Service_Name,
				JBossWSCreationCoreMessages.Tooltip_Service);
		serviceCombo.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event arg0) {
				Service service = (Service) serviceCombo.getData(serviceCombo.getText());
				if (service == null) {
					return;
				} else if (service.getPorts() == null
						|| service.getPorts().isEmpty()) {
					status = StatusUtils.errorStatus(JBossWSCreationCoreMessages.Error_Message_No_ServicePort);
					statusListener.handleEvent(null);
				} else {
					model.setService(service);
				}
			}
		});

		// choose source folder
		sourceCombo = JBossCreationUIUtils.createComboItem(configCom, model,
				JBossWSCreationCoreMessages.Label_SourceFolder_Name,
				JBossWSCreationCoreMessages.Tooltip_SourceFolder);
		sourceCombo.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event arg0) {
				String javaSourceFolder = sourceCombo.getText();
				model.setJavaSourceFolder(javaSourceFolder);
			}
		});

		// custom package name
		customPacText = JBossCreationUIUtils.createTextItemWithButton(configCom, model,
				JBossWSCreationCoreMessages.Label_Custom_Package_Name,
				JBossWSCreationCoreMessages.Tooltip_Custom_Package);
		customPacButton = new Button(configCom, SWT.NONE);
		customPacButton.setText(JBossWSCreationCoreMessages.Label__Browse_Button);
		customPacText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if ("".equals(customPacText.getText()) //$NON-NLS-1$
						|| validatePackage(customPacText.getText())) {
					model.setCustomPackage(customPacText.getText());
				}
				statusListener.handleEvent(null);
			}
		});
		customPacButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				IJavaProject project = model.getJavaProject();
				if (project == null) {
					return;
				}
				try {
					SelectionDialog dialog = JavaUI.createPackageDialog(
									null, project, IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS);
					if (dialog.open() == Window.OK) {
						if (dialog.getResult() != null
								&& dialog.getResult().length == 1) {
							String fqPackageName = ((PackageFragment) dialog
									.getResult()[0]).getElementName();
							customPacText.setText(fqPackageName);
						}
					}
				} catch (JavaModelException e1) {
					e1.printStackTrace();
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		// target
		targetCombo = JBossCreationUIUtils.createComboItem(configCom, model,
				JBossWSCreationCoreMessages.Label_JaxWS_Target,
				JBossWSCreationCoreMessages.Tooltip_JaxWS_Target);
		targetCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				model.setTarget(targetCombo.getText());
			}
		});

		// catalog file
		catalogText = JBossCreationUIUtils.createTextItemWithButton(configCom,
				model, JBossWSCreationCoreMessages.Label_Catalog_File,
				JBossWSCreationCoreMessages.Tooltip_Catalog_File);
		catalogButton = new Button(configCom, SWT.NONE);
		catalogButton.setText(JBossWSCreationCoreMessages.Label_Add_Button);
		catalogButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String fileLocation = new FileDialog(Display.getCurrent().getActiveShell(), SWT.NONE).open();
				catalogText.setText(fileLocation);
				model.setCatalog(fileLocation);
			}
		});

		// binding files
		createBindingFileItem(configCom);

		// extension check button
		extensionButton = JBossCreationUIUtils.createCheckButton(
						configCom, JBossWSCreationCoreMessages.Label_EnableSOAP12_Binding_Extension);
		extensionButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				model.setEnableSOAP12(extensionButton.getSelection());
			}
		});

		if (model.getWsScenario() != WebServiceScenario.CLIENT) {
			// generate default impl class
			genDefaultButton = JBossCreationUIUtils.createCheckButton(configCom,
					JBossWSCreationCoreMessages.Label_Generate_Impelemtation);
			genDefaultButton.setSelection(true);
			genDefaultButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					model.setGenerateImplementatoin(genDefaultButton.getSelection());
					updateWebxmlButton.setEnabled(genDefaultButton.getSelection());
					if (!genDefaultButton.getSelection()) {
						model.setUpdateWebxml(false);
					} else {
						model.setUpdateWebxml(updateWebxmlButton.getSelection());
					}
				}
			});

			// update the web.xml
			updateWebxmlButton = JBossCreationUIUtils.createCheckButton(
					configCom, JBossWSCreationCoreMessages.Label_Update_Webxml);
			updateWebxmlButton.setSelection(true);
			updateWebxmlButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					model.setUpdateWebxml(updateWebxmlButton.getSelection());
				}
			});
		}

		// additional choice
		additionalText = JBossCreationUIUtils.createTextItemWithButton(configCom, model,
				JBossWSCreationCoreMessages.Label_AdditionalOption_Name,
				JBossWSCreationCoreMessages.Tooltip_AdditionalOption);
		additionalButton = new Button(configCom, SWT.NONE);
		additionalButton.setText(JBossWSCreationCoreMessages.Label_Help_Button);
		additionalButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String message = getAdditionalOptions(model);
				MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
								JBossWSCreationCoreMessages.AdditionalOption_Dialog_Title, message);
			}
		});
		additionalText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!"".equals(additionalText.getText())) { //$NON-NLS-1$
					model.setAddOptions(additionalText.getText());
				} else {
					status = StatusUtils.errorStatus(JBossWSCreationCoreMessages.Error_Message_No_ServletName);
					statusListener.handleEvent(null);
				}
			}
		});

		updateComposite();
		return this;
	}

	private void createBindingFileItem(Composite configCom) {
		Label label = new Label(configCom, SWT.NONE);
		label.setText(JBossWSCreationCoreMessages.Label_Binding_File);
		label.setToolTipText(JBossWSCreationCoreMessages.Tooltip_BindingFile);
		bindingList = new List(configCom, SWT.BORDER | SWT.SCROLL_LINE
				| SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		bindingList.setToolTipText(JBossWSCreationCoreMessages.Tooltip_BindingFile);
		gd.heightHint = Display.getCurrent().getActiveShell().getBounds().height / 4;
		gd.verticalSpan = 3;
		bindingList.setLayoutData(gd);
		loadBindingFiles(bindingList);
		bindingList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (bindingList.getSelectionIndex() >= 0) {
					removeButton.setEnabled(true);
				} else {
					removeButton.setEnabled(false);
				}
			}
		});

		Button btnSelect = new Button(configCom, SWT.NONE);
		btnSelect.setText(JBossWSCreationCoreMessages.Label_Add_Button);
		btnSelect.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String fileLocation = new FileDialog(Display.getCurrent()
						.getActiveShell(), SWT.NONE).open();
				if (fileLocation != null
						&& !model.getBindingFiles().contains(fileLocation)) {
					bindingList.add(fileLocation);
					model.addBindingFile(fileLocation);
				}
			}
		});
		new Label(configCom, SWT.NONE);
		removeButton = new Button(configCom, SWT.NONE);
		removeButton.setEnabled(false);
		removeButton.setText(JBossWSCreationCoreMessages.Label_Remove_Button);
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				model.getBindingFiles().remove(bindingList.getSelectionIndex());
				bindingList.remove(bindingList.getSelectionIndex());
				if (bindingList.getSelectionIndex() == -1) {
					removeButton.setEnabled(false);
				}
			}
		});
	}

	private void updateComposite() {
		boolean a = JBossWSCreationUtils.supportSOAP12(model.getWebProjectName());
		extensionButton.setEnabled(a);
		extensionButton.setSelection(a);

		isOK = JBossCreationUIUtils.populateServiceCombo(serviceCombo,
				model.getWsdlDefinition());
		if (!isOK) {
			status = StatusUtils.errorStatus(JBossWSCreationCoreMessages.Error_Message_No_Service);
		}

		isOK = JBossCreationUIUtils.populateSourceFolderCombo(sourceCombo, model.getSrcList());
		if (!isOK) {
			status = StatusUtils.errorStatus(JBossWSCreationCoreMessages.Error_Message_No_SourceFolder);
		}
		JBossCreationUIUtils.populateTargetCombo(targetCombo, model);
		customPacText.setText(model.getCustomPackage());
	}

	private void loadBindingFiles(List bindingList) {
		for (String fileLocation : model.getBindingFiles()) {
			bindingList.add(fileLocation);
		}
	}

	public static String getAdditionalOptions(ServiceModel model) {
		IStatus status = null;
		String message = JBossWSCreationCoreMessages.No_Message_AdditionalOptions_Dialog;
		WSDL2JavaHelpOptionCommand command = new WSDL2JavaHelpOptionCommand(
				model);
		try {
			status = command.execute(null, null);
		} catch (ExecutionException e) {
			status = StatusUtils.errorStatus(e);
		}
		if (status.isOK()) {
			Thread thread = command.getThread();
			int i = 0;
			while (thread.isAlive() && i < 20) {
				i++;
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (command.getHelpOptions() != null) {
				message = command.getHelpOptions();
			}
		}
		return message;
	}

	private boolean validatePackage(String name) {
		status = JBossWSUIUtils.validatePackageName(name,model.getJavaProject());
		if (status != null && status.getSeverity() == IStatus.ERROR) {
			return false;
		}
		return true;
	}

	public IStatus getStatus() {
		return status;
	}

	public ServiceModel getModel() {
		return model;
	}

	public void setModel(ServiceModel model) {
		this.model = model;
	}
}
