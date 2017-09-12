/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.reddeer.ui.wizards.jaxrs;

import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.jface.wizard.WizardPage;
import org.eclipse.reddeer.swt.api.Text;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.button.RadioButton;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.swt.impl.text.LabeledText;

/**
 * First and the only {@link JAXRSApplicationWizard} page.
 *
 * Page offers 2 options have to create a new JAX-RS Application:
 * 1. "Subclass of javax.ws.rs.core.Application()" - {@link #useSubclassOfApplication()}
 * 2. "Defined in the web deployment descriptor" - {@link #useDeploymentDescriptor()}
 *
 * @author Radoslav Rabara
 *
 */
public class JAXRSApplicationWizardPage extends WizardPage {
	
	public JAXRSApplicationWizardPage(ReferencedComposite referencedComposite) {
		super(referencedComposite);
	}

	/**
	 * Returns info text ( = info/warning/error)
	 * @return
	 */
	public String getWizardPageInfoText() {
		return new DefaultText(5).getText();
	}
	
	public SubclassOfApplicationWizardPart useSubclassOfApplication() {
		new RadioButton(0).click();//"Subclass of javax.ws.rs.core.Application"
		return new SubclassOfApplicationWizardPart();
	}
	
	public DeploymentDescriptorWizardPart useDeploymentDescriptor() {
		new RadioButton(1).click();//"Defined in the web deployment descriptor"
		return new DeploymentDescriptorWizardPart();
	}
	
	public class DeploymentDescriptorWizardPart {
		public void setApplicationPath(String path) {
			new LabeledText("Application path:").setText(path);
		}
	}
	
	public class SubclassOfApplicationWizardPart {
		//source folder
		public String getSourceFolder() {
			return getSourceFolderText().getText();
		}
		
		public void setSourceFolder(String srcFolder) {
			getSourceFolderText().setText(srcFolder);
		}
		
		private Text getSourceFolderText() {
			return new LabeledText("Source folder:");
		}
		
		//browse source folder
		public void openBrowseSourceFolder() {
			getBrowseSourceFolderButton().click();
		}
		
		public boolean isBrowseSourceFolderEnabled() {
			return getBrowseSourceFolderButton().isEnabled();
		}
		
		private PushButton getBrowseSourceFolderButton() {
			PushButton browse =  new PushButton(0);
			assert browse.getText().equals("Br&owse...");
			return browse;
		}
		
		//package
		public void setPackage(String pkg) {
			new LabeledText("Package:").setText(pkg);
		}
		
		//browse package
		public void openBrowsePackage() {
			getBrowsePackageButton().click();
		}
		
		public boolean isBrowsePackageEnabled() {
			return getBrowsePackageButton().isEnabled();
		}
		
		private PushButton getBrowsePackageButton() {
			PushButton browse =  new PushButton(1);
			assert browse.getText().equals("Bro&wse...");
			return browse;
		}
		
		//name
		public void setName(String name) {
			new LabeledText("Name:").setText(name);
		}
		
		//application path
		public void setApplicationPath(String appPath) {
			new LabeledText("Application path:").setText(appPath);
		}
		
		//configure templates and default values
		public void configureTemplatesAndDefaultValues() {
			throw new UnsupportedOperationException();
		}
		
		//generate comments
		public void setGenerateComments(boolean generateComments) {
			CheckBox checkBox = new CheckBox();
			if(checkBox.isChecked() == !generateComments) {
				checkBox.click();
			}
		}
	}
}
