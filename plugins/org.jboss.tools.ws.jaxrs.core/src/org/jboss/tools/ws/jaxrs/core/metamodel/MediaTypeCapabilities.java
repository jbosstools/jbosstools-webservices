package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;

/**
 * Application classes can declare the supported request and response media
 * types using the @Consumes and @Produces annotations respectively. These
 * annotations MAY be applied to a resource method, a resource class, or to an
 * entity provider. Use of these annotations on a resource method overrides any
 * on the resource class or on an entity provider for a method argument or retu
 * import java.util.Iterator; rn type. In the absence of either of these
 * annotations, support for any media type is assumed.
 * 
 * @author xcoulon
 * 
 */
public class MediaTypeCapabilities implements Comparable<MediaTypeCapabilities> {

	/** The Java Element that carries the media types. */
	private IJavaElement element;

	private final List<String> mediatypes = new ArrayList<String>();

	/*
	 * public static class MediaTypeCapabilitiesBuilder {
	 * 
	 * private List<String> mediaTypes;
	 * 
	 * private IAnnotation annotation;
	 * 
	 * private IMember annotatedMember;
	 * 
	 * public MediaTypeCapabilitiesBuilder(IMember annotatedMember) {
	 * this.annotatedMember = annotatedMember; }
	 * 
	 * public MediaTypeCapabilitiesBuilder annotation(IAnnotation annotation) {
	 * this.annotation = annotation; return this; }
	 * 
	 * public MediaTypeCapabilitiesBuilder mediaTypes(List<String> mediaTypes) {
	 * this.mediaTypes = mediaTypes; return this; }
	 * 
	 * public MediaTypeCapabilities build() { IJavaElement element = (annotation
	 * != null) ? annotation : annotatedMember;
	 * 
	 * if (mediaTypes == null || mediaTypes.isEmpty()) { this.mediaTypes =
	 * Collections.emptyList(); } return new MediaTypeCapabilities(mediaTypes,
	 * element); } }
	 */

	/**
	 * Full constructor, used in conjunction with its
	 * MediaTypeCapabilitiesBuilder private MediaTypeCapabilities(List<String>
	 * mediaTypes, IJavaElement element) { super(mediaTypes); this.element =
	 * element; }
	 */

	/**
	 * Full constructor.
	 */
	public MediaTypeCapabilities(IJavaElement element) {
		this.element = element;
	}

	/**
	 * Full constructor with mediatypes
	 */
	public MediaTypeCapabilities(IJavaElement element, List<String> mediatypes) {
		this.element = element;
		this.mediatypes.addAll(mediatypes);
	}

	/**
	 * Replace the current media types capabilities with the ones given in
	 * parameter
	 * 
	 * @param mediaTypes
	 *            the new supported media types
	 * @return true if changed were made
	 */
	public boolean merge(MediaTypeCapabilities capabilities) {
		boolean changed = false;
		// remove obsolete values
		changed = mediatypes.retainAll(capabilities.getMediatypes());
		// avoid duplicates
		mediatypes.removeAll(capabilities.getMediatypes());
		changed = changed | mediatypes.addAll(capabilities.getMediatypes());
		this.element = capabilities.getElement();
		return changed;
	}

	/**
	 * @return the annotation
	 */
	public IJavaElement getElement() {
		return element;
	}

	/**
	 * @return the mediatypes
	 */
	public List<String> getMediatypes() {
		return mediatypes;
	}

	@Override
	public int compareTo(MediaTypeCapabilities otherMediaTypes) {
		for (int i = 0; i < mediatypes.size(); i++) {
			if (i >= otherMediaTypes.size()) {
				return 1; // 'this' is greater than 'other'
			}
			int comp = mediatypes.get(i).compareTo(otherMediaTypes.get(i));
			if (comp != 0) {
				return comp;
			}
		}
		return 0;
	}

	public String get(int i) {
		return mediatypes.get(i);
	}

	public int size() {
		return mediatypes.size();
	}

	public boolean contains(String mediatype) {
		return mediatypes.contains(mediatype);
	}

	public boolean isEmpty() {
		return mediatypes.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MediaTypeCapabilities [element=" + element + ", mediatypes=" + mediatypes + "]";
	}

}
