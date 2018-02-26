package uk.ac.warwick.tabula.services.cm2

import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Assignment, AuditEvent}
import uk.ac.warwick.tabula.services.AutowiringAuditEventServiceComponent
import uk.ac.warwick.tabula.services.elasticsearch.AutowiringAuditEventQueryServiceComponent
import scala.collection.JavaConverters._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

trait AssignmentAuditQueryService {
	def getAuditEventsForAssignment(assignment: Assignment): Seq[AssignmentAuditEvent]
}

trait AssignmentAuditQueryServiceComponent {
	def assignmentAuditQueryService: AssignmentAuditQueryService
}

trait AutowiringAssignmentAuditQueryServiceComponent extends AssignmentAuditQueryServiceComponent {
	override def assignmentAuditQueryService: AssignmentAuditQueryService = Wire.auto[AssignmentAuditEventIndexQueryService]
}

@Service
class AssignmentAuditEventIndexQueryService extends AssignmentAuditQueryService
	with AutowiringAuditEventServiceComponent
	with AutowiringAuditEventQueryServiceComponent {

	override def getAuditEventsForAssignment(assignment: Assignment): Seq[AssignmentAuditEvent] = {
		val future: Future[Seq[AssignmentAuditEvent]] = auditEventQueryService.query(s"assignment:${assignment.id}", 0, 100)
			.map(auditEvents => auditEvents.map(e => {
				val event = e.copy(parsedData = auditEventService.parseData(e.data))
				event.copy(related = Seq(event))
			}).flatMap(toAssignmentAuditEvent))

		Await.result(future, Duration(15, SECONDS))
	}

	def toAssignmentAuditEvent(e: AuditEvent): Option[AssignmentAuditEvent] = {
		e.eventType match {
			case "AddAssignment" => Some(AssignmentAuditEvent(e, "Create assignment", "Created"))
			case "AllocateModerators" => Some(AssignmentAuditEvent(e, "Allocate submissions to moderators", "Allocated"))
			case "AssignMarkers" => Some(AssignmentAuditEvent(e, "Assign markers", "Assigned"))
			case "AssignMarkersSmallGroups" => Some(AssignmentAuditEvent(e, "Assign markers from small group tutors", "Assigned"))
			case "DeleteSubmissionsAndFeedback" => Some(AssignmentAuditEvent(e, "Delete submissions and feedback", "Deleted"))
			case "EditAssignmentDetails" => Some(AssignmentAuditEvent(e, "Edit assignment details", "Edited"))
			case "ModifyAssignmentFeedback" => Some(AssignmentAuditEvent(e, "Edit feedback options", "Edited"))
			case "ModifyAssignmentOptions" => Some(AssignmentAuditEvent(e, "Edit assignment options", "Edited"))
			case "ModifyAssignmentSubmissions" => Some(AssignmentAuditEvent(e, "Edit submission options", "Edited"))
			case "PlagiarismInvestigation" => Some(AssignmentAuditEvent(e, "Modify plagiarism flag", "Modified"))
			case "ReleaseForMarking" => Some(AssignmentAuditEvent(e, "Release submissions for marking", "Edited"))
			case "ReturnToMarker" => Some(AssignmentAuditEvent(e, "Return submissions for marking", "Returned"))
			case "PublishFeedback" => Some(AssignmentAuditEvent(e, "Publish feedback", "Published"))
			case _ => None
		}
	}
}

object AssignmentAuditEvent {
	// e.g. ModifyAssignmentOptions => Modify assignment options
	private def toSentenceCase(string: String): String = StringUtils.capitalize(StringUtils.splitByCharacterTypeCamelCase(string).mkString(" ").toLowerCase)
}

case class AssignmentAuditEvent(event: AuditEvent, name: String, verbed: String) {
	import AssignmentAuditEvent._

	def maybeSummary: Option[String] = Option(summary)

	def students: Set[String] = (event.students ++ event.studentUsercodes).toSet

	def summary: String =
		if (students.nonEmpty) {
			s"$verbed for ${students.size} student${if (students.size != 1) "s" else ""}"
		} else {
			null
		}

	def userId: String = event.userId

	def date: DateTime = event.eventDate

	def fields: Map[String, Any] = event.parsedData.getOrElse(Map.empty)
		.collect {
			case (key, value) if key.toLowerCase.contains("date") && value.isInstanceOf[Long] =>
				(key, new DateTime(value.asInstanceOf[Long]))
			case (key, value) if value != null =>
				(key, value)
		}
		.map { case (key, value) => toSentenceCase(key) -> value }

	def getFieldPairs: java.util.List[(String, Any)] = fields.toSeq.sortBy(_._1).asJava

	override def toString: String = s"${getClass.getSimpleName}[$name${maybeSummary.map(s => s" ($s)").getOrElse("")}]"
}

