package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{SingleItemNotification, FreemarkerModel, Assignment, Notification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.coursework.web.Routes
import javax.persistence.{Entity, DiscriminatorValue}

@Entity
@DiscriminatorValue("RequestAssignmentAccess")
class RequestAssignmentAccessNotification
	extends Notification[Assignment, Unit] with SingleItemNotification[Assignment] {

	def assignment = item.entity

	def verb = "request"
	def title = assignment.module.code.toUpperCase + ": Access request"

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/requestassignmentaccess.ftl", Map(
		"assignment" -> assignment,
		"student" -> agent,
		"path" -> url)
	)

	def url = Routes.admin.assignment.edit(assignment)

	def recipients = assignment.module.department.owners.users
		.filter(admin => admin.isFoundUser && admin.getEmail.hasText).toSeq


}
