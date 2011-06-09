package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.utils.ResourceMethodAnnotatedParameter;

public class RouteEndpoint implements Comparable<RouteEndpoint> {

	private HTTPMethod httpMethod = null;

	private String uriPathTemplate = null;

	private final MediaTypeCapabilities consumedMediaTypes;

	private final MediaTypeCapabilities producedMediaTypes;

	private final Route route;

	public RouteEndpoint(Route route) throws InvalidModelElementException {
		this.route = route;
		consumedMediaTypes = new MediaTypeCapabilities(null);
		producedMediaTypes = new MediaTypeCapabilities(null);
		merge();
	}

	public void merge() throws InvalidModelElementException {
		this.httpMethod = computeHttpMethod(route.getResourceMethods());
		this.uriPathTemplate = computeUriPathTemplate(route.getResourceMethods());
		this.consumedMediaTypes.merge(computeConsumedMediaTypes(route.getResourceMethods()));
		this.producedMediaTypes.merge(computeProducedMediaTypes(route.getResourceMethods()));
	}

	/**
	 * @return the httpMethod
	 */
	public HTTPMethod getHttpMethod() {
		return httpMethod;
	}

	/**
	 * @return the uriPathTemplate
	 */
	public String getUriPathTemplate() {
		return uriPathTemplate;
	}

	/**
	 * @return the consumedMediaTypes
	 */
	public MediaTypeCapabilities getConsumedMediaTypes() {
		return consumedMediaTypes;
	}

	/**
	 * @return the producedMediaTypes
	 */
	public MediaTypeCapabilities getProducedMediaTypes() {
		return producedMediaTypes;
	}

	public boolean matches(HTTPMethod otherHttpMethod, String otherUriPathTemplate, String otherConsumes,
			String otherProduces) {
		if (otherHttpMethod != null && !this.httpMethod.getHttpVerb().equals(otherHttpMethod.getHttpVerb())) {
			return false;
		}
		if (otherUriPathTemplate != null && !this.uriPathTemplate.equals(otherUriPathTemplate)) {
			return false;
		}
		if (otherConsumes != null && !otherConsumes.equals("*/*") && !this.consumedMediaTypes.contains(otherConsumes)) {
			return false;
		}
		if (otherProduces != null && !otherProduces.equals("*/*") && !this.producedMediaTypes.contains(otherProduces)) {
			return false;
		}

		return true;
	}

	private static HTTPMethod computeHttpMethod(LinkedList<ResourceMethod> resourceMethods)
			throws InvalidModelElementException {
		for (Iterator<ResourceMethod> iterator = resourceMethods.descendingIterator(); iterator.hasNext();) {
			ResourceMethod resourceMethod = iterator.next();
			HTTPMethod h = resourceMethod.getMapping().getHTTPMethod();
			if (h != null) {
				return h;
			}
		}
		Logger.debug("No HttpMethod annotation found for this endpoint: " + resourceMethods);
		return null;

	}

	private static String computeUriPathTemplate(LinkedList<ResourceMethod> resourceMethods) {
		StringBuffer templateBuffer = new StringBuffer();
		for (Iterator<ResourceMethod> iterator = resourceMethods.iterator(); iterator.hasNext();) {
			ResourceMethod resourceMethod = iterator.next();
			Resource resource = resourceMethod.getParentResource();
			if (resource.isRootResource()) {
				templateBuffer.append("/").append(resource.getMetamodel().getServiceUri());
			}
			if (resource.getMapping().getUriPathTemplateFragment() != null) {
				templateBuffer.append("/").append(resource.getMapping().getUriPathTemplateFragment());
			}
			if (resourceMethod.getMapping().getUriPathTemplateFragment() != null) {
				templateBuffer.append("/").append(resourceMethod.getMapping().getUriPathTemplateFragment());
			}
		}
		ResourceMethod lastMethod = resourceMethods.getLast();
		List<ResourceMethodAnnotatedParameter> queryParams = lastMethod.getMapping().getQueryParams();
		if (queryParams != null && !queryParams.isEmpty()) {
			templateBuffer.append("?");
			for (Iterator<ResourceMethodAnnotatedParameter> queryParamIterator = queryParams.iterator(); queryParamIterator
					.hasNext();) {
				ResourceMethodAnnotatedParameter queryParam = queryParamIterator.next();
				templateBuffer.append(queryParam.getAnnotationValue()).append("={")
						.append(queryParam.getParameterType()).append("}");
				if (queryParamIterator.hasNext()) {
					templateBuffer.append("&");
				}

			}

		}
		return templateBuffer.toString().replaceAll("/\\*", "/").replaceAll("///", "/").replaceAll("//", "/");

	}

	private static MediaTypeCapabilities computeConsumedMediaTypes(LinkedList<ResourceMethod> resourceMethods) {
		for (Iterator<ResourceMethod> iterator = resourceMethods.descendingIterator(); iterator.hasNext();) {
			ResourceMethod resourceMethod = iterator.next();
			MediaTypeCapabilities mediaTypeCapabilities = resourceMethod.getMapping().getConsumedMediaTypes();
			if (!mediaTypeCapabilities.isEmpty()) {
				return mediaTypeCapabilities;
			}

			Resource parentResource = resourceMethod.getParentResource();
			mediaTypeCapabilities = parentResource.getMapping().getConsumedMediaTypes();
			if (!mediaTypeCapabilities.isEmpty()) {
				return mediaTypeCapabilities;
			}
		}
		return new MediaTypeCapabilities(resourceMethods.getLast().getJavaElement(), Arrays.asList("*/*"));
	}

	private static MediaTypeCapabilities computeProducedMediaTypes(LinkedList<ResourceMethod> resourceMethods) {
		for (Iterator<ResourceMethod> iterator = resourceMethods.descendingIterator(); iterator.hasNext();) {
			ResourceMethod resourceMethod = iterator.next();
			MediaTypeCapabilities mediaTypeCapabilities = resourceMethod.getMapping().getProcucedMediaTypes();
			if (!mediaTypeCapabilities.isEmpty()) {
				return mediaTypeCapabilities;
			}

			Resource parentResource = resourceMethod.getParentResource();
			mediaTypeCapabilities = parentResource.getMapping().getProcucedMediaTypes();
			if (!mediaTypeCapabilities.isEmpty()) {
				return mediaTypeCapabilities;
			}
		}
		return new MediaTypeCapabilities(resourceMethods.getLast().getJavaElement(), Arrays.asList("*/*"));
	}

	@Override
	public final int compareTo(final RouteEndpoint other) {
		int u = uriPathTemplate.compareTo(other.getUriPathTemplate());
		if (u != 0) {
			return u;
		}
		int h = httpMethod.compareTo(other.getHttpMethod());
		if (h != 0) {
			return h;
		}
		int c = consumedMediaTypes.compareTo(other.getConsumedMediaTypes());
		if (c != 0) {
			return c;
		}
		return producedMediaTypes.compareTo(other.getProducedMediaTypes());
	}

	/**
	 * {inheritDoc
	 */
	@Override
	public final String toString() {
		StringBuffer buffer = new StringBuffer();
		if (httpMethod != null) {
			buffer.append(httpMethod.getHttpVerb());
			buffer.append(" ");
		}
		if (uriPathTemplate != null) {
			buffer.append(uriPathTemplate);
			buffer.append(" ");
		}
		buffer.append("{Consumes:").append(consumedMediaTypes).append(" Produces: ").append(producedMediaTypes)
				.append("}");
		return buffer.toString();
	}

}
