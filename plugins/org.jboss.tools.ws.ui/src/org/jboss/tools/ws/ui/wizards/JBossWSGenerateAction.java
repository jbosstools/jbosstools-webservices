package org.jboss.tools.ws.ui.wizards;

import java.io.File;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.jboss.tools.ws.creation.core.commands.MergeWebXMLCommand;
import org.jboss.tools.ws.creation.core.commands.ServiceSampleCreationCommand;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.ui.JBossWSUIPlugin;

public class JBossWSGenerateAction implements IWorkbenchWindowActionDelegate {
	private IJavaProject project;
	private static String WEB = "web.xml"; //$NON-NLS-1$
	private static File webFile;

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {
		JBossWSGenerateWizard wizard = new JBossWSGenerateWizard();

		WizardDialog dialog = new WizardDialog(JBossWSUIPlugin.getDefault()
				.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
		dialog.create();
		int result = dialog.open();
		if (result == WizardDialog.OK) {
			ServiceModel model = new ServiceModel();
			model.setWebProjectName(project.getElementName());
			model.addServiceClasses(new StringBuffer().append(
					wizard.getPackageName())
					.append(".").append(wizard.getClassName()).toString()); //$NON-NLS-1$
			model.setServiceName(wizard.getServiceName());
			model.setUpdateWebxml(true);
			model.setCustomPackage(wizard.getPackageName());
			try {
				new MergeWebXMLCommand(model).execute(null, null);
				new ServiceSampleCreationCommand(model).execute(null, null);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		dialog.close();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			project = (IJavaProject) ((IStructuredSelection) selection)
					.getFirstElement();
			if (JavaEEProjectUtilities
					.isDynamicWebProject(project.getProject())) {
				webFile = findFileByPath(project.getProject().getLocation()
						.toOSString());
				if (webFile != null) {
					action.setEnabled(true);
					return;
				}
			}
		}
		action.setEnabled(false);

	}

	private File findFileByPath(String path) {
		File ret = null;
		File folder = new File(path);
		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			for (File file : files) {
				ret = findFileByPath(file.getAbsolutePath());
				if (ret != null) {
					break;
				}
			}
		} else {
			if (WEB.equals(folder.getName())) {
				ret = folder;
			}
		}
		return ret;
	}

}
