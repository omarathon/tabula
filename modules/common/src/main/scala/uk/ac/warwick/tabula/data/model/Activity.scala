package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import org.joda.time.DateTime
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.AssignmentService

// class to expose bean properties via constructor
class Activity[T](val title: String, val date: DateTime, val agent: User, val message: String, val entity: T) {

	// Expose entity type for Freemarker
	def getEntityType = entity.getClass.getSimpleName
}

/** companion object offers apply method to construct new Activities,
 *  drawing its data from other types
 * 
 *  currently, it's ignoring away everything except for
 *  noteworthy submissions (late, auth late, suspected plagiarised)
						
 */
object Activity {
	var userLookup = Wire.auto[UserLookupService]
	var assignmentService = Wire.auto[AssignmentService]

	// given an AuditEvent...
	def apply(event: AuditEvent): Option[Activity[Any]] = {
		event.eventType match {
			case "SubmitAssignment" if (event.hasProperty("submission")) => {
				val submission = assignmentService.getSubmission(event.submissionId getOrElse "")
				
				if (submission.isDefined && submission.get.isNoteworthy) {
					val title = "New submission"
					val date = event.eventDate
					val agent = userLookup.getUserByUserId(event.userId)
					val activity = new Activity[Any](title, date, agent, "", submission.get)
					Option(activity)
				} else None
			}
			case _ => {
				None
			}
		}
	}
	
	private def splitCamelCase(src: String): String = src.replaceAll(
		String.format("%s|%s|%s",
			"(?<=[A-Z])(?=[A-Z][a-z])",
			"(?<=[^A-Z])(?=[A-Z])",
			"(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
}
