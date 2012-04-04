package uk.ac.warwick.courses.services

import java.io.File
import java.io.FileNotFoundException
import scala.collection.JavaConverters._
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Field._
import org.apache.lucene.document._
import org.apache.lucene.index.FieldInfo.IndexOptions
import org.apache.lucene.index._
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search._
import org.apache.lucene.search._
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import org.joda.time.DateTime
import org.joda.time.Duration
import org.springframework.beans.factory.annotation._
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.AuditEvent
import uk.ac.warwick.courses.helpers.Closeables._
import uk.ac.warwick.courses.helpers.Stopwatches._
import uk.ac.warwick.courses.helpers._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.core.StopWatch
import scala.collection.immutable.Stream

/**
 * Indexes audit events using Lucene.
 */
@Component 
class AuditEventIndexService extends InitializingBean with QueryMethods with Logging {
	final val LuceneVersion = Version.LUCENE_35
	
	@Autowired var service:AuditEventService =_ 
	@Value("${filesystem.index.audit.dir}") var indexPath:File =_
	@Value("${filesystem.create.missing}") var createMissingDirectories:Boolean =_
	@Value("${courses.audit.index.weeksbacklog}") var weeksBacklog:Int =_
	
	// Are we indexing now?
	var indexing:Boolean = false
	
	var lastIndexTime:Option[DateTime] = None
	var lastIndexDuration:Option[Duration] = None
	
	/**
	 * Wrapper around the indexing code so that it is only running once.
	 * If it's already running, the code is skipped.
	 * We only try indexing once a minute so there's no need to bother about
	 * tight race conditions here.
	 */
	def ifNotIndexing(work: =>Unit) = 
		if (indexing)
			logger.info("Skipped indexing because the indexer is already/still running.")
		else
			try { indexing = true; work} 
			finally indexing = false
	
//	var reader:IndexReader =_
//	var searcher:IndexSearcher =_
	/**
	 * SearcherManager handles returning a fresh IndexSearcher for each search,
	 * and managing old IndexSearcher instances that might still be in use. Use
	 * acquire() and release() to get IndexSearchers, and call maybeReopen() when
	 * the index has changed (i.e. after an index)
	 */
	var searcherManager: SearcherManager = _
	val analyzer = new StandardAnalyzer(LuceneVersion)
			
//	val executorService = Executors.newCachedThreadPool
	
	/**
	 * When an index run finishes, we note down the date of the newest audit item,
	 * so we know where to check from next time.
	 */
	var mostRecentIndexedItem: Option[DateTime] = None
	
	
	override def afterPropertiesSet {
		if (!indexPath.exists) {
			if (createMissingDirectories) indexPath.mkdirs 
			else throw new IllegalStateException("Audit event index path missing", new FileNotFoundException(indexPath.getAbsolutePath))
		}
		if (!indexPath.isDirectory) throw new IllegalStateException("Audit event index path not a directory: " + indexPath.getAbsolutePath)
		
		initialiseSearching
	}
	
	private def initialiseSearching = {
		if (searcherManager == null) {
			try {
				searcherManager = new SearcherManager(FSDirectory.open(indexPath), null, null)
			} catch {
				case e:IndexNotFoundException => logger.warn("No index found.")
			}
		}
	} 
	
	def listRecent(start:Int, count:Int) : Seq[AuditEvent] = {
		val min = new DateTime().minusYears(2)
		val docs = search(
			query = NumericRangeQuery.newLongRange("eventDate", min.getMillis, null, true, true),
			sort = new Sort(new SortField("eventDate", SortField.LONG, true)),
			offset = start,
			max = count
		)
		docs flatMap toId flatMap service.getById
	}
	
	/**
	 * Sets up a new IndexSearcher with a reopened IndexReader so that
	 * subsequent searches see the results of recent index changes.
	 */
	private def reopen = {
		initialiseSearching
		searcherManager.maybeReopen
	}
	
	/**
	 * Incremental index. Can be run often.
	 */
	@Transactional
	def index() = ifNotIndexing {
		val stopWatch = new StopWatch()
		stopWatch.record("Incremental index") {
			val startDate = latestIndexItem
			val newItems = service.listNewerThan(startDate, 1000).filter{_.eventStage == "before"}
			if (newItems.isEmpty) {
				logger.debug("No new items to index.")
			} else {
				if (debugEnabled) logger.debug("Indexing items from " + startDate)
				doIndexEvents(newItems)
			}
		}
		lastIndexDuration = Some(new Duration(stopWatch.getTotalTimeMillis))
		lastIndexTime = Some(new DateTime())
	}
	
	/**
	 * Indexes a specific given list of events.
	 */
	@Transactional
	def indexEvents(events:Seq[AuditEvent]) = ifNotIndexing { doIndexEvents(events) }
	
	private def doIndexEvents(events:Seq[AuditEvent]) {
		val writerConfig = new IndexWriterConfig(LuceneVersion, analyzer)
		closeThis(new IndexWriter(FSDirectory.open(indexPath), writerConfig)) { writer =>
			for (item <- events) {
				updateMostRecent(item)
				writer.updateDocument(uniqueTerm(item), toDocument(item))
			}
			if (debugEnabled) logger.debug("Indexed " + events.size + " items")
		}
		reopen
	}
	
	private def toId(doc:Document) = documentValue(doc, "id").map{_.toLong}
	
	/**
	 * If this item is the newest item this service has seen, save the date
	 * so we know where to start from next time.
	 */
	private def updateMostRecent(item:AuditEvent) {
		val shouldUpdate = mostRecentIndexedItem.map{ _ isBefore item.eventDate }.getOrElse{ true }
		if (shouldUpdate)
			mostRecentIndexedItem = Some(item.eventDate)
	}
	
	/**
	 * Either get the date of the most recent item we've process in this JVM
	 */
	def latestIndexItem:DateTime = {
		mostRecentIndexedItem.map{ _.minusMinutes(5) }.getOrElse {
			// extract possible list of eventDate values from possible newest item and get possible first value as a Long.
			documentValue(newest(), "eventDate")
				.map { v => new DateTime(v.toLong) }
				.getOrElse { new DateTime().minusYears(1) }
				// TODO change to just a few weeks after first deploy of this - 
				// this is just to get all historical data indexed, after which we won't ever
				// be so out of date.
		}
	}
	
	/**
	 * Try to get the first value out of a document field.
	 */
	def documentValue(doc:Option[Document], key:String): Option[String] = doc.flatMap { _.getValues(key).headOption }
	def documentValue(doc:Document, key:String): Option[String] = doc.getValues(key).headOption
	
	
	/**
	 * Find the newest audit event item that was indexed, by searching by
	 * eventDate and sorting in descending date order.
	 * 
	 * @param since Optional lower bound for date - recommended if possible, as it is faster.
	 */
	def newest(since:DateTime=null) : Option[Document] = {
		initialiseSearching
		
		if (searcherManager == null) { // usually if we've never indexed before, no index file
			None
		} else {
			val min:Option[JLong] = Option(since).map{_.getMillis}
			val docs = search(
				query = NumericRangeQuery.newLongRange("eventDate", min.orNull, null, true, true),
				sort = new Sort(new SortField("eventDate", SortField.LONG, true)),
				max = 1
			)
			docs.headOption // Some(firstResult) or None if empty
		}
	}
	
	def student(student:User) = search(
		new TermQuery(new Term("students", student.getWarwickId))
	)
	
	def findByUserId(usercode:String) = 
		search(new TermQuery(new Term("userId", usercode)))
		
	def whoDownloadedFeedback(assignment:Assignment) =
		search(all(
				new TermQuery(new Term("eventType", "DownloadFeedback")),
				new TermQuery(new Term("assignment", assignment.id))
			))
			.flatMap{ toId(_) }
			.flatMap{ service.getById(_) }
			.map{ _.masqueradeUserId }
			.filterNot{ _ == null }
			.distinct
			
	
	def search(query:Query, max:Int, sort:Sort=null, offset:Int=0) : Seq[Document] = doSearch(query, Some(max), sort, offset)
	def search(query:Query) : Seq[Document] = doSearch(query, None, null, 0)
	def search(query:Query, sort:Sort) : Seq[Document] = doSearch(query, None, sort, 0)
	
	private def doSearch(query:Query, max:Option[Int], sort:Sort, offset:Int) : Seq[Document] = {
		initialiseSearching
		if (searcherManager == null) return Seq.empty
		acquireSearcher { searcher =>
			val maxResults = max.getOrElse(searcher.maxDoc)
			val results = 
				if (sort == null) searcher.search(query, null, searcher.maxDoc)
				else searcher.search(query,null,searcher.maxDoc,sort)
			transformResults(searcher, results, offset, maxResults)
		}
	}
			
	private def acquireSearcher[T](work: IndexSearcher=>T): T = {
		val searcher = searcherManager.acquire
		try work(searcher)
		finally searcherManager.release(searcher)
	}
	
	private def transformResults(searcher:IndexSearcher, results:TopDocs, offset:Int, max:Int) = {
		val hits = results.scoreDocs
		hits.toStream.drop(offset).take(max).map { hit => searcher.doc(hit.doc) }.toList
	}
	
	/**
	 * If an existing Document is in the index with this term, it
	 * will be replaced.
	 */
	private def uniqueTerm(item:AuditEvent) = new Term("id", item.id.toString)
	
	/**
	 * TODO reuse one Document and set of Fields for all items
	 */
	private def toDocument(item:AuditEvent) : Document = {
		val doc = new Document
		doc add plainStringField("id", item.id.toString)
		if (item.eventId != null) { // null for old events
			doc add plainStringField("eventId", item.eventId)
		}
		if (item.userId != null) // system-run actions have no user
			doc add plainStringField("userId", item.userId) 
		if (item.masqueradeUserId != null) 
			doc add plainStringField("masqueradeUserId", item.masqueradeUserId)
		doc add plainStringField("eventType", item.eventType)
		
		service.parseData(item.data) match {
			case None => // no valid JSON
			case Some(data) => addDataToDoc(data, doc)
		}
		doc add dateField("eventDate", item.eventDate)
		doc
	}
	
	private def addDataToDoc(data:Map[String,Any], doc:Document) = {
		for (key <- Seq("feedback", "assignment", "module", "department")) {
			data.get(key) match {
				case Some(value:String) => doc add plainStringField(key, value, stored=false)
				case _ => // missing or not a string
			}
		}
		data.get("students").collect {
			case studentIds:Array[String] => 
				val field = new Field("students", studentIds.mkString(" "), Store.NO, Index.ANALYZED_NO_NORMS)
				field.setIndexOptions(IndexOptions.DOCS_ONLY)
				doc add field
			case _ =>
		}
	}
	
	private def plainStringField(name:String, value:String, stored:Boolean=true) = {
		val storeMode = if (stored) Store.YES else Store.NO
		val field = new Field(name, value, storeMode, Index.NOT_ANALYZED_NO_NORMS)
		field.setIndexOptions(IndexOptions.DOCS_ONLY)
		field
	}
	
	private def dateField(name:String, value:DateTime) = {
		val field = new NumericField(name)
		field.setLongValue(value.getMillis)
		field
	}
}

trait QueryMethods {
	private def boolean(occur:Occur, queries:Query*) = {
		val query = new BooleanQuery
		for (q <- queries) query.add(q, occur)
		query
	}
	
	def all(queries:Query*) = boolean(Occur.MUST, queries:_*)
	def some(queries:Query*) = boolean(Occur.SHOULD, queries:_*)
}