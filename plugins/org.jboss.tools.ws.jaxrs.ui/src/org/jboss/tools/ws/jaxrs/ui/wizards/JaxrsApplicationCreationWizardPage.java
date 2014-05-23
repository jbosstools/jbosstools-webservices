/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.ui.wizards;

import static org.jboss.tools.ws.jaxrs.ui.wizards.JaxrsElementCreationUtils.addAnnotation;
import static org.jboss.tools.ws.jaxrs.ui.wizards.JaxrsElementCreationUtils.getSuggestedPackage;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jst.j2ee.model.IModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.javaee.core.JavaeeFactory;
import org.eclipse.jst.javaee.core.UrlPatternType;
import org.eclipse.jst.javaee.web.ServletMapping;
import org.eclipse.jst.javaee.web.WebApp;
import org.eclipse.jst.javaee.web.WebFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.utils.WtpUtils;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateCategory;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * @author xcoulon
 *
 */
public class JaxrsApplicationCreationWizardPage extends NewClassWizardPage {

	/**
	 * Boolean to configure the visibility of the skipApplicationButton (and its
	 * behaviour).
	 */
	private final boolean canSkipApplicationCreation;

	/** The button to select the web.xml style of JAX-RS Application. */
	private Button createWebxmlApplicationButton = null;

	/** The button to select the Java class style of JAX-RS Application. */
	private Button createJavaApplicationButton = null;

	/** The button to skip the JAX-RS Application creation. */
	private Button skipApplicationButton = null;

	private int applicationMode = APPLICATION_JAVA;

	public static final int APPLICATION_JAVA = 0;

	public static final int APPLICATION_WEB_XML = 1;

	public static final int SKIP_APPLICATION = 2;

	/** The Resource Path value text when choosing the Java Application style. */
	private Text javaApplicationApplicationPathText = null;

	/** The Resource Path status when choosing the web.xml Application style. */
	private Status javaApplicationApplicationPathStatus = new Status(IStatus.OK, JBossJaxrsUIPlugin.PLUGIN_ID, null);

	/**
	 * The Resource Path value text when choosing the web.xml Application style.
	 */
	private Text webxmlApplicationApplicationPathText = null;

	/** The Resource Path status when choosing the Java Application style. */
	private Status webxmlApplicationApplicationPathStatus = new Status(IStatus.OK, JBossJaxrsUIPlugin.PLUGIN_ID, null);

	/** Value of the @Path annotation on the JAX-RS Resource class to create. */
	private String applicationPath = "/rest";

	private Composite javaApplicationControlsContainer;

	private Composite webxmlApplicationControlsContainer;

	/**
	 * flag to indicate that a JAX-RS application already exists in the project.
	 */
	private boolean applicationAlreadyExists = false;

	/**
	 * Constructor
	 * 
	 * @param canSkipApplicationCreation
	 *            {@code true} if the 'Skip JAX-RS Application creation' option
	 *            should be available to the user, {@code false} otherwise.
	 */
	public JaxrsApplicationCreationWizardPage(boolean canSkipApplicationCreation) {
		super();
		setTitle(JaxrsApplicationCreationMessages.JaxrsApplicationCreationWizardPage_Title);
		setDescription(JaxrsApplicationCreationMessages.JaxrsApplicationCreationWizardPage_Description);
		this.canSkipApplicationCreation = canSkipApplicationCreation;

	}

	@Override
	public void init(final IStructuredSelection selection) {
		super.init(selection);
		setDefaultValues(selection);
		setSuperClass(JaxrsClassnames.APPLICATION, true);
	}

	public void setApplicationAlreadyExists(boolean alreadyExists) {
		this.applicationAlreadyExists = alreadyExists;
		setApplicationMode(SKIP_APPLICATION);
	}

	/**
	 * Sets the default values applicable from the given {@code selection}
	 * argument.
	 * 
	 * @param selection
	 *            the first element selected in the Project Explorer when
	 *            calling the Wizard.
	 */
	public void setDefaultValues(final IStructuredSelection selection) {
		setSuperClass(JaxrsClassnames.APPLICATION, false);
		final IJavaElement selectedJavaElement = getInitialJavaElement(selection);
		if (selectedJavaElement instanceof IPackageFragment) {
			setDefaultValues((IPackageFragment) selectedJavaElement);
		} else if (selectedJavaElement instanceof ICompilationUnit) {
			setDefaultValues((ICompilationUnit) selectedJavaElement);
		} else if (selectedJavaElement instanceof IType) {
			setDefaultValues((IType) selectedJavaElement);
		} else if (selection.getFirstElement() instanceof UriPathTemplateCategory) {
			setDefaultValues((UriPathTemplateCategory) selection.getFirstElement());
		}
	}

	/**
	 * Sets the extra default values from the given 'packageFragment' argument
	 * 
	 * @param packageFragment
	 *            the selected {@link IPackageFragment}.
	 */
	private void setDefaultValues(final IPackageFragment packageFragment) {
		final String typeName = "RestApplication";
		final String typeFullyQualifiedName = getPackageFragment().getElementName() + "." + typeName;
		try {
			if (getJavaProject() != null && getJavaProject().findType(typeFullyQualifiedName) == null) {
				setTypeName(typeName, true);
			}
		} catch (JavaModelException e) {
			Logger.error("Failed to check if project contains type '" + typeName + "'", e);
		}
	}

	/**
	 * Sets the extra default values from the given 'compilationUnit' argument
	 * 
	 * @param compilationUnit
	 *            the selected {@link ICompilationUnit}.
	 */
	private void setDefaultValues(final ICompilationUnit compilationUnit) {
		final IPackageFragment selectedPackageFragment = (IPackageFragment) compilationUnit
				.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
		final IPackageFragment suggestedPackage = getSuggestedPackage(selectedPackageFragment);
		setPackageFragment(suggestedPackage, true);
		setDefaultValues(selectedPackageFragment);
	}

	/**
	 * Sets the extra default values from the given 'type' argument unless it
	 * already exists
	 * 
	 * @param type
	 *            the selected {@link IType}
	 */
	private void setDefaultValues(final IType type) {
		setDefaultValues(type.getCompilationUnit());
	}

	/**
	 * Attempts to set the defaults values from the given
	 * {@link UriPathTemplateCategory}
	 * 
	 */
	private void setDefaultValues(final UriPathTemplateCategory category) {
		final IJavaProject javaProject = category.getJavaProject();
		try {
			final IPackageFragmentRoot[] packageFragmentRoots = javaProject.getAllPackageFragmentRoots();
			for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
				if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
					setPackageFragmentRoot(packageFragmentRoot, true);
					break;
				}
			}
		} catch (JavaModelException e) {
			Logger.error("Failed to set the default values from project '" + javaProject.getElementName() + "'", e);
		}
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());

		int nColumns = 1;

		GridLayout layout = new GridLayout();
		layout.numColumns = nColumns;
		composite.setLayout(layout);

		final Label applicationStyleLabel = new Label(composite, SWT.NONE);
		applicationStyleLabel
				.setText(JaxrsApplicationCreationMessages.JaxrsApplicationCreationWizardPage_ApplicationStyle);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(applicationStyleLabel);
		// JAX-RS Application created as a Java class
		createJavaApplicationControls(composite);
		// JAX-RS Application created as a Servlet mapping in web.xml
		createWebxmlApplicationControls(composite);
		// Skip JAX-RS Application creation
		createSkipApplicationControls(composite);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		doStatusUpdate();
		updateApplicationCreationModeControls();
	}

	/**
	 * Overwriting this method to erase the result of the call to the superclass'
	 * {@link NewTypeWizardPage#doStatusUpdate()} method which does not include
	 * all statuses.
	 * 
	 * @param fieldName
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		doStatusUpdate();
	}

	private void doStatusUpdate() {
		final Status applicationAlreadyExistsWarningStatus = new Status(IStatus.WARNING, JBossJaxrsUIPlugin.PLUGIN_ID,
				JaxrsApplicationCreationMessages.JaxrsApplicationCreationWizardPage_ApplicationAlreadyExistsWarning);
		if (applicationMode == APPLICATION_JAVA && !this.applicationAlreadyExists) {
			final IStatus[] status = new IStatus[] { fContainerStatus, fPackageStatus, fTypeNameStatus,
					fModifierStatus, fSuperClassStatus, fSuperInterfacesStatus, javaApplicationApplicationPathStatus };
			updateStatus(status);
		} else if (applicationMode == APPLICATION_JAVA && this.applicationAlreadyExists) {
			final IStatus[] status = new IStatus[] { fContainerStatus, fPackageStatus, fTypeNameStatus,
					fModifierStatus, fSuperClassStatus, fSuperInterfacesStatus, javaApplicationApplicationPathStatus,
					applicationAlreadyExistsWarningStatus };
			updateStatus(status);
		} else if (applicationMode == APPLICATION_WEB_XML && !this.applicationAlreadyExists) {
			final IStatus[] status = new IStatus[] { webxmlApplicationApplicationPathStatus };
			updateStatus(status);
		}
		// warn that a JAX-RS application already exists
		else if (applicationMode == APPLICATION_WEB_XML && !this.applicationAlreadyExists) {
			final IStatus[] status = new IStatus[] { webxmlApplicationApplicationPathStatus,
					applicationAlreadyExistsWarningStatus };
			updateStatus(status);
		} else if (applicationMode == SKIP_APPLICATION && !this.applicationAlreadyExists) {
			final IStatus[] status = new IStatus[] { new Status(IStatus.WARNING, JBossJaxrsUIPlugin.PLUGIN_ID,
					JaxrsApplicationCreationMessages.JaxrsApplicationCreationWizardPage_SkipApplicationCreationWarning) };
			updateStatus(status);
		}
	}

	private void createJavaApplicationControls(Composite composite) {
		this.createJavaApplicationButton = new Button(composite, SWT.RADIO);
		this.createJavaApplicationButton
				.setText(JaxrsApplicationCreationMessages.JaxrsApplicationCreationWizardPage_JavaApplicationCreation);
		this.createJavaApplicationButton.setSelection(true);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(createJavaApplicationButton);
		this.createJavaApplicationButton.addSelectionListener(onSelectJavaApplication());
		// controls for Java application creation
		final int nColumns = 4;
		javaApplicationControlsContainer = new Composite(composite, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(nColumns).applyTo(javaApplicationControlsContainer);
		GridDataFactory.fillDefaults().indent(30, 0).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(javaApplicationControlsContainer);
		createContainerControls(javaApplicationControlsContainer, nColumns);
		createPackageControls(javaApplicationControlsContainer, nColumns);
		createTypeNameControls(javaApplicationControlsContainer, nColumns);
		//createSuperClassControls(javaApplicationControlsContainer, nColumns);

		final Label applicationPathLabel = new Label(javaApplicationControlsContainer, SWT.NONE);
		applicationPathLabel.setText("Application path:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(applicationPathLabel);
		this.javaApplicationApplicationPathText = new Text(javaApplicationControlsContainer, SWT.BORDER);
		this.javaApplicationApplicationPathText.setText(getApplicationPath());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false)
				.applyTo(javaApplicationApplicationPathText);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(new Label(javaApplicationControlsContainer, SWT.NONE));
		javaApplicationApplicationPathText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				onJavaApplicationPathChange();
				doStatusUpdate();
			}
		});

		createCommentControls(javaApplicationControlsContainer, nColumns);
		enableCommentControl(true);
	}

	@Override
	protected IStatus superClassChanged() {
		final IStatus status = super.superClassChanged();
		// if there's already an error, let's go with it
		if (status.getSeverity() == IStatus.ERROR) {
			return status;
		}
		if (getJavaProject() != null && getSuperClass() != null && !getSuperClass().isEmpty()) {
			// check if the selected superclass is a subclass of
			// 'javax.ws.rs.core.Application'
			try {
				final IType selectedSuperClass = getJavaProject().findType(getSuperClass());
				final List<IType> selectedSuperClassHierarchy = JdtUtils.findSupertypes(selectedSuperClass);
				if (selectedSuperClassHierarchy != null) {
					for (IType type : selectedSuperClassHierarchy) {
						if (type.getFullyQualifiedName().equals(JaxrsClassnames.APPLICATION)) {
							return status;
						}
					}
					// no match for 'javax.ws.rs.core.Application', in the
					// hierarchy, let's raise an error
					return new Status(IStatus.ERROR, JBossJaxrsUIPlugin.PLUGIN_ID,
							JaxrsApplicationCreationMessages.JaxrsApplicationCreationWizardPage_IllegalTypeHierarchy);
				}
			} catch (CoreException e) {
				Logger.error("Failed to retrieve type hierarchy for '" + getSuperClass() + "'", e);
			}
		}
		// return the status from the parent class
		return status;
	}
	
	private void onJavaApplicationPathChange() {
		this.applicationPath = this.javaApplicationApplicationPathText.getText();
		if (getApplicationPath() == null) {
			this.javaApplicationApplicationPathStatus = new Status(IStatus.WARNING, JBossJaxrsUIPlugin.PLUGIN_ID,
					JaxrsApplicationCreationMessages.JaxrsApplicationCreationWizardPage_EmptyApplicationPath);
		} else {
			this.javaApplicationApplicationPathStatus = new Status(IStatus.OK, JBossJaxrsUIPlugin.PLUGIN_ID, null);
		}
		doStatusUpdate();
	}

	private SelectionListener onSelectJavaApplication() {
		return new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				JaxrsApplicationCreationWizardPage.this.applicationMode = APPLICATION_JAVA;
				updateApplicationCreationModeControls();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
	}

	private void createWebxmlApplicationControls(Composite composite) {
		this.createWebxmlApplicationButton = new Button(composite, SWT.RADIO);
		this.createWebxmlApplicationButton
				.setText(JaxrsApplicationCreationMessages.JaxrsApplicationCreationWizardPage_WebxmlApplicationCreation);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(createWebxmlApplicationButton);
		this.createWebxmlApplicationButton.addSelectionListener(onSelectWebxmlApplication());
		// controls for web.xml application creation
		final int nColumns = 4;
		webxmlApplicationControlsContainer = new Composite(composite, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(nColumns).applyTo(webxmlApplicationControlsContainer);
		GridDataFactory.fillDefaults().indent(30, 0).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(webxmlApplicationControlsContainer);
		final Label applicationPathLabel = new Label(webxmlApplicationControlsContainer, SWT.NONE);
		applicationPathLabel.setText("Application path:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(applicationPathLabel);
		this.webxmlApplicationApplicationPathText = new Text(webxmlApplicationControlsContainer, SWT.BORDER);
		this.webxmlApplicationApplicationPathText.setText(getApplicationPath());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false)
				.applyTo(webxmlApplicationApplicationPathText);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(new Label(composite, SWT.NONE));
		webxmlApplicationApplicationPathText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				onWebxmlApplicationPathChange();
				doStatusUpdate();
			}
		});
	}

	private void onWebxmlApplicationPathChange() {
		this.applicationPath = this.webxmlApplicationApplicationPathText.getText();
		if (getApplicationPath() == null) {
			this.webxmlApplicationApplicationPathStatus = new Status(IStatus.ERROR, JBossJaxrsUIPlugin.PLUGIN_ID,
					JaxrsApplicationCreationMessages.JaxrsApplicationCreationWizardPage_EmptyApplicationPath);
		} else {
			this.webxmlApplicationApplicationPathStatus = new Status(IStatus.OK, JBossJaxrsUIPlugin.PLUGIN_ID, null);
		}
		doStatusUpdate();
	}

	private SelectionListener onSelectWebxmlApplication() {
		return new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				JaxrsApplicationCreationWizardPage.this.applicationMode = APPLICATION_WEB_XML;
				updateApplicationCreationModeControls();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
	}

	private void createSkipApplicationControls(final Composite composite) {
		this.skipApplicationButton = new Button(composite, SWT.RADIO);
		this.skipApplicationButton
				.setText(JaxrsApplicationCreationMessages.JaxrsApplicationCreationWizardPage_SkipApplicationCreation);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(skipApplicationButton);
		this.skipApplicationButton.addSelectionListener(onSelectSkipApplication());
		if (!this.canSkipApplicationCreation) {
			this.skipApplicationButton.setVisible(false);
		}

	}

	private SelectionListener onSelectSkipApplication() {
		return new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				JaxrsApplicationCreationWizardPage.this.applicationMode = SKIP_APPLICATION;
				updateApplicationCreationModeControls();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
	}

	private void updateApplicationCreationModeControls() {
		this.skipApplicationButton.setSelection(this.applicationMode == SKIP_APPLICATION);
		this.createJavaApplicationButton.setSelection(this.applicationMode == APPLICATION_JAVA);
		this.createWebxmlApplicationButton.setSelection(this.applicationMode == APPLICATION_WEB_XML);
		// enable/disable sub containers, disable status, reset
		for (Control control : javaApplicationControlsContainer.getChildren()) {
			control.setEnabled(this.applicationMode == APPLICATION_JAVA);
		}
		for (Control control : webxmlApplicationControlsContainer.getChildren()) {
			control.setEnabled(this.applicationMode == APPLICATION_WEB_XML);
		}
		doStatusUpdate();
	}

	/**
	 * 
	 * @return the kind of JAX-RS Application that should be created.
	 */
	public int getApplicationMode() {
		return applicationMode;
	}

	public void setApplicationMode(int applicationMode) {
		this.applicationMode = applicationMode;
	}

	/**
	 * @return the JAX-RS Application Path
	 */
	public String getApplicationPath() {
		return applicationPath;
	}

	/**
	 * Creates the JAX-RS Application type or define it in the web.xml,
	 * depending on the user's choice.
	 */
	@Override
	public void createType(final IProgressMonitor monitor) throws CoreException, InterruptedException {
		if (applicationMode == APPLICATION_JAVA) {
			super.createType(monitor);
		} else if (applicationMode == APPLICATION_WEB_XML) {
			createWebXmlApplication(monitor);
		}
	}

	/**
	 * Creates the JAX-RS Application as a servlet-mapping in the web.xml (which
	 * could be created if necessary)
	 * 
	 * @param monitor
	 *            the progress monitor
	 */
	private void createWebXmlApplication(final IProgressMonitor monitor) {
		if (getJavaProject() != null) {
			final IProject project = getJavaProject().getProject();
			final IModelProvider provider = ModelProviderManager.getModelProvider(project);
			provider.modify(new Runnable() {
				@Override
				public void run() {
					Object object = provider.getModelObject();
					if (object instanceof WebApp) {
						final WebApp webApp = (WebApp) object;
						final ServletMapping servletMapping = WebFactory.eINSTANCE.createServletMapping();
						servletMapping.setServletName(JaxrsClassnames.APPLICATION);
						final UrlPatternType urlPattern = JavaeeFactory.eINSTANCE.createUrlPatternType();
						urlPattern.setValue(getApplicationPath());
						servletMapping.getUrlPatterns().add(urlPattern);
						webApp.getServletMappings().add(servletMapping);
						webApp.getServletMappings();
					}

				}
			}, IModelProvider.FORCESAVE);
		}
	}

	public IFile getWebxmlResource() {
		return WtpUtils.getWebDeploymentDescriptor(getJavaProject().getProject());
	}

	/**
	 * {@inheritDoc} Adding {@code @ApplicationPath} annotation (along with
	 * imports) on the created type.
	 */
	@Override
	protected void createTypeMembers(final IType newType, final ImportsManager imports, final IProgressMonitor monitor)
			throws CoreException {
		// implement inherited methods but skip the constructor, as the JAX-RS
		// resource is managed by the runtime (the JEE/JAX-RS container)
		createInheritedMethods(newType, false, true, imports, new SubProgressMonitor(monitor, 1));
		// adding JAX-RS related annotations on the created type
		addAnnotation(newType, JaxrsClassnames.APPLICATION_PATH, Arrays.asList(this.applicationPath), imports);
	}

}
