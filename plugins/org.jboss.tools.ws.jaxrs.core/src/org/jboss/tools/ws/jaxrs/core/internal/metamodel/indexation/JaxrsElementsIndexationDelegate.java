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
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
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
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;

/**
 * @author xcoulon
 * 
 */
public class JaxrsElementsIndexationDelegate {

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
	public JaxrsElementsIndexationDelegate() throws CoreException {
		try {
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
		try {
			Logger.traceIndexing("Indexing {} after addition...", element.getName());
			final Document doc = LuceneDocumentFactory.createDocument(element);
			Logger.debugIndexing("Adding JAX-RS Element into index with following fields: {}", doc.getFields());
			indexWriter.addDocument(doc);
			indexWriter.commit();
		} catch (Exception e) {
			Logger.error("Failed to index the JAX-RS Element " + element, e);
		} finally {
			Logger.traceIndexing(" Done indexing {}.", element.getName());
		}
	}

	public void indexElement(final IJaxrsEndpoint endpoint) {
		try {
			Logger.traceIndexing("Indexing {} after addition...", endpoint);
			final Document doc = LuceneDocumentFactory.createDocument(endpoint);
			Logger.debugIndexing("Adding JAX-RS Endpoint into index with following fields: {}", doc.getFields());
			indexWriter.addDocument(doc);
			indexWriter.commit();
		} catch (Exception e) {
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
		} catch (Exception e) {
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
		} catch (Exception e) {
			Logger.error("Failed to re-index the JAX-RS Endpoint " + endpoint, e);
		} finally {
			Logger.traceIndexing(" Done re-indexing {}.", endpoint);
		}
	}

	/**
	 * Removes the given element from the index.
	 * 
	 * @param element
	 */
	public void unindexElement(final IJaxrsElement element) {
		try {
			Logger.debugIndexing("Unindexing {} after removal...", element.getName());
			final Term identifierTerm = LuceneDocumentFactory.getIdentifierTerm(element);
			indexWriter.deleteDocuments(identifierTerm);
			indexWriter.commit();
		} catch (Exception e) {
			Logger.error("Failed to unindex the JAX-RS Element " + element, e);
		} finally {
			Logger.traceIndexing("Done unindexing {}.", element.getName());
		}
	}

	/**
	 * Removes the given Endpoint from the index.
	 * 
	 * @param element
	 */
	// TODO: avoid code duplication with unindexElement(IJaxrsElement) above
	public void unindexEndpoint(final JaxrsEndpoint endpoint) {
		try {
			Logger.debugIndexing("Unindexing {} after removal...", endpoint);
			final Term identifierTerm = LuceneDocumentFactory.getIdentifierTerm(endpoint);
			indexWriter.deleteDocuments(identifierTerm);
			indexWriter.commit();
		} catch (Exception e) {
			Logger.error("Failed to unindex the JAX-RS Element " + endpoint, e);
		} finally {
			Logger.traceIndexing("Done unindexing {}.", endpoint);
		}
	}

	/**
	 * Performs a {@link BooleanQuery} based on the given {@link TermQuery},
	 * assuming that each query element is mandatory. This method returns a
	 * single document identifier (or null if no document matched).
	 * 
	 * @param queries
	 * @return the document identifier matching the query, or null if no
	 *         document matched
	 */
	public String searchSingle(final Term... terms) {
		try {
			final IndexSearcher searcher = getNewIndexSearcherIfNeeded();
			Logger.traceIndexing("Using IndexReader (current={} / hasDeletions={}) containing {} documents",
					indexReader.isCurrent(), indexReader.hasDeletions(), indexReader.numDocs());
			BooleanQuery query = new BooleanQuery();
			if (terms != null) {
				for (Term term : terms) {
					query.add(new BooleanClause(new TermQuery(term), Occur.MUST));
				}
			}
			Logger.traceIndexing("Searching single document matching {}", (Object[]) query.getClauses());
			final TopDocs result = searcher.search(query, 1);
			if (result.totalHits >= 1) {
				int docIndex = result.scoreDocs[0].doc;
				final Document doc = searcher.doc(docIndex);
				final String docIdentifier = doc.get(LuceneFields.FIELD_IDENTIFIER);
				// Logger.traceIndexing(," Found single document #{}",
				// docIdentifier);
				return docIdentifier;
			}
		} catch (Exception e) {
			Logger.error("Failed to search for JAX-RS element in index", e);
		}
		Logger.traceIndexing(" Not document matched the query.");
		return null;
	}

	/**
	 * Performs a {@link BooleanQuery} based on the given {@link TermQuery},
	 * assuming that each query element is mandatory. This method returns a list
	 * of document identifiers (or an empty list if no document matched).
	 * 
	 * @param queries
	 * @return the document identifier matching the query, or null if no
	 *         document matched
	 */
	public List<String> searchAll(final Term... terms) {
		try {
			final IndexSearcher searcher = getNewIndexSearcherIfNeeded();
			Logger.traceIndexing("Using IndexReader (current={} / hasDeletions={}) containing {} documents",
					indexReader.isCurrent(), indexReader.hasDeletions(), indexReader.numDocs());
			BooleanQuery query = new BooleanQuery();
			if (terms != null) {
				for (Term term : terms) {
					query.add(new BooleanClause(new TermQuery(term), Occur.MUST));
				}
			}
			Logger.traceIndexing("Searching documents matching {}", query.toString());
			final AllResultsCollector collector = new AllResultsCollector(searcher);
			searcher.search(query, collector);
			final List<String> docIdentifiers = collector.getDocIdentifiers();
			Logger.traceIndexing(" Found {} matching documents", docIdentifiers.size());
			return docIdentifiers;
		} catch (Exception e) {
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
	public int count(final Term... terms) {
		try {
			Logger.traceIndexing("Using IndexReader (current={} / hasDeletions={}) containing {} documents",
					indexReader.isCurrent(), indexReader.hasDeletions(), indexReader.numDocs());
			final IndexSearcher searcher = getNewIndexSearcherIfNeeded();
			BooleanQuery query = new BooleanQuery();
			for (Term term : terms) {
				query.add(new BooleanClause(new TermQuery(term), Occur.MUST));
			}
			final TotalHitCountCollector collector = new TotalHitCountCollector();
			Logger.traceIndexing("Counting documents matching {}...", (Object[]) query.getClauses());
			searcher.search(query, collector);
			final int totalHits = collector.getTotalHits();
			Logger.traceIndexing(" Found {} matching documents", totalHits);
			return totalHits;
		} catch (Exception e) {
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

	static class AllResultsCollector extends Collector {

		private IndexReader indexReader;

		final List<String> docIdentifiers = new ArrayList<String>();

		private int docBase;

		public AllResultsCollector(final IndexSearcher searcher) {
		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {

		}

		@Override
		public void collect(int docId) throws IOException {
			Logger.traceIndexing("  Adding doc#{} (deleted={}) to search results", (docBase + docId),
					indexReader.isDeleted(docId));
			final Document document = indexReader.document(docId);
			final String docIdentifier = document.get(LuceneFields.FIELD_IDENTIFIER);
			final String docCategory = document.get(LuceneFields.FIELD_CATEGORY);
			Logger.traceIndexing("  Retrieved document #{} ({})", docIdentifier, docCategory);
			docIdentifiers.add(docIdentifier);
		}

		@Override
		public void setNextReader(IndexReader indexReader, int docBase) throws IOException {
			this.indexReader = indexReader;
			this.docBase = docBase;
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

		public List<String> getDocIdentifiers() throws CorruptIndexException, IOException {
			return docIdentifiers;
		}

	}

}
