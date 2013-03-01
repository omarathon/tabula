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
import org.apache.lucene.search.SearcherLifetimeManager.PruneByAge
import uk.ac.warwick.spring.Wire

case class PagedAuditEvents(val docs: Seq[AuditEvent], private val lastscore: Option[ScoreDoc], val token: Long, val total: Int) {
	// need this pattern matcher as brain-dead IndexSearcher.searchAfter returns an object containing ScoreDocs,
	// and expects a ScoreDoc in its method signature, yet in its implementation throws an exception unless you
	// pass a specific subclass of FieldDoc.
	def last: Option[FieldDoc] = lastscore match {
		case None => None
		case Some(f:FieldDoc) => Some(f)
		case _ => throw new ClassCastException("Lucene did not return an Option[FieldDoc] as expected")
	}
}

/**
 * Methods for querying stuff out of the index. Separated out from
 * the main index service into this trait so they're easier to find.
 * Possibly the indexer and the index querier should be separate classes
 * altogether.
 */
trait AuditEventQueryMethods { self: AuditEventIndexService =>

	def student(user: User) = search(termQuery("students", user.getWarwickId))

	def findByUserId(usercode: String) = search(termQuery("userId", usercode))
	
	def findPublishFeedbackEvents(dept: Department) = {
		val searchResults = search(all(
			termQuery("eventType", "PublishFeedback"),
			termQuery("department", dept.code)))
			.flatMap{ toParsedAuditEvent(_) }
			.filterNot { _.hadError }
		
		searchResults
	}
		
	
	def submissionsForModules(modules: Seq[Module], last: Option[ScoreDoc], token: Option[Long], max: Int = 50): PagedAuditEvents = {
		val moduleTerms = for (module <- modules) yield termQuery("module", module.id)
		
		val searchResults = search(
			query = all(termQuery("eventType", "SubmitAssignment"),
						some(moduleTerms:_*)
					),
			max = max,
			sort = reverseDateSort,
			last = last,
			token = token)
			
		new PagedAuditEvents(parsedAuditEvents(searchResults.results), searchResults.last, searchResults.token, searchResults.total)
	}

	def noteworthySubmissionsForModules(modules: Seq[Module], last: Option[ScoreDoc], token: Option[Long], max: Int = 50): PagedAuditEvents = {
		val moduleTerms = for (module <- modules) yield termQuery("module", module.id)
		
		val searchResults = search(
			query = all(termQuery("eventType", "SubmitAssignment"),
						termQuery("submissionIsNoteworthy", "true"),
						some(moduleTerms:_*)
					),
			max = max,
			sort = reverseDateSort,
			last = last,
			token = token)
		
		new PagedAuditEvents(parsedAuditEvents(searchResults.results), searchResults.last, searchResults.token, searchResults.total)
	}

	/**
	 * Work out which submissions have been downloaded from the admin interface
	 * based on the audit events.
	 */
	def adminDownloadedSubmissions(assignment: Assignment): Seq[Submission] = {
		val assignmentTerm = termQuery("assignment", assignment.id)

		// find events where you downloaded all available submissions
		val allDownloaded = parsedAuditEvents(search(
			all(assignmentTerm, termQuery("eventType", "DownloadAllSubmissions"))))
		// take most recent event and find submissions made before then.
		val submissions1: Seq[Submission] =
			if (allDownloaded.isEmpty) { Nil }
			else {
				val latestDate = allDownloaded.map { _.eventDate }.max
				assignment.submissions.filter { _.submittedDate isBefore latestDate }
			}

		// find events where individual submissions were downloaded
		val someDownloaded = parsedAuditEvents(search(
			all(assignmentTerm, termQuery("eventType", "DownloadSubmissions"))))
		val submissions2 = someDownloaded.flatMap(_.submissionIds).flatMap(id => assignment.submissions.find(_.id == id))

		(submissions1 ++ submissions2).distinct
	}

	def whoDownloadedFeedback(assignment: Assignment) =
		search(all(
			termQuery("eventType", "DownloadFeedback"),
			termQuery("assignment", assignment.id)))
			.flatMap { toItem(_) }
			.filterNot { _.hadError }
			.map { _.masqueradeUserId }
			.filterNot { _ == null }
			.distinct

	def mapToAssignments(seq: Seq[Document]) = seq
		.flatMap(toParsedAuditEvent)
		.flatMap(_.assignmentId)
		.flatMap(assignmentService.getAssignmentById)

	/**
	 * Return the most recently created assignment for this moodule
	 */
	def recentAssignment(module: Module): Option[Assignment] = {
		mapToAssignments(search(query = all(
			termQuery("eventType", "AddAssignment"),
			termQuery("module", module.id)),
			max = 1,
			sort = reverseDateSort)).headOption
	}

	def recentAssignment(department: Department): Option[Assignment] = {
		mapToAssignments(search(query = all(
			termQuery("eventType", "AddAssignment"),
			termQuery("department", department.code)),
			max = 1,
			sort = reverseDateSort)).headOption
	}

	def getAssignmentCreatedDate(assignment: Assignment): Option[DateTime] = {
		search(all(term("eventType" -> "AddAssignment"), term("assignment" -> assignment.id)))
			.flatMap { doc: Document =>
				doc.getField("eventDate") match {
					case field: StoredField if field.numericValue() != null => {
						Some(new DateTime(field.numericValue()))
					}
					case _ => {
						None
					}
				}
			}
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
class AuditEventIndexService extends AbstractIndexService[AuditEvent] with AuditEventQueryMethods {
	// largest batch of event items we'll load in at once.
	final override val MaxBatchSize = 100000

	// largest batch of event items we'll load in at once during scheduled incremental index.
	final override val IncrementalBatchSize = 1000

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
		"studentId")

	// Fields that are a space-separated list of tokens.
	// A list of IDs needs to be in here or else the whole thing
	// will be treated as one value.
	val tokenListFields = Set(
		"students",
		"feedbacks",
		"submissions")

	@Autowired var service: AuditEventService = _
	@Autowired var assignmentService: AssignmentService = _
	@Value("${filesystem.index.audit.dir}") override var indexPath: File = _
	@Value("${audit.index.weeksbacklog}") var weeksBacklog: Int = _

	override val analyzer = {
		//val standard = new StandardAnalyzer(LuceneVersion)
		val token = new KeywordAnalyzer()
		val whitespace: Analyzer = new WhitespaceAnalyzer(LuceneVersion)

		val tokenListMappings = tokenListFields.map(field => (field -> whitespace))
		//val tokenMappings = tokenFields.map(field=> (field -> token))
		val mappings = (tokenListMappings /* ++ tokenMappings*/ ).toMap.asJava

		new PerFieldAnalyzerWrapper(token, mappings)
	}
	
	override val IdField = "id"
	override def getId(item: AuditEvent) = item.id.toString
	
	override val UpdatedDateField = "eventDate"
	override def getUpdatedDate(item: AuditEvent) = item.eventDate
	
	override def listNewerThan(startDate: DateTime, batchSize: Int) =
		service.listNewerThan(startDate, batchSize).filter { _.eventStage == "before" }

	protected def toItem(id: String) = service.getById(id.toLong)

	protected def toParsedAuditEvent(doc: Document): Option[AuditEvent] = toItem(doc).map { event =>
		event.parsedData = service.parseData(event.data)
		event.related.map { e =>
			e.parsedData = service.parseData(e.data)
		}
		event
	}

	protected def auditEvents(docs: Seq[Document]) = docs.flatMap(toItem(_))
	protected def parsedAuditEvents(docs: Seq[Document]) = docs.flatMap(toParsedAuditEvent(_))

	/**
	 * TODO reuse one Document and set of Fields for all items
	 */
	protected def toDocument(item: AuditEvent): Document = {
		val doc = new Document

		if (item.related == null || item.related.isEmpty) {
			service.addRelated(item)
		}

		doc add plainStringField(IdField, item.id.toString)
		if (item.eventId != null) { // null for old events
			doc add plainStringField("eventId", item.eventId)
		}
		if (item.userId != null) // system-run actions have no user
			doc add plainStringField("userId", item.userId)
		if (item.masqueradeUserId != null)
			doc add plainStringField("masqueradeUserId", item.masqueradeUserId)
		doc add plainStringField("eventType", item.eventType)

		// add data from all stages of the event, before and after.
		for (i <- item.related) {
			service.parseData(i.data) match {
				case None => // no valid JSON
				case Some(data) => addDataToDoc(data, doc)
			}
		}

		doc add dateField(UpdatedDateField, item.eventDate)
		doc
	}

	def openQuery(queryString: String, start: Int, count: Int) = {
		logger.info("Opening query: " + queryString)
		val query = parser.parse(queryString)
		val docs = search(query,
			sort = new Sort(new SortField(UpdatedDateField, SortField.Type.LONG, true)),
			offset = start,
			max = count)
		docs flatMap toId map (_.toLong) flatMap service.getById
	}

	// pick items out of the auditevent JSON and add them as document fields.
	private def addDataToDoc(data: Map[String, Any], doc: Document) = {
		for (key <- Seq("submission", "feedback", "assignment", "module", "department", "studentId", "submissionIsNoteworthy")) {
			data.get(key) match {
				case Some(value: String) => {
					doc add plainStringField(key, value, isStored = false)
				}
				case Some(value: Boolean) => {
					doc add plainStringField(key, value.toString, isStored = false)
				}
				case _ => // missing or not a string
			}
		}
		// sequence-type fields
		for (key <- Seq("students")) {
			data.get(key).collect {
				case ids: JList[_] => doc add seqField(key, ids.asScala)
				case ids: Seq[_] => doc add seqField(key, ids)
				case ids: Array[String] => doc add seqField(key, ids)
				case other: AnyRef => logger.warn("Collection field " + key + " was unexpected type: " + other.getClass.getName)
				case _ =>
			}
		}
	}
}