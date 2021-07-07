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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.search;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_ANNOTATION_NAME;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_BUILT_IN_HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_COMPILATION_UNIT_IDENTIFIER;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_CONSUMED_MEDIA_TYPE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_HTTP_VERB;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_IDENTIFIER;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_JAVA_APPLICATION;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_JAVA_APPLICATION_OVERRIDEN;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_JAVA_CLASS_NAME;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_JAVA_ELEMENT;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_JAVA_PROJECT_IDENTIFIER;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_JAXRS_ELEMENT;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_MARKER_IDENTIFIER;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_PACKAGE_FRAGMENT_ROOT_IDENTIFIER;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_PARENT_IDENTIFIER;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_PRODUCED_MEDIA_TYPE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_PROVIDER_KIND;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_RESOURCE_PATH;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_RETURNED_TYPE_NAME;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_TYPE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_URI_PATH_TEMPLATE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_WEBXML_APPLICATION;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.LuceneFields.FIELD_WEBXML_APPLICATION_OVERRIDES_JAVA_APPLICATION;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiBits;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.Bits;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParamConverterProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregator;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorProperty;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceProperty;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;

/**
 * Generated Lucene {@link Document} from the JAX-RS Elements
 * 
 * @author xcoulon
 * 
 */
public class LuceneDocumentFactory {
	
	private static final FieldType STORED_NOT_ANALYZED = new FieldType(TextField.TYPE_STORED);
	
	static {
		STORED_NOT_ANALYZED.setTokenized(false);
	}

	/**
	 * Returns the Identifier {@link Term} for the given {@link IMarker}.
	 * 
	 * @param marker
	 *            the marker to identify in the index
	 * @return the identifier term
	 */
	public static Term getIdentifierTerm(final IMarker marker) {
		return new Term(FIELD_MARKER_IDENTIFIER, getIdentifierValue(marker));
	}
	
	/**
	 * Returns the search {@link Term} for the given className.
	 * 
	 * @param className
	 *            the fully qualified name of the underlying Java Class.
	 * @return the search term
	 */
	public static Term getJavaClassNameTerm(final String className) {
		return new Term(FIELD_JAVA_CLASS_NAME, className);
	}

	/**
	 * Returns the search {@link Term} for the given className.
	 * 
	 * @param className
	 *            the fully qualified name of the underlying Java Class.
	 * @return the search term
	 */
	public static Term getElementCategoryTerm(final EnumElementCategory category) {
		return new Term(FIELD_TYPE, category.toString());
	}
	

	/**
	 * Returns the identifier value to use in index {@link Field} and query
	 * {@link Term}s for the given {@link IMarker}.
	 * 
	 * @param marker
	 *            the marker to identify
	 * @return the identifier value
	 */
	private static String getIdentifierValue(final IMarker marker) {
		return IndexedObjectType.PROBLEM_MARKER.getPrefix() + Long.toString(marker.getId());
	}
	
	/**
	 * Returns the Identifier {@link Term} for the given {@link IJaxrsElement}.
	 * 
	 * @param element
	 *            the element to identify in the index
	 * @return the identifier term
	 */
	public static Term getIdentifierTerm(final IJaxrsElement element) {
		return new Term(FIELD_IDENTIFIER, getIdentifierValue(element));
	}

	/**
	 * Returns the identifier value to use in index {@link Field} and query
	 * {@link Term}s for the given {@link IJaxrsElement}.
	 * 
	 * @param element
	 *            the element to identify
	 * @return the identifier value
	 */
	private static String getIdentifierValue(final IJaxrsElement element) {
		return IndexedObjectType.JAX_RS_ELEMENT.getPrefix() + element.getIdentifier();
	}
	
	/**
	 * Returns the Identifier {@link Term} for the given {@link IJavaElement}.
	 * 
	 * @param element
	 *            the element to identify in the index
	 * @return the identifier term
	 */
	public static Term getIdentifierTerm(final IJavaElement element) {
		return new Term(FIELD_IDENTIFIER, getIdentifierValue(element));
	}
	
	/**
	 * Returns the identifier value to use in index {@link Field} and query
	 * {@link Term}s for the given {@link IJavaElement}.
	 * 
	 * @param element
	 *            the element to identify
	 * @return the identifier value
	 */
	private static String getIdentifierValue(final IJavaElement element) {
		return IndexedObjectType.JAX_RS_ELEMENT.getPrefix() + element.getHandleIdentifier();
	}
	
	/**
	 * Returns the Identifier {@link Term} for the given {@link IJaxrsEndpoint}. 
	 * 
	 * @param endpoint the endpoint to identify in the index
	 * @return the identifier term
	 */
	public static Term getIdentifierTerm(final IJaxrsEndpoint endpoint) {
		return new Term(FIELD_IDENTIFIER, getIdentifierValue(endpoint));
	}
	
	/**
	 * Returns the identifier value to use in index {@link Field} and query
	 * {@link Term}s for the given {@link IJaxrsEndpoint}.
	 * 
	 * @param endpoint
	 *            the endpoint to identify
	 * @return the identifier value
	 */
	public static String getIdentifierValue(final IJaxrsEndpoint endpoint) {
		return IndexedObjectType.JAX_RS_ENDPOINT.getPrefix() + endpoint.getIdentifier();
	}
	
	public static Term getResourcePathTerm(final IResource resource) {
		return new Term(FIELD_RESOURCE_PATH, resource.getFullPath().toPortableString());
	}
	
	public static Term getMarkerTypeTerm() {
		return new Term(FIELD_TYPE, IMarker.class.getSimpleName());
	}

	
	/**
	 * Creates a Lucene {@link Document} from the given {@link IJaxrsElement}.
	 * 
	 * @param element
	 * @return the Lucene document or null if the given element is supposed to
	 *         be indexed.
	 */
	public static Document createDocument(final IJaxrsElement element) {
		switch (element.getElementKind().getCategory()) {
		case APPLICATION:
			if (((IJaxrsApplication) element).isJavaApplication()) {
				return createJavaApplicationDocument((JaxrsJavaApplication) element);
			} else {
				return createWebxmlApplicationDocument((JaxrsWebxmlApplication) element);
			}
		case HTTP_METHOD:
			return createHttpMethodDocument((JaxrsHttpMethod) element);
		case NAME_BINDING:
			return createNameBindingDocument((JaxrsNameBinding) element);
		case PARAM_CONVERTER_PROVIDER:
			return createParamConverterProviderDocument((JaxrsParamConverterProvider) element);
		case PROVIDER:
			return createProviderDocument((JaxrsProvider) element);
		case RESOURCE:
			return createResourceDocument((JaxrsResource) element);
		case RESOURCE_FIELD:
			return createResourceFieldDocument((JaxrsResourceField) element);
		case RESOURCE_PROPERTY:
			return createResourcePropertyDocument((JaxrsResourceProperty) element);
		case RESOURCE_METHOD:
			return createResourceMethodDocument((JaxrsResourceMethod) element);
		case PARAMETER_AGGREGATOR:
			return createParameterAggregatorDocument((JaxrsParameterAggregator) element);
		case PARAMETER_AGGREGATOR_FIELD:
			return createParameterAggregatorFieldDocument((JaxrsParameterAggregatorField) element);
		case PARAMETER_AGGREGATOR_PROPERTY:
			return createParameterAggregatorPropertyDocument((JaxrsParameterAggregatorProperty) element);
		case ENDPOINT:
		case UNDEFINED:
			break;
		
		default:
			break;
		}
		return null;
	}
	
	/**
	 * Creates a Lucene document from the given {@link IJaxrsEndpoint}.
	 * 
	 * @param element the JAX-RS endpoint to index.
	 * @return the Lucene document used to index the given JAX-RS endpoint.
	 */
	public static Document createDocument(final IJaxrsEndpoint endpoint) {
		final Document document = new Document();
		addFieldToDocument(document, FIELD_IDENTIFIER, getIdentifierValue(endpoint));
		addFieldToDocument(document, FIELD_JAVA_PROJECT_IDENTIFIER, getHandleIdentifier(endpoint.getJavaProject()));
		addFieldToDocument(document, FIELD_TYPE, endpoint.getElementCategory().toString());
		addFieldToDocument(document, FIELD_CONSUMED_MEDIA_TYPE, endpoint.getConsumedMediaTypes());
		addFieldToDocument(document, FIELD_PRODUCED_MEDIA_TYPE, endpoint.getProducedMediaTypes());
		addFieldToDocument(document, FIELD_URI_PATH_TEMPLATE, endpoint.getUriPathTemplate());
		if(endpoint.getHttpMethod() != null) {
			addFieldToDocument(document, FIELD_HTTP_VERB, endpoint.getHttpMethod().getHttpVerb());
		}
		if(endpoint.getApplication() != null) {
			addFieldToDocument(document, FIELD_JAXRS_ELEMENT, endpoint.getApplication().getIdentifier());
		}
		addFieldToDocument(document, FIELD_JAXRS_ELEMENT, endpoint.getHttpMethod().getIdentifier());
		for(IJaxrsResourceMethod resourceMethod : endpoint.getResourceMethods()) {
			addFieldToDocument(document, FIELD_JAXRS_ELEMENT, resourceMethod.getIdentifier());
			addFieldToDocument(document, FIELD_JAVA_ELEMENT, resourceMethod.getJavaElement().getHandleIdentifier());
			final JaxrsResource parentResource = (JaxrsResource) resourceMethod.getParentResource();
			addFieldToDocument(document, FIELD_JAXRS_ELEMENT, parentResource.getIdentifier());
			addFieldToDocument(document, FIELD_JAVA_ELEMENT, parentResource.getJavaElement().getHandleIdentifier());
			for(JaxrsResourceField resourceField : parentResource.getAllFields()) {
				addFieldToDocument(document, FIELD_JAXRS_ELEMENT, resourceField.getIdentifier());
				addFieldToDocument(document, FIELD_JAVA_ELEMENT, resourceField.getJavaElement().getHandleIdentifier());
			}
			for(JaxrsResourceProperty resourceProperty : parentResource.getAllProperties()) {
				addFieldToDocument(document, FIELD_JAXRS_ELEMENT, resourceProperty.getIdentifier());
				addFieldToDocument(document, FIELD_JAVA_ELEMENT, resourceProperty.getJavaElement().getHandleIdentifier());
			}
		}
		return document;
	}
	
	public static boolean isDeleted(IndexReader reader, int docID) {
		boolean result = false;
		Bits bits = MultiBits.getLiveDocs(reader);
		if (bits != null) {
			result = !bits.get(docID);
		}
		return result;
	}

	/**
	 * Adds a Field to the given Lucene Document. The generated field will
	 * be stored in the index but NOT analysed.
	 * 
	 * @param document
	 *            the lucene document. Not null
	 * @param name
	 *            the name of the field to add to the document. Not null.
	 * @param value
	 *            the value of the filed to add to the document. Not null.
	 */
	private static void addFieldToDocument(final Document document, final String name, final String value) {
		// skip invalid name/value pairs
		if (name == null || value == null) {
			return;
		}
		document.add(new Field(name, value, STORED_NOT_ANALYZED));
	}

	/**
	 * Adds a Field to the given Lucene Document. The generated field will
	 * be stored in the index but NOT analysed.
	 * 
	 * @param document
	 *            the lucene document. Not null
	 * @param name
	 *            the name of the field to add to the document. Not null.
	 * @param value
	 *            the values of the filed to add to the document. Not null.
	 */
	private static void addFieldToDocument(final Document document, final String name, final Collection<String> values) {
		// skip invalid name/value pairs
		if (name == null || values == null || values.isEmpty()) {
			return;
		}
		for(String value : values) {
			document.add(new Field(name, value, STORED_NOT_ANALYZED));
		}
	}
	
	/**
	 * Returns the given {@link IJavaElement#getHandleIdentifier()} value or
	 * <code>null</null> if the given javaElement was null;
	 * 
	 * @param javaElement
	 * @return the given element's handleIdentifier or null;
	 */
	private static String getHandleIdentifier(final IJavaElement javaElement) {
		if (javaElement == null) {
			return null;
		}
		return javaElement.getHandleIdentifier();
	}

	/**
	 * Initialize a base Lucene Document for the given JAX-RS element.
	 * 
	 * @param element
	 * @return a base document, to be completed with specific fields within the
	 *         calling method.
	 */
	private static Document createBaseDocument(final JaxrsJavaElement<?> element) {
		final Document document = new Document();
		addFieldToDocument(document, FIELD_JAVA_PROJECT_IDENTIFIER, getHandleIdentifier(element.getMetamodel().getJavaProject()));
		addFieldToDocument(document, FIELD_TYPE, element.getElementKind().getCategory().toString());
		addFieldToDocument(document, FIELD_IDENTIFIER, getIdentifierValue(element));
		if (element.getJavaElement() != null) {
			addFieldToDocument(document, FIELD_JAVA_ELEMENT, Boolean.TRUE.toString());
			addFieldToDocument(document, FIELD_COMPILATION_UNIT_IDENTIFIER, getHandleIdentifier(element.getJavaElement()
					.getAncestor(IJavaElement.COMPILATION_UNIT)));
			addFieldToDocument(document, FIELD_PACKAGE_FRAGMENT_ROOT_IDENTIFIER, getHandleIdentifier(element.getJavaElement()
					.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT)));
			if (element.getJavaElement().getElementType() == IJavaElement.TYPE) {
				addFieldToDocument(document, FIELD_JAVA_CLASS_NAME,
						((IType) element.getJavaElement()).getFullyQualifiedName());
				// only applies to JAX-RS element associated with an IType or IAnnotation (not IMethod nor IField)
				if (element.getResource() != null) {
					addFieldToDocument(document, FIELD_RESOURCE_PATH, element.getResource().getFullPath().toPortableString());
				}
			}
		}
		for (Entry<String, Annotation> entry : element.getAnnotations().entrySet()) {
			addFieldToDocument(document, FIELD_ANNOTATION_NAME, entry.getValue().getFullyQualifiedName());
			addFieldToDocument(document, FIELD_PARENT_IDENTIFIER, getHandleIdentifier(entry.getValue().getJavaParent()));
		}
		return document;
	}

	/**
	 * Initializes a Lucene Document for the given JAX-RS Java-based
	 * Application.
	 * 
	 * @param httpMethod
	 *            the JAX-RS Java-based Application.
	 * @return the document.
	 */
	private static Document createJavaApplicationDocument(final JaxrsJavaApplication javaApplication) {
		final Document document = createBaseDocument(javaApplication);
		addFieldToDocument(document, FIELD_APPLICATION_PATH, javaApplication.getApplicationPath());
		addFieldToDocument(document, FIELD_JAVA_APPLICATION, Boolean.TRUE.toString());
		addFieldToDocument(document, FIELD_JAVA_APPLICATION_OVERRIDEN, Boolean.toString(javaApplication.isOverriden()));
		addFieldToDocument(document, FIELD_RESOURCE_PATH, javaApplication.getResource().getFullPath().toPortableString());
		return document;
	}
	
	/**
	 * Initializes a Lucene Document for the given JAX-RS Parameter Aggregator.
	 * 
	 * @param element
	 *            the JAX-RS Parameter Aggregator.
	 * @return the document.
	 */
	private static Document createParameterAggregatorDocument(final JaxrsParameterAggregator parameterAggregator) {
		return createBaseDocument(parameterAggregator);
	}
	
	/**
	 * Initializes a Lucene Document for the given JAX-RS Parameter Aggregator Field.
	 * 
	 * @param parameterAggregatorField
	 *            the JAX-RS Parameter Aggregator Field
	 * @return the document.
	 */
	private static Document createParameterAggregatorFieldDocument(final JaxrsParameterAggregatorField parameterAggregatorField) {
		return createBaseDocument(parameterAggregatorField);
	}

	/**
	 * Initializes a Lucene Document for the given JAX-RS Parameter Aggregator Method.
	 * 
	 * @param parameterAggregatorMethod
	 *            the JAX-RS Parameter Aggregator Method
	 * @return the document.
	 */
	private static Document createParameterAggregatorPropertyDocument(final JaxrsParameterAggregatorProperty parameterAggregatorMethod) {
		final Document document = createBaseDocument(parameterAggregatorMethod);
		if(parameterAggregatorMethod.getType() != null) {
			addFieldToDocument(document, FIELD_TYPE, parameterAggregatorMethod.getType().getErasureName());
			final List<IType> typeArguments = parameterAggregatorMethod.getType().getTypeArguments();
			for(IType typeArg: typeArguments) {
				addFieldToDocument(document, FIELD_TYPE, typeArg.getFullyQualifiedName());
			}
		}
		return document;
	}
	

	/**
	 * Initializes a Lucene Document for the given JAX-RS web.xml base
	 * Application.
	 * 
	 * @param httpMethod
	 *            the JAX-RS web.xml Application.
	 * @return the document.
	 */
	private static Document createWebxmlApplicationDocument(final JaxrsWebxmlApplication webxmlApplication) {
		final Document document = new Document();
		addFieldToDocument(document, FIELD_JAVA_PROJECT_IDENTIFIER, webxmlApplication.getMetamodel().getJavaProject()
				.getHandleIdentifier());
		addFieldToDocument(document, FIELD_TYPE, webxmlApplication.getElementKind().getCategory().toString());
		addFieldToDocument(document, FIELD_IDENTIFIER, getIdentifierValue(webxmlApplication));
		addFieldToDocument(document, FIELD_WEBXML_APPLICATION, Boolean.TRUE.toString());
		addFieldToDocument(document, FIELD_WEBXML_APPLICATION_OVERRIDES_JAVA_APPLICATION, Boolean.toString(webxmlApplication.isOverride()));
		addFieldToDocument(document, FIELD_RESOURCE_PATH, webxmlApplication.getResource().getFullPath()
				.toPortableString());
		addFieldToDocument(document, FIELD_APPLICATION_PATH, webxmlApplication.getApplicationPath());
		addFieldToDocument(document, FIELD_JAVA_CLASS_NAME, webxmlApplication.getJavaClassName());
		return document;
	}

	/**
	 * Initializes a Lucene Document for the given JAX-RS HTTP Method.
	 * 
	 * @param httpMethod
	 *            the JAX-RS HTTP Method
	 * @return the document.
	 */
	private static Document createHttpMethodDocument(final JaxrsHttpMethod httpMethod) {
		final Document document = createBaseDocument(httpMethod);
		addFieldToDocument(document, FIELD_HTTP_VERB, httpMethod.getHttpVerb());
		addFieldToDocument(document, FIELD_JAVA_CLASS_NAME, httpMethod.getJavaClassName());
		if(httpMethod.isBuiltIn()) {
			addFieldToDocument(document, FIELD_BUILT_IN_HTTP_METHOD, "true");
		}
		return document;
	}

	/**
	 * Initializes a Lucene Document for the given JAX-RS Name Binding.
	 * 
	 * @param nameBinding
	 *            the JAX-RS Name Binding
	 * @return the document.
	 */
	private static Document createNameBindingDocument(final JaxrsNameBinding nameBinding) {
		return createBaseDocument(nameBinding);
	}

	/**
	 * Initializes a Lucene Document for the given JAX-RS Provider.
	 * 
	 * @param provider
	 *            the JAX-RS Provider
	 * @return the document.
	 */
	private static Document createProviderDocument(final JaxrsProvider provider) {
		final Document document = createBaseDocument(provider);
		for (Entry<EnumElementKind, IType> entry : provider.getProvidedTypes().entrySet()) {
			if(entry.getKey() != null && entry.getValue() != null) {
				addFieldToDocument(document, FIELD_PROVIDER_KIND + entry.getKey().toString(), entry.getValue().getFullyQualifiedName());
			}
		}
		addFieldToDocument(document, FIELD_CONSUMED_MEDIA_TYPE, provider.getConsumedMediaTypes());
		addFieldToDocument(document, FIELD_PRODUCED_MEDIA_TYPE, provider.getProducedMediaTypes());
		return document;
	}

	/**
	 * Initializes a Lucene Document for the given JAX-RS ParamConverterProvider.
	 * 
	 * @param paramConverterProvider
	 *            the JAX-RS ParamConverterProviderDocument
	 * @return the document.
	 */
	private static Document createParamConverterProviderDocument(final JaxrsParamConverterProvider paramConverterProvider) {
		return createBaseDocument(paramConverterProvider);
	}

	/**
	 * Initializes a Lucene Document for the given JAX-RS Resource.
	 * 
	 * @param resource
	 *            the JAX-RS Resource
	 * @return the document.
	 */
	private static Document createResourceDocument(final JaxrsResource resource) {
		return createBaseDocument(resource);
	}

	/**
	 * Initializes a Lucene Document for the given JAX-RS Resource Field.
	 * 
	 * @param resourceField
	 *            the JAX-RS Resource Field
	 * @return the document.
	 */
	private static Document createResourceFieldDocument(final JaxrsResourceField resourceField) {
		return createBaseDocument(resourceField);
	}

	/**
	 * Initializes a Lucene Document for the given JAX-RS Resource Property.
	 * 
	 * @param resourceProperty
	 *            the JAX-RS Resource Property
	 * @return the document.
	 */
	private static Document createResourcePropertyDocument(final JaxrsResourceProperty resourceProperty) {
		return createBaseDocument(resourceProperty);
	}
	
	/**
	 * Initializes a Lucene Document for the given JAX-RS Resource Method.
	 * 
	 * @param resource
	 *            the JAX-RS Resource
	 * @return the document.
	 */
	private static Document createResourceMethodDocument(final JaxrsResourceMethod resourceMethod) {
		final Document document = createBaseDocument(resourceMethod);
		if(resourceMethod.getReturnedType() != null) {
			addFieldToDocument(document, FIELD_RETURNED_TYPE_NAME, resourceMethod.getReturnedType().getErasureName());
			final List<IType> typeArguments = resourceMethod.getReturnedType().getTypeArguments();
			for(IType typeArg: typeArguments) {
				addFieldToDocument(document, FIELD_TYPE, typeArg.getFullyQualifiedName());
			}
		}
		return document;
	}


}
