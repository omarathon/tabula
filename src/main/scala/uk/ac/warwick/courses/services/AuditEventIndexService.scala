package uk.ac.warwick.courses.services

import java.io.File
import java.io.FileNotFoundException
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Field._
import org.apache.lucene.document._
import org.apache.lucene.index._
import org.apache.lucene.index.FieldInfo.IndexOptions
import org.apache.lucene.search._
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation._
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.courses.data.model.AuditEvent
import uk.ac.warwick.courses.helpers.Closeables._
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.userlookup.User
import org.apache.lucene.search._
import collection.JavaConverters._
import org.springframework.stereotype.Component
import uk.ac.warwick.courses.helpers.Logging
import org.springframework.transaction.annotation.Transactional

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
			try { indexing = true; work } 
			finally indexing = false
	
	var reader:IndexReader =_
	var searcher:IndexSearcher =_
	val analyzer = new StandardAnalyzer(LuceneVersion)
	
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
	
	def initialiseSearching = 
		if (reader == null) {
			try {
				reader = IndexReader.open(FSDirectory.open(indexPath), true)
				searcher = new IndexSearcher(reader)
			} catch {
				case e:IndexNotFoundException => {
					logger.warn("No index found.")
					reader = null
					searcher = null
				}
			}
		}
	
	/**
	 * Incremental index. Can be run often.
	 */
	@Transactional
	def index = ifNotIndexing {
		val writerConfig = new IndexWriterConfig(LuceneVersion, analyzer)
		val startDate = latestIndexItem
		val newItems = service.listNewerThan(startDate, 1000).filter{_.eventStage == "before"}
		if (newItems.isEmpty) {
			logger.debug("No new items to index.")
		} else {
			closeThis(new IndexWriter(FSDirectory.open(indexPath), writerConfig)) { writer =>
				if (debugEnabled) logger.debug("Indexing items from " + startDate)
				for (item <- newItems) {
					updateMostRecent(item)
					writer.updateDocument(uniqueTerm(item), toDocument(item))
				}
				if (debugEnabled) logger.debug("Indexed " + newItems.size + " items")
			}
		}
	}
	
	def toId(doc:Document) = documentValue(doc, "id")
	
	/**
	 * If this item is the newest item this service has seen, save the date
	 * so we know where to start from next time.
	 */
	def updateMostRecent(item:AuditEvent) {
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
		if (searcher == null) { // usually if we've never indexed before, no index file
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
	
	def search(query:Query, max:Int, sort:Sort=null) : Seq[Document] = doSearch(query, Some(max), sort)
	def search(query:Query) : Seq[Document] = doSearch(query, None, null)
	def search(query:Query, sort:Sort) : Seq[Document] = doSearch(query, None, sort)
	private def doSearch(query:Query, max:Option[Int], sort:Sort) : Seq[Document] = {
		initialiseSearching
		val maxResults = max.getOrElse{searcher.maxDoc}
		val results = 
			if (sort == null) searcher.search(query, null, maxResults)
			else searcher.search(query,null,maxResults,sort)
		transformResults(results)
	}
	
	def transformResults(results:TopDocs) = {
		val hits = results.scoreDocs
		hits.par.map { hit => searcher.doc(hit.doc)	}.seq
	}
	
	/**
	 * If an existing Document is in the index with this term, it
	 * will be replaced.
	 */
	def uniqueTerm(item:AuditEvent) = new Term("id", Option(item.eventId).getOrElse(item.id.toString))
	
	/**
	 * TODO reuse one Document and set of Fields for all items
	 */
	def toDocument(item:AuditEvent) : Document = {
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
				case Some(value:String) => doc add plainStringField(key, value)
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