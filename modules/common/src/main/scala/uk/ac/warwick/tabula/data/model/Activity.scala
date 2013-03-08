package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import org.joda.time.DateTime
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.AssignmentService

/** Class to expose bean properties via constructor.
 * 
 *  It currently takes a single entity, as we're currently only interested in
 *  simple relations between agents (users) and entities (objects),
 *  eg. Student does Submission
 *  In future, perhaps we'd extend this to take a collection of entities
 *  for more complex interactions.
 */
class Activity[A](val title: String, val date: DateTime, val agent: User, val message: String, val entity: A) {

	// Expose entity type for Freemarker
	def getEntityType = entity.getClass.getSimpleName
}

/** Companion object offers apply method to construct new Activities,
 *  drawing its data from other types.
 */
object Activity {
	var userLookup = Wire.auto[UserLookupService]
	var assignmentService = Wire.auto[AssignmentService]

	// given an AuditEvent...
	def apply(event: AuditEvent): Option[Activity[Any]] = {
		event.eventType match {
			case "SubmitAssignment" if (event.hasProperty("submission")) => {
				val submission = assignmentService.getSubmission(event.submissionId getOrElse "")
				
				if (submission.isDefined) {
					val title = "New submission"
					val date = event.eventDate
					val agent = userLookup.getUserByUserId(event.userId)
					val activity = new Activity[Any](title, date, agent, "", submission.get)
					Option(activity)
				} else {
					val title = "New submission (since deleted)"
					val date = event.eventDate
					val agent = userLookup.getUserByUserId(event.userId)
					val activity = new Activity[Any](title, date, agent, "", Nil)
					Option(activity)
				}
			}
			case _ => {
				None
			}
		}
	}
	
//	private def splitCamelCase(src: String): String = src.replaceAll(
//		String.format("%s|%s|%s",
//			"(?<=[A-Z])(?=[A-Z][a-z])",
//			"(?<=[^A-Z])(?=[A-Z])",
//			"(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
}
