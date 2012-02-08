package org.jboss.tools.ws.jaxrs.ui.internal.utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ModuleArtifactAdapterDelegate;
import org.eclipse.wst.server.core.util.WebResource;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateElement;

/**
 * This class adapts {@link UriPathTemplateElement} into WebResources that Eclipse Run|Debug On Server functionallity can handle.
 * 
 * This allows you to use the Run As functionality on the JAX-RS node elements which represent a path.
 * 
 * @TODO currently only shows up for paths without "{" in them.
 * @author max
 *
 */
public class JaxrsPathModuleArtifactAdapter extends ModuleArtifactAdapterDelegate  {

	@Override
	public IModuleArtifact getModuleArtifact(Object obj) {
		if (obj instanceof UriPathTemplateElement) {
			UriPathTemplateElement element = (UriPathTemplateElement) obj;
			
			//TODO: NPE check this
			IProject project = element.getEndpoint().getJavaProject().getProject();
			
			IModule module = ServerUtil.getModule(project);
			
			if(plainUrl(element)) {
				Path path = new Path(element.getEndpoint().getUriPathTemplate());
			  //TODO: need to take possible @Application context path into consideration!
				return new WebResource(module, path); //$NON-NLS-1$
			}	
		}
		
		return null; 
	}

	// @return true when the path is directly viewable in a browser and its a GET request.
	private boolean plainUrl(UriPathTemplateElement element) {
		//TODO: NPE check this
		return element.getEndpoint().getHttpMethod().getHttpVerb().equalsIgnoreCase("GET") && !element.getEndpoint().getUriPathTemplate().contains("{");
	}

}
