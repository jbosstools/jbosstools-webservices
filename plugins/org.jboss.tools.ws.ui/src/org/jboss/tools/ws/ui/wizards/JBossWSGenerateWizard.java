package org.jboss.tools.ws.ui.wizards;

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

public class JBossWSGenerateWizard extends Wizard {

	private JBossWSGenerateWebXmlWizardPage firstPage;
	private JBossWSGenerateSampleClassWizardPage secondPage;

	String NAMEDEFAULT = "HelloWorld"; //$NON-NLS-1$
	String PACKAGEDEFAULT = "org.jboss.samples.webservices"; //$NON-NLS-1$
	String CLASSDEFAULT = "HelloWorld"; //$NON-NLS-1$

	private String serviceName = NAMEDEFAULT;
	private String packageName = PACKAGEDEFAULT;
	private String className = CLASSDEFAULT;
	private boolean useDefaultServiceName = true;
	private boolean useDefaultClassName = true;

	public JBossWSGenerateWizard() {
		super();
		super.setWindowTitle(JBossWSUIMessages.JBossWS_GenerateWizard_Title);
		super.setHelpAvailable(false);
	}

	public void addPages() {
		super.addPages();
		firstPage = new JBossWSGenerateWebXmlWizardPage("first"); //$NON-NLS-1$
		secondPage = new JBossWSGenerateSampleClassWizardPage("second"); //$NON-NLS-1$
		addPage(firstPage);
		addPage(secondPage);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public boolean isUseDefaultServiceName() {
		return useDefaultServiceName;
	}

	public void setUseDefaultServiceName(boolean useDefaultServiceName) {
		this.useDefaultServiceName = useDefaultServiceName;
	}

	public boolean isUseDefaultClassName() {
		return useDefaultClassName;
	}

	public void setUseDefaultClassName(boolean useDefaultClassName) {
		this.useDefaultClassName = useDefaultClassName;
	}

}
