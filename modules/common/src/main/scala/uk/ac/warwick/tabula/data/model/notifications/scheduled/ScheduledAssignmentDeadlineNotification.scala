package uk.ac.warwick.tabula.data.model.notifications.scheduled

import uk.ac.warwick.tabula.data.model.{Assignment, ScheduledNotification}
import org.joda.time.{DateTime, Days}
import javax.persistence.{DiscriminatorValue, Entity}

@Entity
@DiscriminatorValue("assignmentDeadline")
class ScheduledAssignmentDeadlineNotification(assignment: Assignment, when: DateTime) extends ScheduledNotification[Assignment](assignment, when) {

	override def generateNotifications = ???
}
