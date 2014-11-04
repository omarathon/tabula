package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleRecipientNotification}

@Entity
@DiscriminatorValue(value="TurnitinJobSuccess")
class TurnitinJobSuccessNotification
	extends TurnitinReportNotification
	with SingleRecipientNotification {

	@transient val failedUploads = StringMapSetting("failedUploads", Map())

	def title = "%s: The Turnitin check for \"%s\" has finished".format(assignment.module.code.toUpperCase, assignment.name)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/turnitinjobdone.ftl", Map(
		"assignment" -> assignment,
		"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
		"failureCount" -> failedUploads.value.size,
		"failedUploads" -> failedUploads.value,
		"path" -> url
	))

	def recipient = agent
}