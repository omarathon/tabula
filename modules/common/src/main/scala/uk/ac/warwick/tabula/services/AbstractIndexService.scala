package uk.ac.warwick.tabula.services

import java.io.{Closeable, File, FileNotFoundException}
import com.fasterxml.jackson.databind.ObjectMapper
import dispatch.classic.thread.ThreadSafeHttpClient
import dispatch.classic.{url, thread, Http}
import org.apache.http.client.params.{CookiePolicy, ClientPNames}
import org.apache.http.entity.{ContentType, ByteArrayEntity}
import org.apache.lucene.analysis._
import org.apache.lucene.document.Field._
import org.apache.lucene.document._
import org.apache.lucene.index._
import org.apache.lucene.queryparser.classic.{QueryParserBase, QueryParser}
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search._
import org.apache.lucene.store.{Directory, FSDirectory}
import org.apache.lucene.util.BytesRef
import org.joda.time.DateTime
import org.joda.time.Duration
import org.springframework.beans.factory.annotation._
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.sso.client.SSOConfiguration
import uk.ac.warwick.sso.client.trusted.{TrustedApplication, SSOConfigTrustedApplicationsManager}
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Closeables._
import uk.ac.warwick.tabula.helpers.Stopwatches._
import uk.ac.warwick.tabula.helpers._
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.DisposableBean
import org.apache.lucene.search.SearcherLifetimeManager.PruneByAge
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import language.implicitConversions
import scala.util.parsing.json.JSON

trait CommonQueryMethods[A] extends TaskBenchmarking { self: AbstractIndexService[A] =>

	/**
	 * Get recent items.
	 */
	def listRecent(start: Int, count: Int): Seq[A] = {
		val min = new DateTime().minusYears(2)
		val docs = benchmarkTask("Search for recent items") {
			search(
				query = NumericRangeQuery.newLongRange(UpdatedDateField, min.getMillis, null, true, true),
				sort = reverseDateSort,
				offset = start,
				max = count
			)
		}
		benchmarkTask("Transform documents to items") { docs.transformAll { docs => toItems(docs) } }
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
	def term(pair: (String, String)) = new TermQuery(new Term(pair._1, pair._2))

	def dateRange(min: DateTime, max: DateTime) = NumericRangeQuery.newLongRange(UpdatedDateField, min.getMillis, max.getMillis, true, true)
	def dateSort = new Sort(new SortField(UpdatedDateField, SortField.Type.LONG, false))
	def reverseDateSort = new Sort(new SortField(UpdatedDateField, SortField.Type.LONG, true))
}

class RichSearchResults(seq: Seq[Document]) {
	def first = seq.headOption
	def transform[A](f: Document => Option[A]): Seq[A] = seq.map(f).flatten
	def transformAll[A](f: Seq[Document] => Seq[A]): Seq[A] = f(seq)
	def size = seq.size
}

trait RichSearchResultsCreator {
	implicit def toRichSearchResults(seq: Seq[Document]) = new RichSearchResults(seq)
}

object RichSearchResultsCreator extends RichSearchResultsCreator

/** Trait for overriding in tests.
	* @see RAMDirectoryOverride
	*/
trait OpensLuceneDirectory {
	protected def openDirectory(): Directory
}

abstract class AbstractIndexService[A]
		extends CommonQueryMethods[A]
			with QueryHelpers[A]
			with SearchHelpers[A]
			with FieldGenerators
			with RichSearchResultsCreator
			with OpensLuceneDirectory
			with InitializingBean
			with Logging
			with DisposableBean {

	// largest batch of items we'll load in at once.
	val MaxBatchSize: Int

	// largest batch of items we'll load in at once during scheduled incremental index.
	val IncrementalBatchSize: Int

	var indexPath: File
	val analyzer: Analyzer
	val UpdatedDateField: String
	val IdField: String

	@Value("${filesystem.create.missing}") var createMissingDirectories: Boolean = _
	@Value("${tabula.yearZero}") var yearZero: Int = _
	@Value("${module.context}") var moduleContext: String = _
	@Autowired var features: Features = _
	@Autowired var objectMapper: ObjectMapper = _

	def searchOverHttp = features.searchOnApiComponent && moduleContext != "/api"

	final val IndexReopenPeriodInSeconds = 20

	// Are we indexing now?
	var indexing: Boolean = false

	var lastIndexTime: Option[DateTime] = None
	var lastIndexDuration: Option[Duration] = None

	// HFC-189 Reopen index every 2 minutes, even if not the indexing instance.
	val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

	lazy val indexAnalyzer = analyzer

	/**
	 * When an index run finishes, we note down the date of the newest item,
	 * so we know where to check from next time.
	 */
	var mostRecentIndexedItem: Option[DateTime] = None

	/**
	* Wrapper around the indexing code so that it is only running once.
	 * If it's already running, the code is skipped.
	 * We only try indexing once a minute so there's no need to bother about
	 * tight race conditions here.
	 */
	def guardMultipleIndexes(work: => Unit) = this.synchronized(work)

	// QueryParser isn't thread safe, hence why this is a def
	def parser = new NumericRangeQueryParser(Seq(UpdatedDateField), "", analyzer)


	override def afterPropertiesSet() {
		if (!indexPath.exists) {
			if (createMissingDirectories) indexPath.mkdirs
			else throw new IllegalStateException("Index path missing", new FileNotFoundException(indexPath.getAbsolutePath))
		}
		if (!indexPath.isDirectory) throw new IllegalStateException("Index path not a directory: " + indexPath.getAbsolutePath)

		// don't want this. http://www.lifeinthefastlane.ca/wp-content/uploads/2008/12/santa_claus_13sfw.jpg
		BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE)

		initialiseSearching()

		// Reopen the index reader periodically, else it won't notice changes.
		executor.scheduleAtFixedRate(Runnable {
			//logger.debug("Trying to reopen index")
			try reopen catch { case e: Throwable => logger.error("Index service reopen failed", e) }
			try prune() catch { case e: Throwable => logger.error("Pruning old searchers failed", e) }
		}, IndexReopenPeriodInSeconds, IndexReopenPeriodInSeconds, TimeUnit.SECONDS)
	}

	override def destroy() {
		executor.shutdown()
	}

	protected override def openDirectory(): Directory = FSDirectory.open(indexPath.toPath)

	/**
	 * Incremental index. Can be run often.
	 * Has a limit to how many items it will load at once, but subsequent indexes
	 * will get through those. There would have to be hundreds of items
	 * per minute in order for the index to lag behind, and even then it would catch
	 * up as soon as it reached a quiet time.
	 */
	def incrementalIndex() = transactional(readOnly = true) {
		guardMultipleIndexes {
			val stopWatch = StopWatch()
			stopWatch.record("Incremental index") {
				val startDate = latestIndexItem
				val newItems = listNewerThan(startDate, IncrementalBatchSize)
				if (newItems.isEmpty) {
					logger.debug("No new items to index.")
				} else {
					if (debugEnabled) logger.debug("Indexing items from " + startDate)
					doIndexItems(newItems, isIncremental = true)
				}
			}
			lastIndexDuration = Some(new Duration(stopWatch.getTotalTimeMillis))
			lastIndexTime = Some(new DateTime())
		}
	}

	protected def listNewerThan(startDate: DateTime, batchSize: Int): TraversableOnce[A]

	def indexFrom(startDate: DateTime) = transactional(readOnly = true) {
		guardMultipleIndexes {
			val newer = listNewerThan(startDate, MaxBatchSize)
			doIndexItems(newer, isIncremental = true)
			newer match {
				case c: Closeable => c.close()
				case _ =>
			}
		}
	}

	/**
	 * Indexes a specific given list of items.
	 */
	def indexItems(items: TraversableOnce[A]) = transactional(readOnly = true) {
		guardMultipleIndexes { doIndexItems(items, isIncremental = false) }
	}

	def indexItemsWithoutNewTransaction(items: TraversableOnce[A]) = {
		guardMultipleIndexes { doIndexItems(items, isIncremental = false) }
	}

	// Overridable - configure index writer
	def configureIndexWriter(config: IndexWriterConfig): Unit = {}

	protected def doIndexItems(items: TraversableOnce[A], isIncremental: Boolean) {
		logger.debug("Writing to the index at " + indexPath + " with analyzer " + indexAnalyzer)
		val writerConfig = new IndexWriterConfig(indexAnalyzer)
		configureIndexWriter(writerConfig)
		closeThis(new IndexWriter(openDirectory(), writerConfig)) { writer =>
			var i = 0
			for (item <- items) {
				tryDescribe(s"indexing $item") {
					if (isIncremental) {
						doUpdateMostRecent(item)
					}
					toDocuments(item).foreach { doc =>
						writer.updateDocument(uniqueTerm(item), doc)
					}

					i += 1
				}
			}
			if (debugEnabled) logger.debug("Indexed " + i + " items") // Don't use .size, it goes through Scrollables again
		}
		reopen // not really necessary as we reopen periodically anyway
	}

	/**
	 * If this item is the newest item this service has seen, save the date
	 * so we know where to start from next time.
	 */
	private def doUpdateMostRecent(item: A) {
		val shouldUpdate = mostRecentIndexedItem match {
			case Some(date) => getUpdatedDate(item).isAfter(date)
			case _ => true
		}
		if (shouldUpdate)
			mostRecentIndexedItem = Some(getUpdatedDate(item))
	}

	protected def getUpdatedDate(item: A): DateTime


	/**
	 * Either get the date of the most recent item we've process in this JVM
	 * or look up the most recent item in the index, or else index everything
	 * from the past year.
	 */
	def latestIndexItem: DateTime = {
		mostRecentIndexedItem.fold{
			// extract possible list of UpdatedDateField values from possible newest item and get possible first value as a Long.
			documentValue(newest(), UpdatedDateField).fold{
				logger.info("No recent document found, indexing since Tabula year zero")
				new DateTime(yearZero, 1, 1, 0, 0)
			}(v => new DateTime(v.toLong).minusMinutes(10))
		}(_.minusSeconds(30))
	}

	/**
	 * Try to get the first value out of a document field.
	 */
	def documentValue(doc: Option[Document], key: String): Option[String] = doc.flatMap { _.getValues(key).headOption }
	def documentValue(doc: Document, key: String): Option[String] = doc.getValues(key).headOption

	/**
	 * If an existing Document is in the index with this term, it
	 * will be replaced.
	 */
	private def uniqueTerm(item: A) = new Term(IdField, getId(item))
	protected def getId(item: A): String

	/**
	 * TODO reuse one Document and set of Fields for all items
	 */
	protected def toDocuments(item: A): Seq[Document]

	protected def toId(doc: Document) = documentValue(doc, IdField)
	protected def toItems(docs: Seq[Document]): Seq[A]

}

case class PagingSearchResultItems[A](items: Seq[A], lastscore: Option[ScoreDoc], token: Long, total: Int) {
	// need this pattern matcher as brain-dead IndexSearcher.searchAfter returns an object containing ScoreDocs,
	// and expects a ScoreDoc in its method signature, yet in its implementation throws an exception unless you
	// pass a specific subclass of FieldDoc.
	def last: Option[FieldDoc] = lastscore match {
		case None => None
		case Some(f:FieldDoc) => Some(f)
		case _ => throw new ClassCastException("Lucene did not return an Option[FieldDoc] as expected")
	}

	def getTokens: String = last.fold("empty")(lastscore =>
		lastscore.doc + "/" + lastscore.fields(0) + "/" + token)
}

case class PagingSearchResult(results: RichSearchResults, last: Option[ScoreDoc], token: Long, total: Int) {
	// Turn results containing documents into results containing actual items.
	def transformAll[A](fn: (Seq[Document] => Seq[A] )) =
		PagingSearchResultItems[A](results.transformAll(fn), last, token, total)
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

	def searchOverHttp: Boolean

	/**
	 * Find the newest item that was indexed, by searching by
	 * UpdatedDateField and sorting in descending date order.
	 *
	 * @param since Optional lower bound for date - recommended if possible, as it is faster.
	 */
	def newest(since: DateTime = null): Option[Document] = {
		initialiseSearching()

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

	protected def initialiseSearching() = {
		if (searcherManager == null) {
			try {
				logger.debug("Opening a new searcher manager at " + indexPath)
				searcherManager = new SearcherManager(openDirectory(), null)
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

		initialiseSearching()
		if (searcherManager != null) searcherManager.maybeRefresh
	}

	/**
	 * Remove saved searchers over 3 minutes old
	 */
	protected def prune() = {
		val ageInSeconds = 3*60
		initialiseSearching()
		if (searcherLifetimeManager != null) searcherLifetimeManager.prune(new PruneByAge(ageInSeconds))
	}

	def search(query: Query): RichSearchResults = doSearch(query, None, null, 0)
	def search(query: Query, sort: Sort): RichSearchResults = doSearch(query, None, sort, 0)
	def search(query: Query, max: Int, sort: Sort = null, offset: Int = 0): RichSearchResults = doSearch(query, Some(max), sort, offset)

	def search(query: Query, max: Int, sort: Sort, last: Option[ScoreDoc], token: Option[Long]): PagingSearchResult =
		doPagingSearch(query, Option(max), Option(sort), last, token)

	lazy val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	lazy val currentApplication = new SSOConfigTrustedApplicationsManager(SSOConfiguration.getConfig).getCurrentApplication

	private def doSearch(query: Query, max: Option[Int], sort: Sort, offset: Int): RichSearchResults = {
		if (searchOverHttp) {
			// Convert the request to JSON
			val requestMap =
				Map(
					"queryString" -> query.toString,
					"offset" -> offset
				) ++
				max.map { m => Map("max" -> m) }.getOrElse(Map()) ++
				Option(sort).map { s => Map("sort" -> s.getSort.map { sortField =>
					Map(
						"field" -> sortField.getField,
						"type" -> sortField.getType.name(),
						"reverse" -> sortField.getReverse
					)
				}) }.getOrElse(Map())

			def handler = { (headers: Map[String,Seq[String]], req: dispatch.classic.Request) =>
				req >- { (json) =>
					JSON.parseFull(json) match {
						case Some(json: Map[String, Any] @unchecked) =>
							json.get("results") match {
								case Some(docs: Seq[Map[String, Any]] @unchecked) =>
									docs.map { fields =>
										val doc = new Document

										fields.foreach { case (key, value) =>
											doc.add(value match {
												case s: String => plainStringField(key, s)
												case d: Double => doubleField(key, d)
												case n: Number => new LongField(key, n.longValue(), Store.YES)
												case o => throw new IllegalArgumentException("Unexpected field type: " + o.getClass.getName)
											})
										}

										doc
									}
								case _ => Nil
							}
						case _ => Nil
					}
				}
			}

			val endpoint = "https://augustus.warwick.ac.uk/api/v1/index/audit"
			val encryptedCertificate = currentApplication.encode("tabula-search-api-user", endpoint)

			val req =
				url(endpoint) <:< Map(
					TrustedApplication.HEADER_CERTIFICATE -> encryptedCertificate.getCertificate,
					TrustedApplication.HEADER_PROVIDER_ID -> encryptedCertificate.getProviderID,
					TrustedApplication.HEADER_SIGNATURE -> encryptedCertificate.getSignature,
					"Content-Type" -> "application/json"
				) << objectMapper.writeValueAsBytes(requestMap)

			http.when(_ != 500)(req >:+ handler)
		} else {
			initialiseSearching()
			if (searcherManager == null) {
				logger.warn("Tried to search but no searcher manager is available")
				Seq.empty
			} else acquireSearcher { searcher =>
				logger.debug("Running search for query: " + query)

				val maxResults = max.getOrElse(searcher.getIndexReader.maxDoc)
				val results =
					if (sort == null) searcher.search(query, null, offset + maxResults)
					else if (searcher.getIndexReader.maxDoc <= 0) new TopDocs(0, Array(), 0f)
					else searcher.search(query, null, offset + maxResults, sort)
				transformResults(searcher, results, offset, maxResults)
			}
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

	private def doPagingSearch(query: Query, max: Option[Int], sort: Option[Sort], lastDoc: Option[ScoreDoc], token: Option[Long]): PagingSearchResult = {
		// guard
		initialiseSearching()

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
				case _ =>
					val hitsOnThisPage = hits.length
					PagingSearchResult(hits.toStream.map (hit => searcher.doc(hit.doc)).toList, Option(hits(hitsOnThisPage-1)), newToken, totalHits)
			}
		}
		finally searcherLifetimeManager.release(searcher)
	}

	/**
	 * Get the searcher that has been recorded in the searcher lifetime manager under this
	 * name. If no token is passed, we acquire a new searcher and record it in the lifetime
	 * manager. We also acquire a new searcher if the requested one couldn't be found.
	 */
	private def acquireSearcher(token: Option[Long]): (Long, IndexSearcher) = {
		def newSearcher() = {
			val searcher = searcherManager.acquire()
			(searcherLifetimeManager.record(searcher), searcher)
		}

		def existingSearcher(t: Long) = {
			val searcher = searcherLifetimeManager.acquire(t)
			if (searcher == null) {
				newSearcher()
			} else {
				(t, searcher)
			}
		}

		token.fold(newSearcher())(existingSearcher)
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

	protected def docValuesStringField(name: String, value: String) = new SortedDocValuesField(name, new BytesRef(value))

	protected def tokenisedStringField(name: String, value: String, isStored: Boolean = true) = {
		val storage = if (isStored) Store.YES else Store.NO
		new TextField(name, value, storage)
	}

	protected def dateField(name: String, value: DateTime) = new LongField(name, value.getMillis, Store.YES)

	protected def docValuesField(name: String, value: Long) = new NumericDocValuesField(name, value)

	protected def doubleField(name: String, value: Double) = new DoubleField(name, value, Store.YES)

	protected def booleanField(name: String, value: Boolean) = new TextField(name, value.toString, Store.YES)
}

class NumericRangeQueryParser(numericFields: Seq[String], delegate: QueryParser) extends QueryParserBase {

	override def newRangeQuery(field: String, part1: String, part2: String, startInclusive: Boolean, endInclusive: Boolean): Query =
		if (numericFields.contains(field)) {
			val min: Option[java.lang.Long] = Option(part1).map(_.toLong)
			val max: Option[java.lang.Long] = Option(part2).map(_.toLong)

			NumericRangeQuery.newLongRange(field, min.orNull[java.lang.Long], max.orNull[java.lang.Long], startInclusive, endInclusive)
		} else {
			delegate.newRangeQuery(field, part1, part2, startInclusive, endInclusive)
		}



}