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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.createAnnotation;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMember;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Testing JAX-RS Element indexation with Lucene
 * 
 * @author xcoulon
 * 
 */
public class LuceneIndexationTestCase {

	private Directory index;
	private IndexWriter w;
	private StandardAnalyzer analyzer;

	private Map<String, IJaxrsElement> elements = new HashMap<String, IJaxrsElement>();
	private IndexWriterConfig config = null;

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", false);
	
	@Before
	public void setup() throws CoreException, CorruptIndexException, IOException {
		metamodelMonitor.getMetamodel();
		analyzer = new StandardAnalyzer();
		config = new IndexWriterConfig(analyzer);
		index = new RAMDirectory();
		w = new IndexWriter(index, config);
		w.commit();
	}

	@After
	public void closeLuceneIndex() throws CorruptIndexException, IOException {
		w.close();
		index.close();
	}

	private void store(JaxrsJavaElement<? extends IMember> element) {
		elements.put(element.getJavaElement().getHandleIdentifier(), element);
	}
	
	private IJaxrsElement retrieve(final String identifier) {
		return elements.get(identifier);
	}

	private void index(final JaxrsHttpMethod httpMethod, boolean update) throws IOException {
		Document doc = new Document();
		
		FieldType storedType = new FieldType();
		storedType.setStored(true);
		storedType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		storedType.setTokenized(false);
		
		doc.add(new Field("verb", httpMethod.getHttpVerb(), storedType));
		doc.add(new Field("javaType", httpMethod.getJavaClassName(), storedType));
		doc.add(new Field("handleIdentifier", httpMethod.getJavaElement().getHandleIdentifier(), storedType));
		if(update) {
			w.updateDocument(new Term("handleIdentifier", httpMethod.getJavaElement().getHandleIdentifier()), doc);
		} else {
			w.addDocument(doc);
		}
		w.commit();
	}

	private void unindex(final JaxrsHttpMethod httpMethod) throws IOException {
		w.deleteDocuments(new Term("handleIdentifier", httpMethod.getJavaElement().getHandleIdentifier()));
		w.commit();
	}


	private IJaxrsElement query(String name, String value) throws Exception {
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		try {
			final TermQuery termQuery = new TermQuery(new Term("verb", value));
			final TopDocs result = searcher.search(termQuery, 1);
			if (result.totalHits.value >= 1) {
				int docIndex = result.scoreDocs[0].doc;
				final Document doc = searcher.doc(docIndex);
				return retrieve(doc.get("handleIdentifier"));
			}
			return null;
		} finally {
			reader.close();

		}
	}

	@Test
	public void shouldRetrieveJaxrsHttpMethodFromVerb() throws Exception {
		// pre-condition
		final JaxrsHttpMethod httpMethod = metamodelMonitor.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		store(httpMethod);
		assertThat(httpMethod.getHttpVerb(), equalTo("FOO"));
		// operations
		index(httpMethod, false);
		final IJaxrsElement result = query("verb", "FOO");
		// verifications
		assertThat(result, equalTo((IJaxrsElement) httpMethod));
	}

	@Test
	public void shouldRetrieveJaxrsHttpMethodFromVerbAfterUpdate() throws Exception {
		// pre-condition
		final JaxrsHttpMethod httpMethod = metamodelMonitor.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		store(httpMethod);
		assertThat(httpMethod.getHttpVerb(), equalTo("FOO"));
		index(httpMethod, false);
		assertThat(query("verb", "FOO"), equalTo((IJaxrsElement) httpMethod));
		httpMethod.addOrUpdateAnnotation(createAnnotation("javax.ws.rs.HttpMethod", "Bar"));
		assertThat(httpMethod.getHttpVerb(), equalTo("Bar"));
		// operations
		index(httpMethod, true);
		final IJaxrsElement fooResult = query("verb", "FOO");
		final IJaxrsElement barResult = query("verb", "Bar");
		// verifications
		assertThat(fooResult, nullValue());
		assertThat(barResult, equalTo((IJaxrsElement)httpMethod));
	}

	@Test
	public void shouldNotRetrieveJaxrsHttpMethodFromVerbAfterRemoval() throws Exception {
		// pre-condition
		final JaxrsHttpMethod httpMethod = metamodelMonitor.createHttpMethod("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		store(httpMethod);
		assertThat(httpMethod.getHttpVerb(), equalTo("FOO"));
		index(httpMethod, false);
		assertThat(query("verb", "FOO"), equalTo((IJaxrsElement) httpMethod));
		// operations
		unindex(httpMethod);
		final IJaxrsElement result = query("verb", "FOO");
		// verifications
		assertThat(result, nullValue());
	}
	
	
}
