/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * @author xcoulon
 *
 */
public class JavaElementDelta implements IJavaElementDelta {

	/** The element associated with this delta.*/
	private final IJavaElement affectedElement;
	
	/** the associated Compilation Unit (or null if it does not apply in the context of the affected element).*/
	private final CompilationUnit ast;
	
	/** The kind of delta.*/
	private final int kind;
	
	/** Some flags o further qualify the delta. */
	private final int flags;

	/** The list of affected children elements.*/
	private final List<IJavaElementDelta> affectedChildren = new ArrayList<IJavaElementDelta>();
	
	/** The list of affected annotations.*/
	private final List<IJavaElementDelta> affectedAnnotations = new ArrayList<IJavaElementDelta>();
	
	/**
	 * Full constructor.
	 * @param affectedElement the affected Java Element
	 * @param ast the associated Compilation Unit (or null if it does not apply in the context of the affected element)
	 * @param kind the kind of change
	 * @param flags some optional change qualifiers
	 */
	public JavaElementDelta(final IJavaElement affectedElement, final CompilationUnit ast, final int kind, final int flags) {
		this.affectedElement = affectedElement;
		this.ast = ast;
		this.kind = kind;
		this.flags = flags;
	}
	
	public void addAffectedElementDelta(final IJavaElementDelta affectedElementDelta) {
		this.affectedChildren.add(affectedElementDelta);
	}
	
	public void addAffectedAnnotation(final IJavaElementDelta affectedAnnotationDelta) {
		this.affectedAnnotations.add(affectedAnnotationDelta);
	}
	
	@Override
	public IJavaElementDelta[] getAddedChildren() {
		return new IJavaElementDelta[0];
	}

	@Override
	public IJavaElementDelta[] getAffectedChildren() {
		return this.affectedChildren.toArray(new IJavaElementDelta[affectedChildren.size()]);
	}

	@Override
	public IJavaElementDelta[] getAnnotationDeltas() {
		return this.affectedAnnotations.toArray(new IJavaElementDelta[affectedAnnotations.size()]);
	}

	@Override
	public CompilationUnit getCompilationUnitAST() {
		return ast;
	}

	@Override
	public IJavaElementDelta[] getChangedChildren() {
		return new IJavaElementDelta[0];
	}

	@Override
	public IJavaElement getElement() {
		return this.affectedElement;
	}

	@Override
	public int getFlags() {
		return this.flags;
	}

	@Override
	public int getKind() {
		return this.kind;
	}

	@Override
	public IJavaElement getMovedFromElement() {
		return null;
	}

	@Override
	public IJavaElement getMovedToElement() {
		return null;
	}

	@Override
	public IJavaElementDelta[] getRemovedChildren() {
		return null;
	}

	@Override
	public IResourceDelta[] getResourceDeltas() {
		return null;
	}

	@Override
	public String toString() {
		return this.affectedElement.getElementName() + " (deltaKind=" + this.kind + ")";
	}
	
}
