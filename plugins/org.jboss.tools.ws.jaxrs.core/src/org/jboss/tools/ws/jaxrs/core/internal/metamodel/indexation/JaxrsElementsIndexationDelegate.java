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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;

/**
 * @author xcoulon
 * 
 */
public class JaxrsElementsIndexationDelegate {

	/** The metamodel associated with this indexation delegate.*/
	private final JaxrsMetamodel metamodel;
	
	private final Directory index;
	private final StandardAnalyzer analyzer;
	private final IndexWriterConfig config;
	private IndexWriter indexWriter;
	private IndexReader indexReader;
	private IndexSearcher indexSearcher;

	/**
	 * Default constructor
	 * 
	 * @throws CoreException
	 * 
	 * @throws CorruptIndexException
	 * @throws LockObtainFailedException
	 * @throws IOException
	 */
	public JaxrsElementsIndexationDelegate(final JaxrsMetamodel metamodel) throws CoreException {
		try {
			this.metamodel = metamodel;
			analyzer = new StandardAnalyzer(Version.LUCENE_35);
			config = new IndexWriterConfig(Version.LUCENE_35, analyzer);
			config.setMaxBufferedDeleteTerms(1);
			index = new RAMDirectory();
			indexWriter = new IndexWriter(index, config);
			indexReader = IndexReader.open(indexWriter, true);
			indexSearcher = new IndexSearcher(indexReader);
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, JBossJaxrsCorePlugin.PLUGIN_ID,
					"Failed to initialize JAX-RS Elements Indexer", e));
		}
	}

	/**
	 * Method to call when the parent metamodel is closed.
	 * 
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public void dispose() throws CorruptIndexException, IOException {
		index.close();
	}

	/**
	 * Clear the whole index at once.
	 * 
	 * @throws CoreException
	 */
	public void clear() throws CoreException {
		try {
			indexWriter.deleteAll();
			indexWriter.commit();
		} catch (IOException e) {
			final Status message = Logger.error("Failed to delete all documents in the JAX-RS Index", e);
			throw new CoreException(message);
		}
	}

	public void indexElement(final IJaxrsElement element) {
		final long start = System.currentTimeMillis();
		try {
			Logger.traceIndexing("Indexing {} after addition...", element.getName());
			final Document doc = LuceneDocumentFactory.createDocument(element);
			Logger.debugIndexing("Adding JAX-RS Element into index with following fields: {}", doc.getFields());
			indexWriter.addDocument(doc);
			indexWriter.commit();
		} catch (IOException e) {
			Logger.error("Failed to index the JAX-RS Element " + element, e);
		} finally {
			Logger.traceIndexing(" Done indexing {}.", element.getName());
			final long end = System.currentTimeMillis();
			Logger.tracePerf("Element indexed in {}ms", (end - start));
		}
	}

	public void indexElement(final IJaxrsEndpoint endpoint) {
		try {
			Logger.traceIndexing("Indexing {} after addition...", endpoint);
			final Document doc = LuceneDocumentFactory.createDocument(endpoint);
			Logger.debugIndexing("Adding JAX-RS Endpoint into index with following fields: {}", doc.getFields());
			indexWriter.addDocument(doc);
			indexWriter.commit();
		} catch (IOException e) {
			Logger.error("Failed to index the JAX-RS Endpoint " + endpoint, e);
		} finally {
			Logger.traceIndexing(" Done indexing {}.", endpoint);
		}
	}

	public void reindexElement(final IJaxrsElement element) {
		try {
			Logger.traceIndexing("Re-indexing {} after some internal change...", element.getName());
			final Document doc = LuceneDocumentFactory.createDocument(element);
			final Term identifierTerm = LuceneDocumentFactory.getIdentifierTerm(element);
			indexWriter.updateDocument(identifierTerm, doc);
			indexWriter.commit();
			Logger.debugIndexing("Updated JAX-RS Element index with following fields: {}. Writer.hasDeletions={}",
					doc.getFields(), indexWriter.hasDeletions());
		} catch (IOException e) {
			Logger.error("Failed to re-index the JAX-RS Element " + element, e);
		} finally {
			Logger.traceIndexing(" Done re-indexing {}.", element);
		}
	}

	public void reindexElement(final IJaxrsEndpoint endpoint) {
		try {
			Logger.traceIndexing("Re-indexing {} after some internal change...", endpoint);
			final Document doc = LuceneDocumentFactory.createDocument(endpoint);
			final Term identifierTerm = LuceneDocumentFactory.getIdentifierTerm(endpoint);
			indexWriter.updateDocument(identifierTerm, doc);
			indexWriter.commit();
			Logger.debugIndexing("Updated JAX-RS Endpoint index with following fields: {}. Writer.hasDeletions={}",
					doc.getFields(), indexWriter.hasDeletions());
		} catch (IOException e) {
			Logger.error("Failed to re-index the JAX-RS Endpoint " + endpoint, e);
		} finally {
			Logger.traceIndexing(" Done re-indexing {}.", endpoint);
		}
	}

	/**
	 * Removes the given {@link IJaxrsElement} from the index.
	 * 
	 * @param element
	 */
	public void unindexElement(final IJaxrsElement element) {
		try {
			final Term identifierTerm = LuceneDocumentFactory.getIdentifierTerm(element);
			final Term markerTypeTerm = LuceneDocumentFactory.getMarkerTypeTerm();
			if(Logger.isDebugIndexingEnabled()) {
				Logger.debugIndexing("Unindexing {} after removal...", identifierTerm);
				
			}
			final BooleanQuery deleteResourceMarkersQuery = new BooleanQuery();
			deleteResourceMarkersQuery.add(new BooleanClause(new TermQuery(identifierTerm), Occur.MUST));
			deleteResourceMarkersQuery.add(new BooleanClause(new TermQuery(markerTypeTerm), Occur.MUST_NOT));
			
			if(Logger.isDebugIndexingEnabled()) {
				Logger.debugIndexing("Removing {} documents from index", count(deleteResourceMarkersQuery));
			}
			indexWriter.deleteDocuments(deleteResourceMarkersQuery);
			indexWriter.commit();
		} catch (IOException e) {
			Logger.error("Failed to unindex the JAX-RS Element " + element, e);
		} finally {
			Logger.traceIndexing("Done unindexing {}.", element.getName());
		}
	}

	/**
	 * Removes the given {@link IJaxrsEndpoint} from the index.
	 * 
	 * @param element
	 */
	// TODO: avoid code duplication with unindexElement(IJaxrsElement) above
	public void unindexEndpoint(final IJaxrsEndpoint endpoint) {
		try {
			Logger.debugIndexing("Unindexing {} after removal...", endpoint);
			final Term identifierTerm = LuceneDocumentFactory.getIdentifierTerm(endpoint);
			indexWriter.deleteDocuments(identifierTerm);
			indexWriter.commit();
		} catch (IOException e) {
			Logger.error("Failed to unindex the JAX-RS Element " + endpoint, e);
		} finally {
			Logger.traceIndexing("Done unindexing {}.", endpoint);
		}
	}

	/**
	 * Index the given {@link IMarker} 
	 * @param marker the marker to index
	 */
	public void indexMarker(final IMarker marker) {
		try {
			Logger.traceIndexing("Indexing {} after addition...", marker);
			final Document doc = LuceneDocumentFactory.createDocument(marker);
			Logger.debugIndexing("Adding Marker into index with following fields: {}", doc.getFields());
			indexWriter.addDocument(doc);
			indexWriter.commit();
		} catch (IOException e) {
			Logger.error("Failed to index the JAX-RS Endpoint " + marker, e);
		} finally {
			Logger.traceIndexing(" Done indexing {}.", marker);
		}

		
	}

	/**
	 * Unindex the markers on the given {@link IResource}, whatever their associated {@link LuceneFields#FIELD_JAXRS_PROBLEM_TYPE}
	 * @param the resource on which markers should be removed from the index
	 */
	public void unindexMarkers(final IResource resource) {
		// skip this step for built-in elements (ie: not associated with an IResource)
		if(resource == null) {
			return;
		}
		try {
			Logger.debugIndexing("Unindexing markers on {} after removal...", resource.getFullPath());
			final Term markerTypeTerm = LuceneDocumentFactory.getMarkerTypeTerm();
			final Term resourcePathTerm = LuceneDocumentFactory.getResourcePathTerm(resource);
			final BooleanQuery deleteResourceMarkersQuery = new BooleanQuery();
			deleteResourceMarkersQuery.add(new BooleanClause(new TermQuery(markerTypeTerm), Occur.MUST));
			deleteResourceMarkersQuery.add(new BooleanClause(new TermQuery(resourcePathTerm), Occur.MUST));
			if(Logger.isDebugIndexingEnabled()) {
				Logger.debugIndexing("Removing {} documents from index", count(deleteResourceMarkersQuery));
			}
			indexWriter.deleteDocuments(deleteResourceMarkersQuery);
			indexWriter.commit();
		} catch (IOException e) {
			Logger.error("Failed to unindex the Resource " + resource.getFullPath(), e);
		} finally {
			Logger.traceIndexing("Done unindexing {}.", resource.getFullPath());
		}
	}
	
	/**
	 * Unindexes the given {@link IMarker}.
	 * @param marker the lonely marker to unindex and delete
	 */
	public void unindexMarker(final IMarker marker) {
		try {
			Logger.debugIndexing("Unindexing marker #{} on {}...", marker.getId(), marker.getResource().getFullPath().toOSString());
			final Term markerTypeTerm = LuceneDocumentFactory.getMarkerTypeTerm();
			final Term markerIdTerm = LuceneDocumentFactory.getIdentifierTerm(marker);
			final BooleanQuery deleteResourceMarkersQuery = joinTerms(markerTypeTerm, markerIdTerm);
			if(Logger.isDebugIndexingEnabled()) {
				Logger.debugIndexing("Removing {} documents from index", count(deleteResourceMarkersQuery));
			}
			indexWriter.deleteDocuments(deleteResourceMarkersQuery);
			indexWriter.commit();
		} catch (IOException e) {
			Logger.error("Failed to unindex the marker " + marker.getId() + " on " + marker.getResource().getFullPath().toOSString(), e);
		} finally {
			Logger.traceIndexing("Done unindexing {}.", marker.getId());
		}		
	}

	/**
	 * Join the given {@link Term}s using the {@link Occur#MUST} clause into a
	 * {@link BooleanQuery}.
	 * 
	 * @param terms
	 *            the search terms to join
	 * @return the result query.
	 */
	public static BooleanQuery joinTerms(final Term... terms) {
		final BooleanQuery query = new BooleanQuery();
		if (terms != null) {
			for (Term term : terms) {
				query.add(new BooleanClause(new TermQuery(term), Occur.MUST));
			}
		}
		return query;
	}
	
	/**
	 * Performs a {@link BooleanQuery} based on the given {@link TermQuery},
	 * assuming that each query element is mandatory. This method returns a
	 * single document identifier for an {@link IJaxrsElement}, or null if no
	 * document matched.
	 * 
	 * @param queries
	 * @return the document identifier matching the query, or null if no
	 *         document matched
	 */
	public String searchElement(final Term... terms) {
		final BooleanQuery query = joinTerms(terms);
		return searchSingle(query, IndexedObjectType.JAX_RS_ELEMENT);
	}

	private String searchSingle(final Query query, final IndexedObjectType type) {
		try {
			final IndexSearcher searcher = getNewIndexSearcherIfNeeded();
			Logger.traceIndexing("Using IndexReader (current={} / hasDeletions={}) containing {} documents",
					indexReader.isCurrent(), indexReader.hasDeletions(), indexReader.numDocs());
			Logger.traceIndexing("Searching single document matching {}", query.toString());
			final TopDocs result = searcher.search(query, 1);
			if (result.totalHits >= 1) {
				int docIndex = result.scoreDocs[0].doc;
				final Document doc = searcher.doc(docIndex);
				final String docIdentifier = doc.get(LuceneFields.FIELD_IDENTIFIER);
				return docIdentifier.substring(type.getPrefix().length());
			}
		} catch (IOException e) {
			Logger.error("Failed to search for JAX-RS element in index", e);
		}
		Logger.traceIndexing(" Not document matched the query.");
		return null;
	}

	/**
	 * Searches and returns a collection of elements matching the
	 * {@link BooleanQuery} based on the given {@link TermQuery}, assuming that
	 * each query term is MUST match.
	 * 
	 * @param clazz the expected JAX-RS element type
	 * @param terms the search terms
	 * 
	 * @return the {@link IJaxrsElement}s matching the query, or null if no
	 *         document matched
	 */
	public <T> List<T> searchElements(final Term... terms) {
		try {
			final IndexSearcher searcher = getNewIndexSearcherIfNeeded();
			Logger.traceIndexing("Using IndexReader (current={} / hasDeletions={}) containing {} documents",
					indexReader.isCurrent(), indexReader.hasDeletions(), indexReader.numDocs());
			final BooleanQuery query = joinTerms(terms);
			Logger.traceIndexing("Searching documents matching {}", query.toString());
			final JaxrsElementsCollector<T> collector = new JaxrsElementsCollector<T>();
			searcher.search(query, collector);
			final List<T> elements = collector.getResults();
			Logger.traceIndexing(" Found {} matching endpoints", elements.size());
			return elements;
		} catch (IOException e) {
			Logger.error("Failed to search for JAX-RS element in index", e);
		}
		return Collections.emptyList();
	}
	
	/**
	 * Searches and returns a collection of {@link IResource}s matching the
	 * {@link BooleanQuery} based on the given {@link TermQuery}, assuming that
	 * each query term is MUST match.
	 * @param categoryTerm
	 * @param problemTypeTerm
	 * @return
	 */
	public List<IResource> searchResources(final Term categoryTerm, final Term problemTypeTerm) {
		try {
			Logger.debug("Looking for workspace resources with problem {}", problemTypeTerm);
			final IndexSearcher searcher = getNewIndexSearcherIfNeeded();
			Logger.traceIndexing("Using IndexReader (current={} / hasDeletions={}) containing {} documents",
					indexReader.isCurrent(), indexReader.hasDeletions(), indexReader.numDocs());
			final BooleanQuery query = joinTerms(categoryTerm, problemTypeTerm);
			Logger.traceIndexing("Searching documents matching {}", query.toString());
			final ResourcesCollector collector = new ResourcesCollector();
			searcher.search(query, collector);
			final List<IResource> resources = collector.getResults();
			Logger.traceIndexing(" Found {} matching endpoints", resources.size());
			return resources;
		} catch (IOException e) {
			Logger.error("Failed to search for JAX-RS element in index", e);
		}
		return Collections.emptyList();	
	}


	/**
	 * Searches and returns a collection of {@link JaxrsEndpoint}s matching the
	 * {@link BooleanQuery} based on the given {@link TermQuery}, assuming that
	 * each query term is MUST match.
	 * 
	 * @param queries
	 * @return the {@link JaxrsEndpoint}s matching the query, or null if no
	 *         document matched
	 */
	public List<JaxrsEndpoint> searchEndpoints(final Term... terms) {
		try {
			Logger.debugIndexing("Searching for Endpoints with using: {}", Arrays.asList(terms));
			final IndexSearcher searcher = getNewIndexSearcherIfNeeded();
			Logger.traceIndexing("Using IndexReader (current={} / hasDeletions={}) containing {} documents",
					indexReader.isCurrent(), indexReader.hasDeletions(), indexReader.numDocs());
			final BooleanQuery query = joinTerms(terms);
			Logger.traceIndexing("Searching documents matching {}", query.toString());
			final JaxrsEndpointsCollector collector = new JaxrsEndpointsCollector();
			searcher.search(query, collector);
			final List<JaxrsEndpoint> endpoints = collector.getResults();
			Logger.traceIndexing(" Found {} matching endpoints", endpoints.size());
			return endpoints;
		} catch (IOException e) {
			Logger.error("Failed to search for JAX-RS element in index", e);
		}
		return Collections.emptyList();
	}
	
	/**
	 * Performs a {@link BooleanQuery} based on the given {@link TermQuery},
	 * assuming that each element MUST occur, and return the number of matches.
	 * 
	 * @param terms
	 * @return the number of matching documents
	 */
	public int count(final Query query) {
		try {
			Logger.traceIndexing("Using IndexReader (current={} / hasDeletions={}) containing {} documents",
					indexReader.isCurrent(), indexReader.hasDeletions(), indexReader.numDocs());
			final IndexSearcher searcher = getNewIndexSearcherIfNeeded();
			final TotalHitCountCollector collector = new TotalHitCountCollector();
			Logger.traceIndexing("Counting documents matching {}...", query.toString());
			searcher.search(query, collector);
			final int totalHits = collector.getTotalHits();
			Logger.traceIndexing(" Found {} matching documents", totalHits);
			return totalHits;
		} catch (IOException e) {
			Logger.error("Failed to search for JAX-RS element in index", e);
		}
		return 0;
	}

	private IndexSearcher getNewIndexSearcherIfNeeded() throws IOException {
		final IndexReader newIndexReader = IndexReader.openIfChanged(indexReader, indexWriter, true);
		if (newIndexReader != null) {
			this.indexReader = newIndexReader;
			Logger.traceIndexing("Reopening IndexReader (current={} / hasDeletions={}) now containing {} documents",
					indexReader.isCurrent(), indexReader.hasDeletions(), indexReader.numDocs());
			this.indexSearcher = new IndexSearcher(indexReader);
		}
		return this.indexSearcher;
	}

	/**
	 * Document IDs Collector. Collectd the mathcing {@link Document}'s
	 * {@code LuceneDocumentFactory#FIELD_IDENTIFIER} {@link Field} in a
	 * {@link Set} to avoid duplicate results.
	 * 
	 * @author xcoulon
	 * 
	 */
	static abstract class AnyResultsCollector<T> extends Collector {

		/** The current Lucene {@link IndexReader}. */
		private IndexReader indexReader;
		
		/**
		 * Set of doc identifiers matching the query. Using a {@link TreeSet} to
		 * keep order of returned elements.
		 */
		final List<T> results;

		private int docBase;

		public AnyResultsCollector(final List<T> results) {
			this.results = results;
		}
		
		@Override
		public void setScorer(Scorer scorer) throws IOException {
		}

		@Override
		public void setNextReader(IndexReader indexReader, int docBase) throws IOException {
			this.setIndexReader(indexReader);
			this.setDocBase(docBase);
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

		public List<T> getResults() throws CorruptIndexException, IOException {
			return results;
		}

		/**
		 * @return the indexReader
		 */
		public IndexReader getIndexReader() {
			return indexReader;
		}

		/**
		 * @param indexReader the indexReader to set
		 */
		public void setIndexReader(IndexReader indexReader) {
			this.indexReader = indexReader;
		}

		/**
		 * @return the docBase
		 */
		public int getDocBase() {
			return docBase;
		}

		/**
		 * @param docBase the docBase to set
		 */
		public void setDocBase(int docBase) {
			this.docBase = docBase;
		}

	}
	
	class JaxrsElementsCollector<T> extends AnyResultsCollector<T> {

		/** The prefix to strip off the result identifiers. */
		private final String identifierPrefix;

		public JaxrsElementsCollector() {
			super(new ArrayList<T>());
			this.identifierPrefix = IndexedObjectType.JAX_RS_ELEMENT.getPrefix();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void collect(int docId) throws IOException {
			Logger.traceIndexing("  Adding doc#{} (deleted={}) to search results", (getDocBase() + docId),
					getIndexReader().isDeleted(docId));
			final Document document = getIndexReader().document(docId);
			final String docIdentifier = document.get(LuceneFields.FIELD_IDENTIFIER);
			final String docCategory = document.get(LuceneFields.FIELD_TYPE);
			Logger.traceIndexing("  Retrieved document #{} ({})", docIdentifier, docCategory);
			final IJaxrsElement element = metamodel.getElement(docIdentifier.substring(identifierPrefix.length()));
			if(element != null) {
				results.add((T) element);
			}
		}
	}

	class JaxrsEndpointsCollector extends AnyResultsCollector<JaxrsEndpoint> {
		
		/** The prefix to strip off the result identifiers. */
		private final String identifierPrefix;
		
		public JaxrsEndpointsCollector() {
			super(new ArrayList<JaxrsEndpoint>());
			this.identifierPrefix = IndexedObjectType.JAX_RS_ENDPOINT.getPrefix();
		}
		
		@Override
		public void collect(int docId) throws IOException {
			Logger.traceIndexing("  Adding doc#{} (deleted={}) to search results", (getDocBase() + docId),
					getIndexReader().isDeleted(docId));
			final Document document = getIndexReader().document(docId);
			final String docIdentifier = document.get(LuceneFields.FIELD_IDENTIFIER);
			final String docCategory = document.get(LuceneFields.FIELD_TYPE);
			Logger.traceIndexing("  Retrieved document #{} ({})", docIdentifier, docCategory);
			final JaxrsEndpoint endpoint = metamodel.getEndpoint(docIdentifier.substring(identifierPrefix.length()));
			if(endpoint != null) {
				results.add(endpoint);
			}
		}
	}
	
	class ResourcesCollector extends AnyResultsCollector<IResource> {

		public ResourcesCollector() {
			super(new ArrayList<IResource>());
		}
		@Override
		public void collect(int docId) throws IOException {
			Logger.traceIndexing("  Adding doc#{} (deleted={}) to search results", (getDocBase() + docId),
					getIndexReader().isDeleted(docId));
			final Document document = getIndexReader().document(docId);
			final String identifier = document.get(LuceneFields.FIELD_IDENTIFIER);
			final String resourcePath = document.get(LuceneFields.FIELD_RESOURCE_PATH);
			Logger.traceIndexing("  Retrieved document #{} for resource at {}", identifier, resourcePath);
			final IResource resource = metamodel.getProject().getWorkspace().getRoot().findMember(new Path(resourcePath));
			if(resource != null) {
				results.add(resource);
			}
		}
	}

}
