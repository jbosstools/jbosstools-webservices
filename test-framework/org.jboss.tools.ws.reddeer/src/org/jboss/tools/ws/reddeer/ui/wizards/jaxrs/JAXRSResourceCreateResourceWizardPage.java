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
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.swt.impl.text.LabeledText;

/**
 * {@link JAXRSResourceWizard}'s first wizard page.
 *
 * @author Radoslav Rabar
 *
 */
public class JAXRSResourceCreateResourceWizardPage extends WizardPage {

	public JAXRSResourceCreateResourceWizardPage(ReferencedComposite referencedComposite) {
		super(referencedComposite);
	}

	/**
	 * Returns info text ( = info/warning/error)
	 * 
	 * @return
	 */
	public String getWizardPageInfoText() {
		return new DefaultText(5).getText();
	}

	// source folder
	public String getSourceFolder() {
		return getSourceFolderText().getText();
	}

	public void setSourceFolder(String srcFolder) {
		getSourceFolderText().setText(srcFolder);
	}

	private Text getSourceFolderText() {
		return new LabeledText("Source folder:");
	}

	// browse source folder
	public void openBrowseSourceFolder() {
		getBrowseSourceFolderButton().click();
	}

	public boolean isBrowseSourceFolderEnabled() {
		return getBrowseSourceFolderButton().isEnabled();
	}

	private PushButton getBrowseSourceFolderButton() {
		PushButton browse = new PushButton(0);
		assert browse.getText().equals("Br&owse...");
		return browse;
	}

	// package
	public void setPackage(String pkg) {
		new LabeledText("Package:").setText(pkg);
	}

	// browse package
	public void openBrowsePackage() {
		getBrowsePackageButton().click();
	}

	public boolean isBrowsePackageEnabled() {
		return getBrowsePackageButton().isEnabled();
	}

	private PushButton getBrowsePackageButton() {
		PushButton browse = new PushButton(1);
		assert browse.getText().equals("Bro&wse...");
		return browse;
	}

	// target entity
	public void setTargetEntity(String entityPath) {
		new LabeledText("Target entity:").setText(entityPath);
	}

	public void openBrowseTargetEntity() {
		getBrowseTargetEntityButton().click();
	}

	public boolean isBrowseTargetEntityEnabled() {
		return getBrowseTargetEntityButton().isEnabled();
	}

	private PushButton getBrowseTargetEntityButton() {
		PushButton browse = new PushButton(1);
		assert browse.getText().equals("Brow&se...");
		return browse;
	}
	
	// name
	public void setName(String name) {
		new LabeledText("Name:").setText(name);
	}

	// resource path
	public void setResourcePath(String resourcePath) {
		new LabeledText("Resource path:").setText(resourcePath);
	}

	// available JAX-RS Resource Method stubs
	public boolean areAvailableJAXRSResourceMethodEnabled() {
		return getFindByIdCheckBox().isEnabled() && getListAll().isEnabled()
				&& getCreate().isEnabled() && getUpdate().isEnabled()
				&& getDeleteById().isEnabled();
	}

	public void setFindById(boolean findById) {
		setCheckBoxState(getFindByIdCheckBox(), findById);
	}

	public void setListAll(boolean listAll) {
		setCheckBoxState(getListAll(), listAll);
	}

	public void setCreate(boolean create) {
		setCheckBoxState(getCreate(), create);
	}

	public void setUpdate(boolean update) {
		setCheckBoxState(getUpdate(), update);
	}

	public void setDeleteById(boolean deleteById) {
		setCheckBoxState(getDeleteById(), deleteById);
	}

	private CheckBox getFindByIdCheckBox() {
		return new CheckBox("findById()");
	}

	private CheckBox getListAll() {
		return new CheckBox("listAll()");
	}

	private CheckBox getCreate() {
		return new CheckBox("create()");
	}

	private CheckBox getUpdate() {
		return new CheckBox("update()");
	}

	private CheckBox getDeleteById() {
		return new CheckBox("deleteById()");
	}

	// configure templates and default values
	public void configureTemplatesAndDefaultValues() {
		throw new UnsupportedOperationException();
	}

	// generate comments
	public void setGenerateComments(boolean generateComments) {
		setCheckBoxState(new CheckBox("Generate comments"), generateComments);
	}

	protected static final void setCheckBoxState(CheckBox checkBox, boolean check) {
		if (checkBox.isChecked() == !check) {
			checkBox.click();
		}
	}
}
