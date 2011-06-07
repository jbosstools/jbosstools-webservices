package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JAXRSAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ObjectUtils;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.utils.ResourceMethodAnnotatedParameter;

public class ResourceMapping {

	private final Resource resource;

	/**
	 * the default consumed media type capabilities offered by this resource.
	 * May be overridden at method level
	 */
	private final MediaTypeCapabilities consumedMediaTypes;

	/**
	 * the default produced media type capabilities offered by this resource.
	 * May be overridden at method level
	 */
	private final MediaTypeCapabilities producedMediaTypes;

	/**
	 * The URI Path Template Fragment resolve from the type-level
	 * <code>javax.ws.rs.Path</code> annotation.
	 */
	private String uriPathTemplateFragment = null;

	private final List<ResourceMethodAnnotatedParameter> pathParams = null;

	private final List<ResourceMethodAnnotatedParameter> queryParams = null;

	/**
	 * Full constructor using the inner 'MediaTypeCapabilitiesBuilder' static
	 * class.
	 * 
	 * @param builder
	 */
	public ResourceMapping(final Resource resource) {
		this.resource = resource;
		this.consumedMediaTypes = new MediaTypeCapabilities(resource.getJavaElement());
		this.producedMediaTypes = new MediaTypeCapabilities(resource.getJavaElement());
	}

	/**
	 * @return the resourceMethod
	 */
	public Resource getResource() {
		return resource;
	}

	/**
	 * @param javaMethod
	 * @param compilationUnit
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	public Set<EnumElementChange> merge(CompilationUnit compilationUnit) throws JavaModelException, CoreException {
		Set<EnumElementChange> changes = new HashSet<EnumElementChange>();
		IType javaType = resource.getJavaElement();
		// resource method
		String newValue = (String) JdtUtils.resolveAnnotationAttributeValue(javaType, compilationUnit, Path.class,
				"value");
		if (ObjectUtils.compare(uriPathTemplateFragment, newValue)) {
			if (uriPathTemplateFragment == null || newValue == null) {
				changes.add(EnumElementChange.KIND);
			} else {
				changes.add(EnumElementChange.MAPPING);
			}
			this.uriPathTemplateFragment = newValue;
		}
		if (this.consumedMediaTypes.merge(JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(javaType,
				compilationUnit, Consumes.class))) {
			changes.add(EnumElementChange.MAPPING);
		}
		if (this.producedMediaTypes.merge(JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(javaType,
				compilationUnit, Produces.class))) {
			changes.add(EnumElementChange.MAPPING);
		}
		return changes;
	}

	/*
	 * private static final String resolveBaseURIPathTemplate(Resource resource)
	 * { if (resource.isRootResource()) { return uriPathTemplateBufferTemplate;
	 * } return new StringBuffer(resolveBaseURIPathTemplate(resource.get));
	 * uriPathTemplateBuffer.append("/").append(resourceUriPathTemplate); return
	 * uriPathTemplateBuffer.toString(); }
	 * 
	 * private static String computeFullUriPathTemplate(ResourceMethodMapping
	 * mapping) { String uriPathTemplate = mapping.getUriPathTemplateFragment();
	 * List<ResourceMethodAnnotatedParameter> queryParams =
	 * mapping.getQueryParams(); String baseURIPathTemplate =
	 * resolveBaseURIPathTemplate
	 * (mapping.getResourceMethod().getParentResource()); StringBuffer
	 * uriPathTemplateBuffer = new StringBuffer(baseURIPathTemplate);
	 * uriPathTemplateBuffer.append(uriPathTemplate); if (queryParams != null &&
	 * !queryParams.isEmpty()) { uriPathTemplateBuffer.append("?"); for
	 * (Iterator<ResourceMethodAnnotatedParameter> queryParamIterator =
	 * queryParams.iterator(); queryParamIterator .hasNext();) {
	 * ResourceMethodAnnotatedParameter queryParam = queryParamIterator.next();
	 * uriPathTemplateBuffer
	 * .append(queryParam.getAnnotationValue()).append("={")
	 * .append(queryParam.getParameterType()).append("}"); if
	 * (queryParamIterator.hasNext()) { uriPathTemplateBuffer.append("&"); }
	 * 
	 * }
	 * 
	 * } return uriPathTemplateBuffer.toString().replaceAll("/\\*",
	 * "/").replaceAll("///", "/").replaceAll("//", "/"); }
	 */

	/**
	 * @return the uriPathTemplateFragment
	 */
	public final String getUriPathTemplateFragment() {
		return uriPathTemplateFragment;
	}

	@Override
	public final String toString() {
		StringBuffer buffer = new StringBuffer();
		String uriPathTemplate = getUriPathTemplateFragment();
		if (uriPathTemplate != null) {
			buffer.append(uriPathTemplate);
			buffer.append(" ");
		}
		buffer.append("{consumes:").append(consumedMediaTypes).append(" produces:").append(producedMediaTypes)
				.append("}");
		return buffer.toString();
	}

	/**
	 * @return the Consumed MediaTypes
	 */
	public final MediaTypeCapabilities getConsumedMediaTypes() {
		return consumedMediaTypes;
	}

	/**
	 * @return the Produced MediaTypes
	 */
	public final MediaTypeCapabilities getProcucedMediaTypes() {
		return producedMediaTypes;
	}

	/**
	 * @return the queryParams
	 */
	public final List<ResourceMethodAnnotatedParameter> getQueryParams() {
		return queryParams;
	}

	/**
	 * @return the pathParams
	 */
	public final List<ResourceMethodAnnotatedParameter> getPathParams() {
		return pathParams;
	}

}
