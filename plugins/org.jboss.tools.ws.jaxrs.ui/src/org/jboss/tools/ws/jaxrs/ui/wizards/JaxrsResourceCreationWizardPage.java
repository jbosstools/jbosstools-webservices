/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
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
import static org.jboss.tools.ws.jaxrs.ui.wizards.JaxrsElementCreationUtils.getSimpleName;
import static org.jboss.tools.ws.jaxrs.ui.wizards.JaxrsElementCreationUtils.getSuggestedPackage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateCategory;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

import ui.internal.org.atteo.evo.inflector.English;

/**
 * Page to create a new JAX-RS Resource
 * 
 * @author xcoulon
 *
 */
@SuppressWarnings("restriction")
public class JaxrsResourceCreationWizardPage extends NewClassWizardPage {

	private static class ButtonSelectionListener implements SelectionListener {

		public void selectionChanged(final boolean value) {
		}

		public void widgetSelected(SelectionEvent e) {
			selectionChanged(((Button) e.getSource()).getSelection());
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}

	}

	private static final class MediaTypesLabelProvider implements ILabelProvider {
		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public String getText(Object element) {
			return element.toString();
		}

		@Override
		public Image getImage(Object element) {
			return JBossJaxrsUIPlugin.getDefault().getImage("filter_mapping_in_out.png");
		}
	}

	/** The Target class associated with the JAX-RS Resource to create. */
	private String targetClass = "";
	
	/** The Target classname selection text. */
	private Text targetClassText = null;
	
	/** The Target classname status. */
	private Status targetClassStatus = new Status(IStatus.OK, JBossJaxrsUIPlugin.PLUGIN_ID, null);
	
	/** The button to select a class in the selected project's classpath. */
	private Button browseClassesButton = null;
	
	/** The Resource Path value text. */
	private Text resourcePathText = null;
	
	/** The Resource Path status. */
	private Status resourcePathStatus = new Status(IStatus.OK, JBossJaxrsUIPlugin.PLUGIN_ID, null);
	
	/** Value of the @Path annotation on the JAX-RS Resource class to create. */
	private String resourcePath = "";
	
	/** The Mediatypes text. */
	private TableViewer mediaTypesList = null;

	/** Value of the @Path annotation on the JAX-RS Resource class to create. */
	private java.util.List<String> mediaTypes = new ArrayList<String>();

	/** The button to add mediatypes. */
	private Button addMediaTypesButton = null;

	/** The button to remove mediatypes. */
	private Button removeMediaTypesButton = null;
	
	/** The container for the method stubs selection buttons. */
	private Composite methodStubsContainer = null; 

	/** Flag to create the 'findById()' stub method. Default to {@code true}. */
	private boolean includeFindByIdMethod = false;

	/** Flag to create the 'listAll()' stub method. Default to {@code true}. */
	private boolean includeListAllMethod = false;

	/** Flag to create the 'create()' stub method. Default to {@code true}. */
	private boolean includeCreateMethod = false;

	/** Flag to create the 'update()' stub method. Default to {@code true}. */
	private boolean includeUpdateMethod = false;

	/** Flag to create the 'update()' stub method. Default to {@code true}. */
	private boolean includeDeleteByIdMethod = false;

	/** Java SourceType completion processor for content assist on Text control. */
	private JavaTypeCompletionProcessor targetClassCompletionProcessor = new JavaTypeCompletionProcessor(true, false,
			true);

	/**
	 * Default constructor.
	 */
	public JaxrsResourceCreationWizardPage() {
		super();
		setTitle(JaxrsResourceCreationMessages.JaxrsResourceCreationWizardPage_Title);
		setDescription(JaxrsResourceCreationMessages.JaxrsResourceCreationWizardPage_Description);
	}

	@Override
	public void init(final IStructuredSelection selection) {
		super.init(selection);
		final IJavaElement selectedJavaElement = getInitialJavaElement(selection);
		if (selectedJavaElement instanceof ICompilationUnit) {
			setDefaultValues((ICompilationUnit) selectedJavaElement);
		} else if (selectedJavaElement instanceof IType) {
			setDefaultValues((IType) selectedJavaElement);
		} else if (selection.getFirstElement() instanceof UriPathTemplateCategory) {
			setDefaultValues((UriPathTemplateCategory) selection.getFirstElement());
		}
		// pre-fill the mediaTypes with usual values
		mediaTypes.add("application/xml");
		mediaTypes.add("application/json");
	}

	/**
	 * Sets the extra default values from the given 'compilationUnit' argument
	 * 
	 * @param compilationUnit
	 *            the selected {@link ICompilationUnit}.
	 */
	private void setDefaultValues(final ICompilationUnit compilationUnit) {
		final IType primaryType = compilationUnit.findPrimaryType();
		// if/else statements to avoid looping back/to 'IType' case below
		if (primaryType != null && (getTargetClass() == null || getTargetClass().isEmpty())) {
			setDefaultValues(primaryType);
		}
	}

	/**
	 * Sets the extra default values from the given 'type' argument unless it
	 * already exists
	 * 
	 * @param type
	 *            the selected {@link IType}
	 */
	private void setDefaultValues(final IType type) {
		setTargetClass(type.getFullyQualifiedName());
		setResourcePath(buildResourcePath(type));
		final ICompilationUnit compilationUnit = type.getCompilationUnit();
		final IPackageFragment suggestedPackage = getSuggestedPackage(compilationUnit);
		setPackageFragment(suggestedPackage, true);
		final String typeName = type.getElementName() + "Endpoint";
		final String typeFullyQualifiedName = suggestedPackage.getElementName() + "." + typeName;
		try {
			if (getJavaProject() != null && getJavaProject().findType(typeFullyQualifiedName) == null) {
				setTypeName(typeName, true);
			}
		} catch (JavaModelException e) {
			Logger.error("Failed to check if project contains type '" + typeName + "'", e);
		}
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
	protected IStatus containerChanged() {
		final IPackageFragmentRoot root = getPackageFragmentRoot();
		if (root != null) {
			targetClassCompletionProcessor.setPackageFragment(root.getPackageFragment("")); //$NON-NLS-1$
		}
		return super.containerChanged();
	}

	public void createControl(final Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());

		final int nColumns = 4;

		GridLayout layout = new GridLayout();
		layout.numColumns = nColumns;
		composite.setLayout(layout);

		// pick & choose the wanted UI components
		createContainerControls(composite, nColumns);
		createPackageControls(composite, nColumns);

		createSeparator(composite, nColumns);

		createTargetClassControls(composite);

		createSeparator(composite, nColumns);

		createTypeNameControls(composite, nColumns);

		createResourcePathControls(composite);
		createMediaTypesControls(composite);
		createResourceMethodStubsControls(composite);

		createCommentControls(composite, nColumns);
		enableCommentControl(true);

		setControl(composite);

		Dialog.applyDialogFont(composite);
		doStatusUpdate();

	}

	private void createResourcePathControls(final Composite composite) {
		final Label resourcePathLabel = new Label(composite, SWT.NONE);
		resourcePathLabel.setText("Resource path:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(resourcePathLabel);
		this.resourcePathText = new Text(composite, SWT.BORDER);
		this.resourcePathText.setText(this.resourcePath);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false)
				.applyTo(resourcePathText);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(new Label(composite, SWT.NONE));
		resourcePathText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				onResourcePathChange();
			}
		});

	}

	private void createTargetClassControls(final Composite composite) {
		// target class
		final Label targetClassLabel = new Label(composite, SWT.NONE);
		targetClassLabel.setText("Target entity:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(targetClassLabel);
		this.targetClassText = new Text(composite, SWT.BORDER);
		this.targetClassText.setText(this.targetClass);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false)
				.applyTo(targetClassText);

		this.browseClassesButton = new Button(composite, SWT.NONE);
		this.browseClassesButton.setText("Browse...");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(browseClassesButton);
		this.browseClassesButton.addSelectionListener(onBrowseTargetClasses());

		ControlContentAssistHelper.createTextContentAssistant(targetClassText, targetClassCompletionProcessor);

		targetClassText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				onTargetClassChange();
			}
		});
	}

	private void onTargetClassChange() {
		setTargetClass(this.targetClassText.getText());
		if(this.targetClass == null || this.targetClass.isEmpty() && methodStubsContainer != null) {
			setAllMethodStubsButtonsEnabled(false);
		} else if(this.targetClass != null || !this.targetClass.isEmpty() && methodStubsContainer != null) {
			setAllMethodStubsButtonsEnabled(true);
		}
	}

	private void onResourcePathChange() {
		setResourcePath(this.resourcePathText.getText());
	}

	private void createResourceMethodStubsControls(final Composite composite) {
		final Label methodStubsLabel = new Label(composite, SWT.NONE);
		methodStubsLabel.setText("Which JAX-RS Resource Method stubs would you like to create ?");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(4, 1).grab(true, false)
				.applyTo(methodStubsLabel);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(new Label(composite, SWT.NONE));

		this.methodStubsContainer = new Composite(composite, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(0, 0).applyTo(methodStubsContainer);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 3).grab(true, false)
				.applyTo(methodStubsContainer);

		final Button includeFindByIdMethodButton = new Button(methodStubsContainer, SWT.CHECK);
		includeFindByIdMethodButton.setText("findById()");
		includeFindByIdMethodButton.setSelection(isIncludeFindByIdMethod());
		includeFindByIdMethodButton.addSelectionListener(new ButtonSelectionListener() {
			@Override
			public void selectionChanged(boolean value) {
				JaxrsResourceCreationWizardPage.this.includeFindByIdMethod = value;
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(includeFindByIdMethodButton);

		final Button includeListAllMethodButton = new Button(methodStubsContainer, SWT.CHECK);
		includeListAllMethodButton.setText("listAll()");
		includeListAllMethodButton.setSelection(isIncludeListAllMethod());
		includeListAllMethodButton.addSelectionListener(new ButtonSelectionListener() {
			@Override
			public void selectionChanged(boolean value) {
				JaxrsResourceCreationWizardPage.this.includeListAllMethod = value;
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(includeListAllMethodButton);

		final Button includeCreateMethodButton = new Button(methodStubsContainer, SWT.CHECK);
		includeCreateMethodButton.setText("create()");
		includeCreateMethodButton.setSelection(isIncludeCreateMethod());
		includeCreateMethodButton.addSelectionListener(new ButtonSelectionListener() {
			@Override
			public void selectionChanged(boolean value) {
				JaxrsResourceCreationWizardPage.this.includeCreateMethod = value;
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(includeCreateMethodButton);

		final Button includeUpdateMethodButton = new Button(methodStubsContainer, SWT.CHECK);
		includeUpdateMethodButton.setText("update()");
		includeUpdateMethodButton.setSelection(isIncludeUpdateMethod());
		includeUpdateMethodButton.addSelectionListener(new ButtonSelectionListener() {
			@Override
			public void selectionChanged(boolean value) {
				JaxrsResourceCreationWizardPage.this.includeUpdateMethod = value;
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(includeUpdateMethodButton);

		final Button includeDeleteByIdMethodButton = new Button(methodStubsContainer, SWT.CHECK);
		includeDeleteByIdMethodButton.setText("deleteById()");
		includeDeleteByIdMethodButton.setSelection(isIncludeDeleteByIdMethod());
		includeDeleteByIdMethodButton.addSelectionListener(new ButtonSelectionListener() {
			@Override
			public void selectionChanged(boolean value) {
				JaxrsResourceCreationWizardPage.this.includeDeleteByIdMethod = value;
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(includeDeleteByIdMethodButton);
		if(this.targetClass == null || this.targetClass.isEmpty()) {
			setAllMethodStubsButtonsEnabled(false);
		}
	}

	/**
	 * Enables or disables the method stubs creation button
	 * @param enabled {@code true} to enable, {@code false} otherwise.
	 */
	public void setAllMethodStubsButtonsEnabled(final boolean enabled) {
		if(methodStubsContainer != null) {
			for (Control control : methodStubsContainer.getChildren()) {
				control.setEnabled(enabled);
			}
		}
	}

	private void createMediaTypesControls(final Composite composite) {
		final Label mediaTypesLabel = new Label(composite, SWT.NONE);
		mediaTypesLabel.setText("Media types:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(false, false).applyTo(mediaTypesLabel);
		this.mediaTypesList = new TableViewer(composite, SWT.BORDER + SWT.MULTI);
		this.mediaTypesList.setComparator(new ViewerComparator());
		this.mediaTypesList.setLabelProvider(new MediaTypesLabelProvider());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).span(2, 4)
				.applyTo(mediaTypesList.getControl());
		for (String mediaType : this.mediaTypes) {
			this.mediaTypesList.add(mediaType);
		}
		this.addMediaTypesButton = new Button(composite, SWT.NONE);
		this.addMediaTypesButton.setText("Add...");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(false, false).applyTo(addMediaTypesButton);
		this.addMediaTypesButton.addSelectionListener(onAddMediaTypes());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(new Label(composite, SWT.NONE));

		this.removeMediaTypesButton = new Button(composite, SWT.NONE);
		this.removeMediaTypesButton.setText("Remove");
		// disabled by default, until the user selects at least one
		this.removeMediaTypesButton.setEnabled(false);

		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(false, false).applyTo(removeMediaTypesButton);
		this.removeMediaTypesButton.addSelectionListener(onRemoveMediaTypes());
		// listener to enable/disable the "remove" button if no element is selected in the mediaTypeList
		this.mediaTypesList.addSelectionChangedListener(onMediaTypesSelectionChanged());
	}

	private ISelectionChangedListener onMediaTypesSelectionChanged() {
		return new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				if(event.getSelection().isEmpty()) {
					JaxrsResourceCreationWizardPage.this.removeMediaTypesButton.setEnabled(false);
				} else {
					JaxrsResourceCreationWizardPage.this.removeMediaTypesButton.setEnabled(true);
				}				
			}
		};
	}

	private void doStatusUpdate() {
		// status of all used components
		final IStatus[] status = new IStatus[] { fContainerStatus, fPackageStatus, fTypeNameStatus, fModifierStatus,
				fSuperClassStatus, fSuperInterfacesStatus, resourcePathStatus, targetClassStatus };
		// the most severe status will be displayed and the OK button
		// enabled/disabled.
		updateStatus(status);
	}

	private SelectionListener onBrowseTargetClasses() {
		return new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					final IRunnableContext context = getWizard().getContainer();
					final IJavaSearchScope searchScope = SearchEngine
							.createJavaSearchScope(new IJavaElement[] { getPackageFragmentRoot() });
					final SelectionDialog dialog = JavaUI.createTypeDialog(getShell(), context, searchScope,
							IJavaElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES, false);
					final int result = dialog.open();
					if (result == SelectionDialog.OK) {
						final IType selectedType = (IType) dialog.getResult()[0];
						if (selectedType != null) {
							setDefaultValues(selectedType);
						}
					}
				} catch (JavaModelException e1) {
					Logger.error("Failed to open the source folder selection dialog", e1);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
	}

	private SelectionListener onAddMediaTypes() {
		return new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ElementListSelectionDialog mediaTypesSelectionDialog = new ElementListSelectionDialog(getShell(),
						new MediaTypesLabelProvider());
				mediaTypesSelectionDialog.setTitle("Media Types Selection");
				mediaTypesSelectionDialog
						.setMessage("Select one or more media types to be supported by the JAX-RS Resource:");
				mediaTypesSelectionDialog.setMultipleSelection(true);
				mediaTypesSelectionDialog.setElements(new Object[] { "application/atom+xml",
						"application/x-www-form-urlencoded", "application/octet-stream", "application/xml",
						"application/json", "application/svg+xml", "application/xhtml+xml", "multipart/form-data",
						"text/html", "text/plain", "text/xml", "*", "*/*" });
				final int returnCode = mediaTypesSelectionDialog.open();
				if (returnCode == Dialog.OK) {
					final Object[] selectedMediaTypes = mediaTypesSelectionDialog.getResult();
					for (Object selectedMediaType : selectedMediaTypes) {
						addMediaType((String) selectedMediaType);
					}
				}

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
	}

	private SelectionListener onRemoveMediaTypes() {
		return new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				final IStructuredSelection selectedMediatypes = (IStructuredSelection) JaxrsResourceCreationWizardPage.this.mediaTypesList
						.getSelection();
				for (Iterator<?> selectedMediatypeIterator = selectedMediatypes.iterator(); selectedMediatypeIterator
						.hasNext();) {
					removeMediaType(selectedMediatypeIterator.next().toString());
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
	}

	private void addMediaType(final String selectedMediaType) {
		if (!mediaTypes.contains(selectedMediaType)) {
			this.mediaTypes.add(selectedMediaType);
			Collections.sort(this.mediaTypes);
			this.mediaTypesList.add(selectedMediaType);
		}
	}

	private void removeMediaType(final String selectedMediaType) {
		if (mediaTypes.contains(selectedMediaType)) {
			this.mediaTypes.remove(selectedMediaType);
			this.mediaTypesList.remove(selectedMediaType);
		}
	}

	@Override
	public boolean isPageComplete() {
		return super.isPageComplete();
	}

	/**
	 * @return the resourcePath
	 */
	public String getResourcePath() {
		return this.resourcePath;
	}

	public void setResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
		if (this.resourcePathText != null && this.resourcePathText.getText() != null
				&& !this.resourcePathText.getText().equals(this.resourcePath)) {
			this.resourcePathText.setText(resourcePath);
		}
		if (this.resourcePath == null) {
			this.resourcePathStatus = new Status(IStatus.WARNING, JBossJaxrsUIPlugin.PLUGIN_ID,
					JaxrsResourceCreationMessages.JaxrsResourceCreationWizardPage_EmptyResourcePath);
		} else {
			this.resourcePathStatus = new Status(IStatus.OK, JBossJaxrsUIPlugin.PLUGIN_ID, null);
		}
		doStatusUpdate();
	}

	/**
	 * @return the mediaTypes
	 */
	public java.util.List<String> getMediaTypes() {
		return mediaTypes;
	}

	/**
	 * @return the includeFindByIdMethod
	 */
	public boolean isIncludeFindByIdMethod() {
		return includeFindByIdMethod;
	}

	public void setIncludeFindByIdMethod(boolean includeFindByIdMethod) {
		this.includeFindByIdMethod = includeFindByIdMethod;
	}

	/**
	 * @return the includeListAllMethod
	 */
	public boolean isIncludeListAllMethod() {
		return includeListAllMethod;
	}

	public void setIncludeListAllMethod(boolean includeListAllMethod) {
		this.includeListAllMethod = includeListAllMethod;
	}

	/**
	 * @return the includeCreateMethod
	 */
	public boolean isIncludeCreateMethod() {
		return includeCreateMethod;
	}

	public void setIncludeCreateMethod(boolean includeCreateMethod) {
		this.includeCreateMethod = includeCreateMethod;
	}

	/**
	 * @return the includeUpdateMethod
	 */
	public boolean isIncludeUpdateMethod() {
		return includeUpdateMethod;
	}

	public void setIncludeUpdateMethod(boolean includeUpdateMethod) {
		this.includeUpdateMethod = includeUpdateMethod;
	}

	/**
	 * @return the includeDeleteByIdMethod
	 */
	public boolean isIncludeDeleteByIdMethod() {
		return includeDeleteByIdMethod;
	}

	public void setIncludeDeleteByIdMethod(boolean includeDeleteByIdMethod) {
		this.includeDeleteByIdMethod = includeDeleteByIdMethod;
	}

	/**
	 * @return the targetClass
	 */
	public String getTargetClass() {
		return this.targetClass;
	}

	public void setTargetClass(final String targetClass) {
		if(targetClass == null || targetClass.isEmpty()) {
			setAllMethodStubsButtonsEnabled(false);
		} else {
			setAllMethodStubsButtonsEnabled(true);
		}
		if(targetClass != null && targetClass.equals(this.targetClass)) {
			// skip, because there's no change
			return;
		}
		this.targetClass = targetClass;
		if (this.targetClassText != null && this.targetClassText.getText() != null
				&& !this.targetClassText.getText().equals(this.targetClass)) {
			this.targetClassText.setText(targetClass);
		}
		try {
			if (this.targetClass != null && !this.targetClass.isEmpty() && getJavaProject() != null && getJavaProject().findType(this.targetClass) == null) {
				this.targetClassStatus = new Status(IStatus.ERROR, JBossJaxrsUIPlugin.PLUGIN_ID,
						JaxrsResourceCreationMessages.JaxrsResourceCreationWizardPage_InvalidTargetClass);
			} else {
				this.targetClassStatus = new Status(IStatus.OK, JBossJaxrsUIPlugin.PLUGIN_ID, null);
			}
			doStatusUpdate();
		} catch (JavaModelException e) {
			Logger.error("Unable to validate Target Class", e);
		}
	}

	/**
	 * {@inheritDoc} Adding {@code @Path} and {@code @RequestScoped} annotations
	 * (along with imports) on the created type, then adding selected annotated
	 * method stubs.
	 */
	@Override
	protected void createTypeMembers(final IType newType, final ImportsManager imports, final IProgressMonitor monitor)
			throws CoreException {
		// implement inherited methods but skip the constructor, as the JAX-RS
		// resource is managed by the runtime (the JEE/JAX-RS container)
		createInheritedMethods(newType, false, true, imports, new SubProgressMonitor(monitor, 1));
		// adding JAX-RS related annotations on the created type
		addAnnotation(newType, JaxrsClassnames.PATH, Arrays.asList(this.resourcePath), imports);
		if (getJavaProject() != null && getJavaProject().findType(JaxrsClassnames.REQUEST_SCOPED) != null) {
			addAnnotation(newType, JaxrsClassnames.REQUEST_SCOPED, null, imports);
		}
		// now, add the selected method stubs
		createMethodStubs(newType, imports, new SubProgressMonitor(monitor, 1));
	}

	void createMethodStubs(final IType type, final ImportsManager imports, final IProgressMonitor monitor)
			throws JavaModelException {
		final String targetClassSimpleName = getSimpleName(this.targetClass);
		final String targetClassParamName = targetClassSimpleName.toLowerCase();
		if(this.targetClass != null && !this.targetClass.isEmpty()) {
			imports.addImport(this.targetClass);
		}
		imports.addImport(JaxrsClassnames.RESPONSE);
		imports.addImport(JaxrsClassnames.URI_BUILDER);
		if (isIncludeCreateMethod()) {
			imports.addImport(JaxrsClassnames.POST);
			final String contents = NLS.bind(
					JaxrsResourceCreationMessages.JaxrsResourceCreationWizardPage_CreateMethodSkeleton, new String[] {
							targetClassSimpleName, targetClassParamName });
			final IMethod createdMethod = type.createMethod(contents, null, true, monitor);
			if (!this.mediaTypes.isEmpty()) {
				addAnnotation(createdMethod, JaxrsClassnames.CONSUMES, this.mediaTypes, imports);
			}
			addAnnotation(createdMethod, JaxrsClassnames.POST, null, imports);
			addMethodComments(createdMethod);
		}
		if (isIncludeFindByIdMethod()) {
			imports.addImport(JaxrsClassnames.GET);
			imports.addImport(JaxrsClassnames.RESPONSE_STATUS);
			imports.addImport(JaxrsClassnames.PATH_PARAM);
			final String contents = NLS.bind(
					JaxrsResourceCreationMessages.JaxrsResourceCreationWizardPage_FindByIdMethodSkeleton, new String[] {
							targetClassSimpleName, targetClassParamName });
			final IMethod createdMethod = type.createMethod(contents, null, true, monitor);
			if (!this.mediaTypes.isEmpty()) {
				addAnnotation(createdMethod, JaxrsClassnames.PRODUCES, this.mediaTypes, imports);
			}
			addAnnotation(createdMethod, JaxrsClassnames.PATH, Arrays.asList("/{id:[0-9][0-9]*}"), imports);
			addAnnotation(createdMethod, JaxrsClassnames.GET, null, imports);
			addMethodComments(createdMethod);
		}
		if (isIncludeListAllMethod()) {
			imports.addImport(JaxrsClassnames.GET);
			imports.addImport("java.util.List");
			imports.addImport(JaxrsClassnames.QUERY_PARAM);
			final String contents = NLS.bind(
					JaxrsResourceCreationMessages.JaxrsResourceCreationWizardPage_ListAllMethodSkeleton, new String[] {
							targetClassSimpleName, English.plural(targetClassParamName) });
			final IMethod createdMethod = type.createMethod(contents, null, true, monitor);
			if (!this.mediaTypes.isEmpty()) {
				addAnnotation(createdMethod, JaxrsClassnames.PRODUCES, this.mediaTypes, imports);
			}
			addAnnotation(createdMethod, JaxrsClassnames.GET, null, imports);
			addMethodComments(createdMethod);
		}
		if (isIncludeUpdateMethod()) {
			imports.addImport(JaxrsClassnames.PUT);
			final String contents = NLS.bind(
					JaxrsResourceCreationMessages.JaxrsResourceCreationWizardPage_UpdateMethodSkeleton, new String[] {
							targetClassSimpleName, targetClassParamName });
			final IMethod createdMethod = type.createMethod(contents, null, true, monitor);
			if (!this.mediaTypes.isEmpty()) {
				addAnnotation(createdMethod, JaxrsClassnames.CONSUMES, this.mediaTypes, imports);
			}
			addAnnotation(createdMethod, JaxrsClassnames.PATH, Arrays.asList("/{id:[0-9][0-9]*}"), imports);
			addAnnotation(createdMethod, JaxrsClassnames.PUT, null, imports);
			addMethodComments(createdMethod);
		}
		if (isIncludeDeleteByIdMethod()) {
			imports.addImport(JaxrsClassnames.DELETE);
			imports.addImport(JaxrsClassnames.PATH_PARAM);
			final String contents = NLS.bind(
					JaxrsResourceCreationMessages.JaxrsResourceCreationWizardPage_DeleteByIdMethodSkeleton,
					new String[] { targetClassSimpleName, targetClassParamName });
			final IMethod createdMethod = type.createMethod(contents, null, true, monitor);
			addAnnotation(createdMethod, JaxrsClassnames.PATH, Arrays.asList("/{id:[0-9][0-9]*}"), imports);
			addAnnotation(createdMethod, JaxrsClassnames.DELETE, null, imports);
			addMethodComments(createdMethod);
		}

	}

	/**
	 * Adds the comments on the given method, if the 'Add comments' option was
	 * checked.
	 * 
	 * @param method
	 *            the method on which comments should be added.
	 */
	private void addMethodComments(final IMethod method) {
		if (isAddComments()) {
			try {
				final String lineDelimiter = StubUtility.getLineDelimiterUsed(method.getJavaProject());
				final String comment = CodeGeneration.getMethodComment(method, null, lineDelimiter);
				if (comment != null) {
					final IBuffer buf = method.getCompilationUnit().getBuffer();
					buf.replace(method.getSourceRange().getOffset(), 0, comment);
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}

	}

	/**
	 * @param selectedType
	 * @return
	 */
	public String buildResourcePath(final IType selectedType) {
		return "/" + English.plural(selectedType.getElementName().toLowerCase());
	}

}
