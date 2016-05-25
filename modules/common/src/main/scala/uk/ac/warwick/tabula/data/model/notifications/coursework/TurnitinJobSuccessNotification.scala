package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleRecipientNotification}
import scala.collection.JavaConverters._

@Entity
@DiscriminatorValue(value="TurnitinJobSuccess")
class TurnitinJobSuccessNotification
	extends TurnitinReportNotification
	with SingleRecipientNotification {

	def title = "%s: The Turnitin check for \"%s\" has finished".format(assignment.module.code.toUpperCase, assignment.name)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/turnitinjobdone.ftl", Map(
		"assignment" -> assignment,
		"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
		"failureCount" -> items.size,
		"failedReports" -> items.asScala.map(_.entity),
		"path" -> url
	))

	def recipient = agent
}