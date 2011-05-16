package org.jboss.tools.ws.creation.ui.utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;

public class JBossCreationUIUtils {

	public static Combo createSourceCombo(Composite parent, final ServiceModel model) {
		final Combo outputDirCombo = new Combo(parent, SWT.READ_ONLY);
        outputDirCombo.setToolTipText(JBossWSCreationCoreMessages.Tooltip_SourceFolder);
        outputDirCombo.addListener(SWT.Modify, new Listener(){
			public void handleEvent(Event arg0) {
                String javaSourceFolder = outputDirCombo.getText();
                model.setJavaSourceFolder(javaSourceFolder);	
			}
        	
        });

        populateSourceFolderCombo(outputDirCombo, model.getWebProjectName());
        return outputDirCombo;
	}
	
    public static void populateSourceFolderCombo(Combo outputDirCombo, String projectName) {
        outputDirCombo.removeAll();
        try {
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            IPackageFragmentRoot[] packageFragmentRoots = JavaCore.create(project).getAllPackageFragmentRoots();
            for (int i = 0; i < packageFragmentRoots.length; i++) {
                IPackageFragmentRoot packageFragmentRoot = packageFragmentRoots[i];
                if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    outputDirCombo.add(packageFragmentRoot.getResource().getFullPath().toOSString());
                }
            }
            outputDirCombo.select(0);
        } catch (JavaModelException jme) {
            // catch it
        }
    }

	public static void createSourceComboLabel(Composite configCom) {
        final Label srcDirLabel = new Label(configCom, SWT.NONE);
        srcDirLabel.setText(JBossWSCreationCoreMessages.Label_SourceFolder_Name);
        srcDirLabel.setToolTipText(JBossWSCreationCoreMessages.Tooltip_SourceFolder);
	}

	public static void createSourceComboItem(Composite configCom,
			Combo sourceCombo, ServiceModel model) {
		JBossCreationUIUtils.createSourceComboLabel(configCom);
		sourceCombo = JBossCreationUIUtils.createSourceCombo(configCom, model);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
        sourceCombo.setLayoutData(gd);
	}
}
