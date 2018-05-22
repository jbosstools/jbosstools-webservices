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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
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
			analyzer = new StandardAnalyzer();
			config = new IndexWriterConfig(analyzer);
			//config.setMaxBufferedDeleteTerms(1);
			index = new RAMDirectory();
			indexWriter = new IndexWriter(index, config);
			indexReader = DirectoryReader.open(indexWriter, true, false);
			indexSearcher = new IndexSearcher(indexReader);
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, JBossJaxrsCorePlugin.PLUGIN_ID,
					"Failed to initialize JAX-RS Elements Indexer", e));
		}
	}

	/**
	 * Method to call when the parent metamodel is closed.
	 * 
	 * @throws IOException
	 */
	public void dispose() throws IOException {
		indexWriter.close();
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
			Logger.traceIndexingPerf("Element indexed in {}ms", (end - start));
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
			final BooleanQuery.Builder deleteResourceMarkersQueryBuilder = new BooleanQuery.Builder();
			deleteResourceMarkersQueryBuilder.add(new BooleanClause(new TermQuery(identifierTerm), Occur.MUST));
			deleteResourceMarkersQueryBuilder.add(new BooleanClause(new TermQuery(markerTypeTerm), Occur.MUST_NOT));
			final BooleanQuery deleteResourceMarkersQuery = deleteResourceMarkersQueryBuilder.build();
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
	 * Join the given {@link Term}s using the {@link Occur#MUST} clause into a
	 * {@link BooleanQuery}.
	 * 
	 * @param terms
	 *            the search terms to join
	 * @return the result query.
	 */
	public static BooleanQuery joinTerms(final Term... terms) {
		final BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		if (terms != null) {
			for (Term term : terms) {
				queryBuilder.add(new BooleanClause(new TermQuery(term), Occur.MUST));
			}
		}
		return queryBuilder.build();
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
			Logger.traceIndexing("Using IndexReader (hasDeletions={}) containing {} documents",
					indexReader.hasDeletions(), indexReader.numDocs());
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
	public <T> Set<T> searchElements(final Term... terms) {
		try {
			final IndexSearcher searcher = getNewIndexSearcherIfNeeded();
			Logger.traceIndexing("Using IndexReader (hasDeletions={}) containing {} documents",
					indexReader.hasDeletions(), indexReader.numDocs());
			final BooleanQuery query = joinTerms(terms);
			Logger.traceIndexing("Searching documents matching {}", query.toString());
			final JaxrsElementsCollector<T> collector = new JaxrsElementsCollector<T>();
			searcher.search(query, collector);
			final Set<T> elements = collector.getResults();
			Logger.traceIndexing(" Found {} matching elements", elements.size());
			return elements;
		} catch (IOException e) {
			Logger.error("Failed to search for JAX-RS element in index", e);
		}
		return Collections.emptySet();
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
	public Set<JaxrsEndpoint> searchEndpoints(final Term... terms) {
		try {
			Logger.debugIndexing("Searching for Endpoints with using: {}", Arrays.asList(terms));
			final IndexSearcher searcher = getNewIndexSearcherIfNeeded();
			Logger.traceIndexing("Using IndexReader (hasDeletions={}) containing {} documents",
					indexReader.hasDeletions(), indexReader.numDocs());
			final BooleanQuery query = joinTerms(terms);
			Logger.traceIndexing("Searching documents matching {}", query.toString());
			final JaxrsEndpointsCollector collector = new JaxrsEndpointsCollector();
			searcher.search(query, collector);
			final Set<JaxrsEndpoint> endpoints = collector.getResults();
			Logger.traceIndexing(" Found {} matching endpoints", endpoints.size());
			return endpoints;
		} catch (IOException e) {
			Logger.error("Failed to search for JAX-RS element in index", e);
		}
		return Collections.emptySet();
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
			Logger.traceIndexing("Using IndexReader (hasDeletions={}) containing {} documents",
					indexReader.hasDeletions(), indexReader.numDocs());
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
		final IndexReader newIndexReader = DirectoryReader.openIfChanged((DirectoryReader) indexReader, indexWriter, true);
		if (newIndexReader != null) {
			this.indexReader = newIndexReader;
			Logger.traceIndexing("Reopening IndexReader (hasDeletions={}) now containing {} documents",
					indexReader.hasDeletions(), indexReader.numDocs());
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
	abstract static class AnyResultsCollector<T> extends SimpleCollector {

		/** The current Lucene {@link IndexReader}. */
		private LeafReader indexReader;
		
		/**
		 * Set of doc identifiers matching the query. Using a {@link TreeSet} to
		 * keep order of returned elements.
		 */
		final Set<T> results;

		private int docBase;

		public AnyResultsCollector(final Set<T> results) {
			this.results = results;
		}
		
		@Override
		public void setScorer(Scorer scorer) throws IOException {
		}

		@Override
		public void doSetNextReader(LeafReaderContext context) throws IOException {
			this.setIndexReader(context.reader());
			this.setDocBase(context.docBase);
		}

		@Override
		public boolean needsScores() {
			return false;
		}

		public Set<T> getResults() throws CorruptIndexException, IOException {
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
		public void setIndexReader(LeafReader indexReader) {
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
			super(new HashSet<T>());
			this.identifierPrefix = IndexedObjectType.JAX_RS_ELEMENT.getPrefix();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void collect(int docId) throws IOException {
			Logger.traceIndexing("  Adding doc#{} (deleted={}) to search results", (getDocBase() + docId),
					LuceneDocumentFactory.isDeleted(getIndexReader(), docId));
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
			super(new HashSet<JaxrsEndpoint>());
			this.identifierPrefix = IndexedObjectType.JAX_RS_ENDPOINT.getPrefix();
		}
		
		@Override
		public void collect(int docId) throws IOException {
			Logger.traceIndexing("  Adding doc#{} (deleted={}) to search results", (getDocBase() + docId),
					LuceneDocumentFactory.isDeleted(getIndexReader(), docId));
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
			super(new HashSet<IResource>());
		}
		@Override
		public void collect(int docId) throws IOException {
			Logger.traceIndexing("  Adding doc#{} (deleted={}) to search results", (getDocBase() + docId),
					LuceneDocumentFactory.isDeleted(getIndexReader(), docId));
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
