package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import org.joda.time.DateTime
import uk.ac.warwick.userlookup.User

// empty class to expose bean properties via constructor
class Activity(val title: String, val message: String, val date: DateTime, val agent: User) {}

// object to do construction based on other types
object Activity {
	var userLookup = Wire.auto[UserLookupService]

	// TODO make this a matcher for different eventTypes
	def apply(auditEvent: AuditEvent): Activity = {
		val title = "A title"
		val message = "The message"
		val date = auditEvent.eventDate
		val agent = userLookup.getUserByUserId(auditEvent.userId)
		new Activity(title, message, date, agent)
	}
}
