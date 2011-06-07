package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/**
 * Container for all elements extending the
 * <code>javax.ws.rs.core.Application</code>. There should be only one per
 * application, so this container will be useful when it comes to detecting
 * "duplicate" classes.
 * 
 * @author xcoulon
 * 
 */
public class Applications extends BaseElementContainer<Application> {

	/**
	 * Default constructor
	 * 
	 * @param metamodel
	 */
	public Applications(Metamodel metamodel) {
		super(metamodel);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return
	 * 
	 * @throws InvalidModelElementException
	 */
	@Override
	public List<Application> addFrom(IJavaElement scope, IProgressMonitor progressMonitor) throws CoreException {
		IType applicationType = JdtUtils.resolveType(javax.ws.rs.core.Application.class.getName(),
				metamodel.getJavaProject(), progressMonitor);
		ITypeHierarchy applicationTypeHierarchy = JdtUtils
				.resolveTypeHierarchy(applicationType, false, progressMonitor);
		IType[] subtypes = applicationTypeHierarchy.getAllSubtypes(applicationType);
		List<Application> addedApps = new ArrayList<Application>();
		if (subtypes.length > 1) {
			List<String> s = new ArrayList<String>();
			for (IType t : subtypes) {
				s.add(t.getElementName());
			}
			Logger.error("Multiple subclasses of '" + Application.class.getName()
					+ " were found, but only one should be defined: " + s);
		}
		for (IType t : subtypes) {
			Application application = new Application.Builder(t, metamodel).build(progressMonitor);
			elements.put(t.getElementName(), application);
			addedApps.add(application);
		}
		return addedApps;
	}
}
