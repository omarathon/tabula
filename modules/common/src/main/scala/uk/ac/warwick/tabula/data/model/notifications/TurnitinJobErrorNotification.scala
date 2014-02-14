package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, FreemarkerModel}
import javax.persistence.{Entity, DiscriminatorValue}

@Entity
@DiscriminatorValue(value="TurnitinJobError")
class TurnitinJobErrorNotification
		extends TurnitinReportNotification
		with SingleRecipientNotification {

	def title = "Turnitin check has not completed successfully for %s - %s" format (assignment.module.code.toUpperCase, assignment.name)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/turnitinjobfailed.ftl", Map(
		"assignment" -> assignment,
		"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
		"path" -> url
	))

	def recipient = agent
}
