package uk.ac.warwick.tabula.services

import java.io.File
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.apache.lucene.analysis._
import org.apache.lucene.document._
import org.apache.lucene.search._
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation._
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User
import org.apache.lucene.analysis.core._
import org.apache.lucene.analysis.miscellaneous._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import AuditEventIndexService._
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.SortingMergePolicy

object AuditEventIndexService {
	type PagedAuditEvents = PagingSearchResultItems[AuditEvent]
	val PagedAuditEvents = PagingSearchResultItems[AuditEvent] _
}

trait AuditEventNoteworthySubmissionsService {
	final val DefaultMaxEvents = 50
	
	def submissionsForModules(modules: Seq[Module], last: Option[ScoreDoc], token: Option[Long], max: Int = DefaultMaxEvents): PagedAuditEvents
	def noteworthySubmissionsForModules(modules: Seq[Module], last: Option[ScoreDoc], token: Option[Long], max: Int = DefaultMaxEvents): PagedAuditEvents
}

/**
 * Methods for querying stuff out of the index. Separated out from
 * the main index service into this trait so they're easier to find.
 * Possibly the indexer and the index querier should be separate classes
 * altogether.
 */
trait AuditEventQueryMethods extends AuditEventNoteworthySubmissionsService { self: AuditEventIndexService =>

	def student(user: User) = search(termQuery("students", user.getWarwickId))

	def findByUserId(usercode: String) = search(termQuery("userId", usercode))
	
	def findPublishFeedbackEvents(dept: Department) = {
		val searchResults = search(all(
			termQuery("eventType", "PublishFeedback"),
			termQuery("department", dept.code)))
			.transformAll(toParsedAuditEvents)
			.filterNot { _.hadError }

		searchResults
	}

	private def assignmentRangeRestriction(assignment: Assignment, referenceDate: Option[DateTime]) = referenceDate match {
		case Some(createdDate) => all(
			termQuery("assignment", assignment.id),
			dateRange(createdDate, DateTime.now)
		)
		case _ => termQuery("assignment", assignment.id)
	}


	
	def submissionsForModules(modules: Seq[Module], last: Option[ScoreDoc], token: Option[Long], max: Int = DefaultMaxEvents): PagedAuditEvents = {
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

	def submissionsForAssignment(assignment: Assignment): Seq[AuditEvent] = search(
		query = all(
			termQuery("eventType", "SubmitAssignment"),
			assignmentRangeRestriction(assignment, assignment.submissions.asScala.map { _.submittedDate }.sorted.headOption.orElse { Option(assignment.createdDate )})
		)
	).transformAll(toParsedAuditEvents)


	def publishFeedbackForStudent(assignment: Assignment, student: User): Seq[AuditEvent] = 
		publishFeedbackForStudent(assignment, student.getWarwickId)
	
	def publishFeedbackForStudent(assignment: Assignment, warwickId: String): Seq[AuditEvent] = search(
		query = all(
			termQuery("eventType", "PublishFeedback"),
			termQuery("students", warwickId),
			assignmentRangeRestriction(assignment, assignment.feedbacks.asScala.flatMap { f => Option(f.releasedDate) }.sorted.headOption.orElse { Option(assignment.createdDate )})
		)
	).transformAll(toParsedAuditEvents)
	.sortBy(_.eventDate).reverse
	
	def submissionForStudent(assignment: Assignment, student: User): Seq[AuditEvent] = search(
		query = all(
			termQuery("eventType", "SubmitAssignment"),
			termQuery("masqueradeUserId", student.getUserId),
			assignmentRangeRestriction(assignment, assignment.submissions.asScala.map { _.submittedDate }.sorted.headOption.orElse { Option(assignment.createdDate )})
		)
	).transformAll(toParsedAuditEvents)
	.sortBy(_.eventDate).reverse
	
	

	def noteworthySubmissionsForModules(modules: Seq[Module], last: Option[ScoreDoc], token: Option[Long], max: Int = DefaultMaxEvents): PagedAuditEvents = {
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
		val assignmentTerm = assignmentRangeRestriction(assignment, assignment.submissions.asScala.map { _.submittedDate }.sorted.headOption.orElse { Option(assignment.createdDate )})

		// find events where you downloaded all available submissions
		val allDownloaded = parsedAuditEvents(
			search(
				query = all(
					assignmentTerm,
					termQuery("eventType", "DownloadAllSubmissions")
				)
			)
		).sortBy(_.eventDate).reverse
		
		// take most recent event and find submissions made before then.
		val submissions1: Seq[Submission] = allDownloaded.headOption match {
			case None => Nil
			case Some(event) =>
				val latestDate = event.eventDate
				assignment.submissions.filter { _.submittedDate isBefore latestDate }
		}

		// find events where selected submissions were downloaded
		val someDownloaded = parsedAuditEvents(search(
			all(assignmentTerm, termQuery("eventType", "DownloadSubmissions"))))
		val submissions2 = someDownloaded.flatMap(_.submissionIds).flatMap(id => assignment.submissions.find(_.id == id))

		// find events where individual submissions were downloaded
		val individualDownloads = parsedAuditEvents(
				search(all(assignmentTerm, termQuery("eventType", "AdminGetSingleSubmission"))))
		val submissions3 = individualDownloads.flatMap(_.submissionId).flatMap(id => assignment.submissions.find(_.id == id))
		(submissions1 ++ submissions2 ++ submissions3).distinct
	}

	def feedbackDownloads(assignment: Assignment) = {
		search(all(
			termQuery("eventType", "DownloadFeedback"),
			assignmentRangeRestriction(assignment, assignment.feedbacks.asScala.flatMap { f => Option(f.releasedDate) }.sorted.headOption.orElse { Option(assignment.createdDate )})))
			.transformAll(toItems)
			.filterNot { _.hadError }
			.map( whoDownloaded => {
				(whoDownloaded.masqueradeUserId, whoDownloaded.eventDate)
			})
	}
	
	def latestAssignmentEvent(assignment: Assignment, eventName: String) = {
		search(all(
			termQuery("eventType", "ViewOnlineFeedback"),
			assignmentRangeRestriction(assignment, assignment.feedbacks.asScala.flatMap { f => Option(f.releasedDate) }.sorted.headOption.orElse { Option(assignment.createdDate )})))
			.transformAll(toItems)
			.filterNot { _.hadError }
			.map { user => (user.masqueradeUserId, user.eventDate) }
			.groupBy { case (userId, _) => userId }
			.map { case (_, events) => events.maxBy { case (_, eventDate) => eventDate } }
			.toSeq
	}

	def latestOnlineFeedbackViews(assignment: Assignment) = latestAssignmentEvent(assignment, "ViewOnlineFeedback")
	def latestDownloadFeedbackAsPdf(assignment: Assignment) = latestAssignmentEvent(assignment, "DownloadFeedbackAsPdf")

	def latestGenericFeedbackAdded(assignment: Assignment): Option[DateTime] = {
		val allEvents =
			search( all(
				termQuery("eventType", "GenericFeedback"),
				assignmentRangeRestriction(assignment, Option(assignment.createdDate)))
			).transformAll(toItems)

		if (allEvents.isEmpty) None
		else Some(allEvents.maxBy(_.eventDate).eventDate)
	}

	def latestOnlineFeedbackAdded(assignment: Assignment) =
		search(all(
			termQuery("eventType", "OnlineFeedback"),
			assignmentRangeRestriction(assignment, Option(assignment.createdDate))))
			.transformAll(toParsedAuditEvents)
			.filterNot { _.hadError }
			.flatMap(auditEvent => auditEvent.students.map( student => (student, auditEvent.eventDate)))
			.groupBy( _._1)
			.map(x => x._2.maxBy(_._2))
			.toSeq
			
	def whoDownloadedFeedback(assignment: Assignment) =
		search(all(
			termQuery("eventType", "DownloadFeedback"),
			assignmentRangeRestriction(assignment, assignment.feedbacks.asScala.flatMap { f => Option(f.releasedDate) }.sorted.headOption.orElse { Option(assignment.createdDate )})))
			.transformAll(toItems)
			.filterNot { _.hadError }
			.map { _.masqueradeUserId }
			.filterNot { _ == null }
			.distinct

	def mapToAssignments(results: RichSearchResults) = 
		results.transformAll(toParsedAuditEvents)
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
		search(all(term("eventType" -> "AddAssignment"), assignmentRangeRestriction(assignment, Option(assignment.createdDate))))
			.transform { doc: Document =>
				doc.getField("eventDate") match {
					case field: StoredField if field.numericValue() != null =>
						Some(new DateTime(field.numericValue()))
					case _ =>
						None
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
	final val apiIndexName = "audit"

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
		"submissions",
		"attachments")

	@Autowired var service: AuditEventService = _
	@Autowired var assignmentService: AssessmentService = _
	@Value("${filesystem.index.audit.dir}") override var indexPath: File = _
	@Value("${audit.index.weeksbacklog}") var weeksBacklog: Int = _

	override val analyzer = {
		val token = new KeywordAnalyzer()
		val whitespace: Analyzer = new WhitespaceAnalyzer

		val tokenListMappings = tokenListFields.map(field => field -> whitespace)
		//val tokenMappings = tokenFields.map(field=> (field -> token))
		val mappings = tokenListMappings.toMap.asJava

		new PerFieldAnalyzerWrapper(token, mappings)
	}
	
	override val IdField = "id"
	override def getId(item: AuditEvent) = item.id.toString
	
	override val UpdatedDateField = "eventDate"
	override def getUpdatedDate(item: AuditEvent) = item.eventDate
	
	override def listNewerThan(startDate: DateTime, batchSize: Int) =
		service.listNewerThan(startDate, batchSize).filter { _.eventStage == "before" }

	/**
	 * Set merge policy so that when segments of index are merged, it sorts them
	 * by descending docvalue (date) at the same time. Then a correctly configured
	 * search query can more efficiently look by the same sort
	 */
	override def configureIndexWriter(config: IndexWriterConfig) {
		val sort: Sort = new Sort(new SortedNumericSortField(UpdatedDateField, SortField.Type.LONG))
		val sortingMergePolicy = new SortingMergePolicy(config.getMergePolicy, sort)
		config.setMergePolicy(sortingMergePolicy)
	}

	/**
	 * Convert a list of Lucene Documents to a list of AuditEvents.
	 * Any events not found in the database will be returned as placeholder
	 * events with whatever data we kept in the Document, just in case
	 * an event went missing and we'd like to see the data.
	 */
	protected def toItems(docs: Seq[Document]): Seq[AuditEvent] = {
		// Pair Documents up with the contained ID if present
		val docIds = docs.map { doc =>
			doc -> documentValue(doc, IdField).map {
				_.toLong
			}
		}
		val ids = docIds.flatMap {
			case (_, id) => id
		}
		// Most events should be in the DB....
		val eventsMap = service.getByIds(ids)
			.map { event => (event.id, event) }
			.toMap

		// But we will return placeholder items for any IDs that weren't.
		docIds.map {
			case (doc, id) =>
				id.flatMap(eventsMap.get).getOrElse(placeholderEventFromDoc(doc))
		}
	}

	/** A placeholder AuditEvent to display if a Document has no matching event in the DB. */
	private def placeholderEventFromDoc(doc: Document) = {
		val event = AuditEvent()
		event.eventStage = "before"
		event.data = "{}" // We can't restore this if it's not from the db
		documentValue(doc, IdField).foreach { id => event.id = id.toLong }
		documentValue(doc, "eventId").foreach { id => event.eventId = id }
		documentValue(doc, "userId").foreach { id => event.userId = id }
		documentValue(doc, "masqueradeUserId").foreach { id => event.masqueradeUserId = id }
		documentValue(doc, "eventType").foreach { typ => event.eventType = typ }
		documentValue(doc, UpdatedDateField).foreach { ts => event.eventDate = new DateTime(ts.toLong) }
		event
	}

	protected def toParsedAuditEvents(doc: Seq[Document]): Seq[AuditEvent] = toItems(doc).map { event =>
		event.parsedData = service.parseData(event.data)
		event.related.map { e =>
			e.parsedData = service.parseData(e.data)
		}
		event
	}

	protected def auditEvents(results: RichSearchResults) = results.transformAll(toItems)
	protected def parsedAuditEvents(results: RichSearchResults) = results.transformAll(toParsedAuditEvents)

	/**
	 * TODO reuse one Document and set of Fields for all items
	 */
	protected def toDocuments(item: AuditEvent): Seq[Document] = {
		val doc = new Document
		doc add plainStringField(IdField, item.id.toString)
		doc add plainStringField("eventType", item.eventType)

		if (item.related == null || item.related.isEmpty) {
			service.addRelated(item)
		}

		if (item.eventId != null) { // null for old events
			doc add plainStringField("eventId", item.eventId)
		}
		if (item.userId != null) { // system-run actions have no user
			doc add plainStringField("userId", item.userId)
		}
		if (item.masqueradeUserId != null) {
			doc add plainStringField("masqueradeUserId", item.masqueradeUserId)
		}

		// add data from all stages of the event, before and after.
		for (i <- item.related) {
			service.parseData(i.data) match {
				case None => // no valid JSON
				case Some(data) => addDataToDoc(data, doc)
			}
		}

		doc add dateField(UpdatedDateField, item.eventDate)
		doc add docValuesField(UpdatedDateField, item.eventDate.getMillis)
		Seq(doc)
	}

	def openQuery(queryString: String, start: Int, count: Int) = {
		logger.info("Opening query: " + queryString)
		val query = parser.parse(queryString)
		val docs = search(query,
			sort = reverseDateSort,
			offset = start,
			max = count)
		docs.transformAll(toItems)
	}
	
	private def addFieldToDoc(field: String, data: Map[String, Any], doc: Document) = data.get(field) match  {
		case Some(value: String) =>
			doc add plainStringField(field, value, isStored = false)
		case Some(value: Boolean) =>
			doc add plainStringField(field, value.toString, isStored = false)
		case _ => // missing or not a string
	}
	
	private def addSequenceToDoc(field: String, data: Map[String, Any], doc: Document) = data.get(field).collect {
		case ids: JList[_] => doc add seqField(field, ids.asScala)
		case ids: Seq[_] => doc add seqField(field, ids)
		case ids: Array[String] => doc add seqField(field, ids)
		case other: AnyRef => logger.warn("Collection field " + field + " was unexpected type: " + other.getClass.getName)
		case _ =>
	}
	
	val SingleDataFields = Seq("submission", "feedback", "assignment", "module", "department", "studentId", "submissionIsNoteworthy")
	val SequenceDataFields = Seq("students", "attachments")

	// pick items out of the auditevent JSON and add them as document fields.
	private def addDataToDoc(data: Map[String, Any], doc: Document) = {
		SingleDataFields.foreach(addFieldToDoc(_, data, doc))
		
		// sequence-type fields
		SequenceDataFields.foreach(addSequenceToDoc(_, data, doc))
	}
}

trait AuditEventIndexServiceComponent {
	def auditEventIndexService: AuditEventIndexService
}

trait AutowiringAuditEventIndexServiceComponent extends AuditEventIndexServiceComponent {
	var auditEventIndexService = Wire[AuditEventIndexService]
}