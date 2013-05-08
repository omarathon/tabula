package uk.ac.warwick.tabula.services

import java.io.File
import java.io.FileNotFoundException
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.apache.lucene.analysis._
import org.apache.lucene.document.Field._
import org.apache.lucene.document._
import org.apache.lucene.index.FieldInfo.IndexOptions
import org.apache.lucene.index._
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search._
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import org.joda.time.DateTime
import org.joda.time.Duration
import org.springframework.beans.factory.annotation._
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Closeables._
import uk.ac.warwick.tabula.helpers.Stopwatches._
import uk.ac.warwick.tabula.helpers._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.userlookup.User
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.DisposableBean
import org.apache.lucene.analysis.core._
import org.apache.lucene.analysis.miscellaneous._
import uk.ac.warwick.spring.Wire
import org.apache.lucene.search.SearcherLifetimeManager.PruneByAge
import scala.collection.GenTraversableOnce
import language.implicitConversions

trait CommonQueryMethods[A] { self: AbstractIndexService[A] =>

	/**
	 * Get recent items.
	 */
	def listRecent(start: Int, count: Int): Seq[A] = {
		val min = new DateTime().minusYears(2)
		val docs = search(
			query = NumericRangeQuery.newLongRange(UpdatedDateField, min.getMillis, null, true, true),
			sort = reverseDateSort,
			offset = start,
			max = count)			
		docs transform { toItem(_) }
	}
	
}

trait QueryHelpers[A] { self: AbstractIndexService[A] =>
	private def boolean(occur: Occur, queries: Query*): Query = {
		val query = new BooleanQuery
		for (q <- queries) query.add(q, occur)
		query
	}

	def all(queries: Query*): Query = boolean(Occur.MUST, queries: _*)
	def some(queries: Query*): Query = boolean(Occur.SHOULD, queries: _*)

	def termQuery(name: String, value: String) = new TermQuery(new Term(name, value))
	def term(pair: Pair[String, String]) = new TermQuery(new Term(pair._1, pair._2))

	def dateSort = new Sort(new SortField(UpdatedDateField, SortField.Type.LONG, false))
	def reverseDateSort = new Sort(new SortField(UpdatedDateField, SortField.Type.LONG, true))
}

class RichSearchResults(seq: Seq[Document]) {
	def first = seq.headOption	
	def transform[A](f: Document => GenTraversableOnce[A]) = seq.flatMap(f)
	def size = seq.size
}

trait RichSearchResultsCreator {
	implicit def toRichSearchResults(seq: Seq[Document]) = new RichSearchResults(seq)
}

object RichSearchResultsCreator extends RichSearchResultsCreator

abstract class AbstractIndexService[A] 
		extends CommonQueryMethods[A] 
			with QueryHelpers[A]
			with SearchHelpers[A]
			with FieldGenerators
			with RichSearchResultsCreator
			with InitializingBean 
			with Logging 
			with DisposableBean {
	
	final val LuceneVersion = Version.LUCENE_40
	
	// largest batch of items we'll load in at once.
	val MaxBatchSize: Int
	
	// largest batch of items we'll load in at once during scheduled incremental index.
	val IncrementalBatchSize: Int
		
	@Value("${filesystem.create.missing}") var createMissingDirectories: Boolean = _
	
	// Are we indexing now?
	var indexing: Boolean = false
	
	var indexPath: File

	var lastIndexTime: Option[DateTime] = None
	var lastIndexDuration: Option[Duration] = None

	// HFC-189 Reopen index every 2 minutes, even if not the indexing instance.
	val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

	/**
	 * Wrapper around the indexing code so that it is only running once.
	 * If it's already running, the code is skipped.
	 * We only try indexing once a minute so thmiere's no need to bother about
	 * tight race conditions here.
	 */
	def ifNotIndexing(work: => Unit) =
		if (indexing)
			logger.info("Skipped indexing because the indexer is already/still running.")
		else
			try { indexing = true; work }
			finally indexing = false
			
	val analyzer: Analyzer
	lazy val indexAnalyzer = analyzer
	
	// QueryParser isn't thread safe, hence why this is a def
	def parser = new QueryParser(LuceneVersion, "", analyzer)

	/**
	 * When an index run finishes, we note down the date of the newest item,
	 * so we know where to check from next time.
	 */
	var mostRecentIndexedItem: Option[DateTime] = None
	
	final val IndexReopenPeriodInSeconds = 20

	override def afterPropertiesSet {
		if (!indexPath.exists) {
			if (createMissingDirectories) indexPath.mkdirs
			else throw new IllegalStateException("Index path missing", new FileNotFoundException(indexPath.getAbsolutePath))
		}
		if (!indexPath.isDirectory) throw new IllegalStateException("Index path not a directory: " + indexPath.getAbsolutePath)
		
		// don't want this. http://www.lifeinthefastlane.ca/wp-content/uploads/2008/12/santa_claus_13sfw.jpg
		BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE)

		initialiseSearching

		// Reopen the index reader periodically, else it won't notice changes.
		executor.scheduleAtFixedRate(Runnable {
			//logger.debug("Trying to reopen index")
			try reopen catch { case e: Throwable => logger.error("Index service reopen failed", e) }
			try prune catch { case e: Throwable => logger.error("Pruning old searchers failed", e) }
		}, IndexReopenPeriodInSeconds, IndexReopenPeriodInSeconds, TimeUnit.SECONDS)
	}

	override def destroy {
		executor.shutdown()
	}

	/**
	 * Incremental index. Can be run often.
	 * Has a limit to how many items it will load at once, but subsequent indexes
	 * will get through those. There would have to be hundreds of items
	 * per minute in order for the index to lag behind, and even then it would catch
	 * up as soon as it reached a quiet time.
	 */
	def index() = transactional() {
		ifNotIndexing {
			val stopWatch = StopWatch()
			stopWatch.record("Incremental index") {
				val startDate = latestIndexItem
				val newItems = listNewerThan(startDate, IncrementalBatchSize)
				if (newItems.isEmpty) {
					logger.debug("No new items to index.")
				} else {
					if (debugEnabled) logger.debug("Indexing items from " + startDate)
					doIndexItems(newItems)
				}
			}
			lastIndexDuration = Some(new Duration(stopWatch.getTotalTimeMillis))
			lastIndexTime = Some(new DateTime())
		}
	}
	
	protected def listNewerThan(startDate: DateTime, batchSize: Int): Seq[A]

	def indexFrom(startDate: DateTime) = transactional() {
		ifNotIndexing {
			doIndexItems(listNewerThan(startDate, MaxBatchSize))
		}
	}

	/**
	 * Indexes a specific given list of items.
	 */
	def indexItems(items: Seq[A]) = transactional() {
		ifNotIndexing { doIndexItems(items) }
	}

	private def doIndexItems(items: Seq[A]) {
		logger.debug("Writing to the index at " + indexPath + " with analyzer " + indexAnalyzer)
		val writerConfig = new IndexWriterConfig(LuceneVersion, indexAnalyzer)
		closeThis(new IndexWriter(FSDirectory.open(indexPath), writerConfig)) { writer =>
			for (item <- items) {
				updateMostRecent(item)
				writer.updateDocument(uniqueTerm(item), toDocument(item))
			}
			if (debugEnabled) logger.debug("Indexed " + items.size + " items")
		}
		reopen // not really necessary as we reopen periodically anyway
	}

	/**
	 * If this item is the newest item this service has seen, save the date
	 * so we know where to start from next time.
	 */
	private def updateMostRecent(item: A) {
		val shouldUpdate = mostRecentIndexedItem.map { _ isBefore getUpdatedDate(item) }.getOrElse { true }
		if (shouldUpdate)
			mostRecentIndexedItem = Some(getUpdatedDate(item))
	}
	
	protected def getUpdatedDate(item: A): DateTime
	
	val UpdatedDateField: String

	/**
	 * Either get the date of the most recent item we've process in this JVM
	 * or look up the most recent item in the index, or else index everything
	 * from the past year.
	 */
	def latestIndexItem: DateTime = {
		mostRecentIndexedItem.map { _.minusMinutes(1) }.getOrElse {
			// extract possible list of UpdatedDateField values from possible newest item and get possible first value as a Long.
			documentValue(newest(), UpdatedDateField)
				.map { v => new DateTime(v.toLong) }
				.getOrElse {
					logger.info("No recent document found, indexing the past year")
					new DateTime().minusYears(1)
				}
			// TODO change to just a few weeks after first deploy of this -
			// this is just to get all historical data indexed, after which we won't ever
			// be so out of date.
		}
	}

	/**
	 * Try to get the first value out of a document field.
	 */
	def documentValue(doc: Option[Document], key: String): Option[String] = doc.flatMap { _.getValues(key).headOption }
	def documentValue(doc: Document, key: String): Option[String] = doc.getValues(key).headOption

	val IdField: String

	/**
	 * If an existing Document is in the index with this term, it
	 * will be replaced.
	 */
	private def uniqueTerm(item: A) = new Term(IdField, getId(item))
	protected def getId(item: A): String
	
	/**
	 * TODO reuse one Document and set of Fields for all items
	 */
	protected def toDocument(item: A): Document
	
	protected def toId(doc: Document) = documentValue(doc, IdField)
	protected def toItem(id: String): Option[A]
	protected def toItem(doc: Document): Option[A] = { toId(doc) flatMap (toItem) }

}

trait SearchHelpers[A] extends Logging with RichSearchResultsCreator { self: AbstractIndexService[A] =>

	/**
	 * SearcherManager handles returning a fresh IndexSearcher for each search,
	 * and managing old IndexSearcher instances that might still be in use. Use
	 * acquire() and release() to get IndexSearchers, and call maybeReopen() when
	 * the index has changed (i.e. after an index)
	 */
	var searcherManager: SearcherManager = _
	
	/**
	 * SearcherLifetimeManager allows a specific IndexSearcher state to be
	 * retrieved later by passing a token. This allows stateful search context
	 * per-user. If you don't have a token, use the regular SearcherManager
	 * to do the initial creation.
	 */
	var searcherLifetimeManager: SearcherLifetimeManager = _

	/**
	 * Find the newest item that was indexed, by searching by
	 * UpdatedDateField and sorting in descending date order.
	 *
	 * @param since Optional lower bound for date - recommended if possible, as it is faster.
	 */
	def newest(since: DateTime = null): Option[Document] = {
		initialiseSearching

		if (searcherManager == null) { // usually if we've never indexed before, no index file
			None
		} else {
			val min: Option[JLong] = Option(since).map { _.getMillis }
			val docs = search(
				query = NumericRangeQuery.newLongRange(UpdatedDateField, min.orNull, null, true, true),
				sort = new Sort(new SortField(UpdatedDateField, SortField.Type.LONG, true)),
				max = 1)
			docs.first // Some(firstResult) or None if empty
		}
	}

	protected def initialiseSearching = {	
		if (searcherManager == null) {
			try {
				logger.debug("Opening a new searcher manager at " + indexPath)
				searcherManager = new SearcherManager(FSDirectory.open(indexPath), null)
			} catch {
				case e: IndexNotFoundException => logger.warn("No index found.")
			}
		}
		if (searcherLifetimeManager == null) {
			try {
				logger.debug("Opening a new searcher lifetime manager at " + indexPath)
				searcherLifetimeManager = new SearcherLifetimeManager
			} catch {
				case e: IllegalStateException => logger.warn("Could not create SearcherLifetimeManager.")
			}
		}
	}

	/**
	 * Sets up a new IndexSearcher with a refreshed IndexReader so that
	 * subsequent searches see the results of recent index changes.
	 */
	protected def reopen = {
		//logger.debug("Reopening index")
		
		initialiseSearching
		if (searcherManager != null) searcherManager.maybeRefresh
	}

	/**
	 * Remove saved searchers over 20 minutes old
	 */
	protected def prune = {
		val ageInSeconds = 20*60
		initialiseSearching
		if (searcherLifetimeManager != null) searcherLifetimeManager.prune(new PruneByAge(ageInSeconds))
	}
	
	def search(query: Query, max: Int, sort: Sort = null, offset: Int = 0): RichSearchResults = doSearch(query, Some(max), sort, offset)
	def search(query: Query): RichSearchResults = doSearch(query, None, null, 0)
	def search(query: Query, sort: Sort): RichSearchResults = doSearch(query, None, sort, 0)
	def search(query: Query, max: Int, sort: Sort, last: Option[ScoreDoc], token: Option[Long]): PagingSearchResult = 
		doPagingSearch(query, Option(max), Option(sort), last, token)

	private def doSearch(query: Query, max: Option[Int], sort: Sort, offset: Int): RichSearchResults = {
		initialiseSearching
		if (searcherManager == null) {
			logger.warn("Tried to search but no searcher manager is available")
			Seq.empty
		} else acquireSearcher { searcher =>
			logger.debug("Running search for query: " + query)
			
			val maxResults = max.getOrElse(searcher.getIndexReader.maxDoc)
			val results =
				if (sort == null) searcher.search(query, null, searcher.getIndexReader.maxDoc)
				else searcher.search(query, null, searcher.getIndexReader.maxDoc, sort)
			transformResults(searcher, results, offset, maxResults)
		}
	}

	private def acquireSearcher[A](work: IndexSearcher => A): A = {
		val searcher = searcherManager.acquire
		try work(searcher)
		finally searcherManager.release(searcher)
	}

	private def transformResults(searcher: IndexSearcher, results: TopDocs, offset: Int, max: Int) = {
		val hits = results.scoreDocs
		hits.toStream.drop(offset).take(max).map { hit => searcher.doc(hit.doc) }.toList
	}
	
	case class PagingSearchResult(val results: RichSearchResults, val last: Option[ScoreDoc], val token: Long, val total: Int)
	
	private def doPagingSearch(query: Query, max: Option[Int], sort: Option[Sort], lastDoc: Option[ScoreDoc], token: Option[Long]): PagingSearchResult = {
		// guard
		initialiseSearching
		
		val (newToken, searcher) = acquireSearcher(token)
		if (searcher == null) 
			throw new IllegalStateException("Original IndexSearcher has expired.")
		
		logger.debug("Running paging search for query: " + query)
			
		try {
			val maxResults = max.getOrElse(searcher.getIndexReader.maxDoc)
			val results = (lastDoc, sort) match {
				case (None, None) => searcher.search(query, maxResults)
				case (after:Some[ScoreDoc], None) => searcher.searchAfter(after.get, query, maxResults)
				case (after:Some[ScoreDoc], sort: Some[Sort]) => searcher.searchAfter(lastDoc.get, query, maxResults, sort.get)
				case (None, sort: Some[Sort]) => searcher.search(query, maxResults, sort.get)
			}
			
			val hits = results.scoreDocs
			val totalHits = results.totalHits
			hits match {
				case Array() => PagingSearchResult(Nil, None, newToken, 0)
				case _ => {
					val hitsOnThisPage = hits.length
					PagingSearchResult(hits.toStream.map (hit => searcher.doc(hit.doc)).toList, Option(hits(hitsOnThisPage-1)), newToken, totalHits)
				}
			}
		}
		finally searcherLifetimeManager.release(searcher)
	}
	
	private def acquireSearcher(token: Option[Long]): (Long, IndexSearcher) = {
		var searcher: IndexSearcher = null
		var newToken: Long = 0
		
		token match {
			case None => {
				searcher = searcherManager.acquire
				newToken = searcherLifetimeManager.record(searcher)
			}
			case Some(t) => {
				searcher = searcherLifetimeManager.acquire(token.get)
				newToken = t
			}
		}
		
		(newToken, searcher)
	}
}

trait FieldGenerators {
	protected def seqField(key: String, ids: Seq[_]) = {
		new TextField(key, ids.mkString(" "), Store.NO)
	}

	protected def plainStringField(name: String, value: String, isStored: Boolean = true) = {
		val storage = if (isStored) Store.YES else Store.NO
		new StringField(name, value, storage)
	}
	
	protected def tokenisedStringField(name: String, value: String, isStored: Boolean = true) = {
		val storage = if (isStored) Store.YES else Store.NO
		new TextField(name, value, storage)
	}

	protected def dateField(name: String, value: DateTime) = new LongField(name, value.getMillis, Store.YES)
}