package uk.ac.warwick.courses.services

import java.io.File
import java.io.FileNotFoundException
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.immutable.Stream
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.KeywordAnalyzer
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.WhitespaceAnalyzer
import org.apache.lucene.document.Field._
import org.apache.lucene.document._
import org.apache.lucene.index.FieldInfo.IndexOptions
import org.apache.lucene.index._
import org.apache.lucene.queryParser.QueryParser
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
import org.springframework.context.Lifecycle
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.AuditEvent
import uk.ac.warwick.courses.helpers.Closeables._
import uk.ac.warwick.courses.helpers.Stopwatches._
import uk.ac.warwick.courses.helpers._
import uk.ac.warwick.courses.helpers.DateTimeOrdering._
import uk.ac.warwick.userlookup.User
import scala.actors.threadpool.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.DisposableBean
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.model.Submission

/**
 * Methods for querying stuff out of the index. Separated out from
 * the main index service into this trait so they're easier to find.
 * Possibly the indexer and the index querier should be separate classes
 * altogether.
 */
trait QueryMethods { self:AuditEventIndexService =>
	
	/**
	 * Get recent AuditEvent items.
	 */
	def listRecent(start:Int, count:Int) : Seq[AuditEvent] = {
		val min = new DateTime().minusYears(2)
		val docs = search(
			query = NumericRangeQuery.newLongRange("eventDate", min.getMillis, null, true, true),
			sort = reverseDateSort,
			offset = start,
			max = count
		)
		docs flatMap { toAuditEvent(_) }
	}
	
	def student(user:User) = search(termQuery("students", user.getWarwickId))
	
	def findByUserId(usercode:String) = search(termQuery("userId", usercode))
		
	/**
	 * Work out which submissions have been downloaded from the admin interface
	 * based on the audit events.
	 */
	def adminDownloadedSubmissions(assignment:Assignment) : Seq[Submission] = {
		val assignmentTerm = termQuery("assignment", assignment.id)
		
		// find events where you downloaded all available submissions
		val allDownloaded = parsedAuditEvents(search(
				all(assignmentTerm, termQuery("eventType", "DownloadAllSubmissions"))
			))
		// take most recent event and find submissions made before then.
		val submissions1 : Seq[Submission] = 
			if (allDownloaded.isEmpty) { Nil } 
			else {
				val latestDate = allDownloaded.map{ _.eventDate }.max
				assignment.submissions.filter{ _.submittedDate isBefore latestDate }
			}
		
		// find events where individual submissions were downloaded
		val someDownloaded = parsedAuditEvents(search(
				all(assignmentTerm, termQuery("eventType", "DownloadSubmissions"))
			))
		val submissions2 = someDownloaded.flatMap( _.submissionIds ).flatMap( id=> assignment.submissions.find(_.id == id) )
		
		(submissions1 ++ submissions2).distinct
	}
		
	def whoDownloadedFeedback(assignment:Assignment) =
		search(all(
				termQuery("eventType", "DownloadFeedback"),
				termQuery("assignment", assignment.id)
			))
			.flatMap( toAuditEvent(_) )
			.filterNot( _.hadError )
			.map{ _.masqueradeUserId }
			.filterNot{ _ == null }
			.distinct
			
	/**
	 * Return the most recently created assignment for this Module.
	 */
	def recentAssignment(module:Module) : Option[Assignment] = {
		search(query = all(
				termQuery("eventType", "AddAssignment"),
				termQuery("module", module.id)),
				max=1,
				sort=reverseDateSort)
			.flatMap( toParsedAuditEvent )
			.flatMap( _.assignmentId )
			.flatMap( assignmentService.getAssignmentById )
			.headOption
	}
	
}

/**
 * Indexes audit events using Lucene, and does searches on it.
 * 
 * TODO Split out indexing from searching, it's a big mess.
 *      Maybe put searches in AuditEventService.
 */
@Component 
class AuditEventIndexService extends InitializingBean with QueryHelpers with QueryMethods with Logging with DisposableBean {
	
	final val LuceneVersion = Version.LUCENE_35
	
	// largest batch of event items we'll load in at once.
	final val MaxBatchSize = 100000
	
	// largest batch of event items we'll load in at once during scheduled incremental index.
	final val IncrementalBatchSize = 1000
	
	// Fields containing IDs and things that should be passed
	// around as-is.
	// NOTE: analyzer was switched to do token analysis by default,
	//    so this particular list is not used.
	val tokenFields = Set(
			"eventType",
			"department",
			"module",
			"assignment",
			"submission",
			"feedback",
			"studentId"
		)
	
	// Fields that are a space-separated list of tokens.
	// A list of IDs needs to be in here or else the whole thing
	// will be treated as one value.
	val tokenListFields = Set(
			"students",
			"feedbacks",
			"submissions"
		)
	
	@Autowired var maintenanceService:MaintenanceModeService = _
	@Autowired var service:AuditEventService =_ 
	@Autowired var assignmentService:AssignmentService =_ 
	@Value("${filesystem.index.audit.dir}") var indexPath:File =_
	@Value("${filesystem.create.missing}") var createMissingDirectories:Boolean =_
	@Value("${courses.audit.index.weeksbacklog}") var weeksBacklog:Int =_
	
	// Are we indexing now?
	var indexing:Boolean = false
	
	var lastIndexTime:Option[DateTime] = None
	var lastIndexDuration:Option[Duration] = None
	
	// HFC-189 Reopen index every 2 minutes, even if not the indexing instance.
	val executor:ScheduledExecutorService = Executors.newScheduledThreadPool(1)
	
	/**
	 * Wrapper around the indexing code so that it is only running once.
	 * If it's already running, the code is skipped.
	 * We only try indexing once a minute so thmiere's no need to bother about
	 * tight race conditions here.
	 */
	def ifNotIndexing(work: =>Unit) = 
		if (indexing)
			logger.info("Skipped indexing because the indexer is already/still running.")
		else if (maintenanceService.enabled)
			logger.info("Skipped indexing because maintenance mode is enabled.")
		else
			try { indexing = true; work} 
			finally indexing = false

	/**
	 * SearcherManager handles returning a fresh IndexSearcher for each search,
	 * and managing old IndexSearcher instances that might still be in use. Use
	 * acquire() and release() to get IndexSearchers, and call maybeReopen() when
	 * the index has changed (i.e. after an index)
	 */
	var searcherManager: SearcherManager = _
	val analyzer = {
		//val standard = new StandardAnalyzer(LuceneVersion)
		val token = new KeywordAnalyzer()
		val whitespace:Analyzer = new WhitespaceAnalyzer(LuceneVersion)

		val tokenListMappings = tokenListFields.map(field=> (field -> whitespace))
		//val tokenMappings = tokenFields.map(field=> (field -> token))
		val mappings = (tokenListMappings/* ++ tokenMappings*/).toMap.asJava
		
		new PerFieldAnalyzerWrapper(token, mappings)
	}
	val parser = new QueryParser(LuceneVersion, "", analyzer)
	
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
		
		// Reopen the index reader periodically, else it won't notice changes.
		executor.scheduleAtFixedRate( Runnable{ 
			try reopen catch { case e => logger.error("Index service reopen failed", e) } 
		}, 20, 20, TimeUnit.SECONDS)
	}
	
	override def destroy {
		executor.shutdown()
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
	 * Has a limit to how many items it will load at once, but subsequent indexes
	 * will get through those. There would have to be hundreds of audit events
	 * per minute in order for the index to lag behind, and even then it would catch
	 * up as soon as it reached a quiet time.
	 */
	@Transactional
	def index() = ifNotIndexing {
		val stopWatch = StopWatch()
		stopWatch.record("Incremental index") {
			val startDate = latestIndexItem
			val newItems = service.listNewerThan(startDate, IncrementalBatchSize).filter{_.eventStage == "before"}
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
	
	@Transactional
	def indexFrom(startDate:DateTime) = ifNotIndexing {
		val newItems = service.listNewerThan(startDate, MaxBatchSize).filter{_.eventStage == "before"}
		doIndexEvents(newItems)
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
		reopen // not really necessary as we reopen periodically anyway
	}
	
	protected def toId(doc:Document) = documentValue(doc, "id").map{_.toLong}
	
	protected def toAuditEvent(id:Long) : Option[AuditEvent] = service.getById(id)
	protected def toAuditEvent(doc:Document) : Option[AuditEvent] = { toId(doc) flatMap (toAuditEvent) }
	
	protected def toParsedAuditEvent(doc:Document) : Option[AuditEvent] = toAuditEvent(doc).map { event =>
		event.parsedData = service.parseData(event.data)
		event.related.map { e =>
			e.parsedData = service.parseData(e.data)
		}
		event
	}
	
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
	 * or look up the most recent item in the index, or else index everything
	 * from the past year.
	 */
	def latestIndexItem:DateTime = {
		mostRecentIndexedItem.map{ _.minusMinutes(5) }.getOrElse {
			// extract possible list of eventDate values from possible newest item and get possible first value as a Long.
			documentValue(newest(), "eventDate")
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
	
	def search(query:Query, max:Int, sort:Sort=null, offset:Int=0) : Seq[Document] = doSearch(query, Some(max), sort, offset)
	def search(query:Query) : Seq[Document] = doSearch(query, None, null, 0)
	def search(query:Query, sort:Sort) : Seq[Document] = doSearch(query, None, sort, 0)
	
	protected def auditEvents(docs:Seq[Document]) = docs.flatMap(toAuditEvent(_))
	protected def parsedAuditEvents(docs:Seq[Document]) = docs.flatMap(toParsedAuditEvent(_))
	
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
	protected def toDocument(item:AuditEvent) : Document = {
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
	
	def openQuery(queryString:String, start:Int, count:Int) = {
		val query = parser.parse(queryString)
		val docs = search(query,
			sort = new Sort(new SortField("eventDate", SortField.LONG, true)),
			offset = start,
			max = count
		)
		docs flatMap toId flatMap service.getById
	}
	
	private def addDataToDoc(data:Map[String,Any], doc:Document) = {
		for (key <- Seq("submission", "feedback", "assignment", "module", "department","studentId")) {
			data.get(key) match {
				case Some(value:String) => doc add plainStringField(key, value, stored=false)
				case _ => // missing or not a string
			}
		}
		for (key <- Seq("students")) {
			data.get(key).collect {
				case ids:JList[_] => doc add seqField(key, ids.asScala)
				case ids:Seq[_] => doc add seqField(key, ids)
				case ids:Array[String] => doc add seqField(key, ids)
				case other:Any => logger.warn("Collection field "+key+" was unexpected type: " + other.getClass.getName)
				case _ =>
			}
		}
	}
	
	private def seqField(key:String, ids:Seq[_]) = {
		val field = new Field(key, ids.mkString(" "), Store.NO, Index.ANALYZED_NO_NORMS)
		field.setIndexOptions(IndexOptions.DOCS_ONLY)
		field
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



trait QueryHelpers {
	private def boolean(occur:Occur, queries:Query*) = {
		val query = new BooleanQuery
		for (q <- queries) query.add(q, occur)
		query
	}
	
	def all(queries:Query*) = boolean(Occur.MUST, queries:_*)
	def some(queries:Query*) = boolean(Occur.SHOULD, queries:_*)
	
	def termQuery(name:String, value:String) = 
		new TermQuery(new Term(name, value))

	def dateSort = new Sort(new SortField("eventDate", SortField.LONG, false))
	def reverseDateSort = new Sort(new SortField("eventDate", SortField.LONG, true))
}