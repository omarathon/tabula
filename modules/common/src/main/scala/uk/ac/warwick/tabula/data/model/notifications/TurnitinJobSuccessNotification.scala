package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, FreemarkerModel}
import javax.persistence.{Entity, DiscriminatorValue}

@Entity
@DiscriminatorValue(value="TurnitinJobSuccess")
class TurnitinJobSuccessNotification
	extends TurnitinReportNotification
	with SingleRecipientNotification {

	@transient val failedUploads = StringMapSetting("failedUploads", Map())

	def title = "Turnitin check finished for %s - %s" format (assignment.module.code.toUpperCase, assignment.name)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/turnitinjobdone.ftl", Map(
		"assignment" -> assignment,
		"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
		"failureCount" -> failedUploads.value.size,
		"failedUploads" -> failedUploads.value,
		"path" -> url
	))

	def recipient = agent
}