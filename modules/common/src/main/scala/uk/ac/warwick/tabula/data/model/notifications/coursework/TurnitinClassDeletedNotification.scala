package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import org.springframework.util.Assert
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleRecipientNotification}

@Entity
@DiscriminatorValue(value="TurnitinClassDeleted")
class TurnitinClassDeletedNotification
		extends TurnitinReportNotification
		with SingleRecipientNotification {

	def title = "%s: The Turnitin check for \"%s\" has not completed successfully".format(assignment.module.code.toUpperCase, assignment.name)

	@transient val className = StringSetting("className", "")
	@transient val classId = StringSetting("classId", "")

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/turnitinclassdeleted.ftl", Map(
		"assignment" -> assignment,
		"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
		"path" -> url,
		"className" -> className.value,
		"classId" -> classId.value
	))

	def recipient = agent

	override def onPreSave(newRecord: Boolean) {
		Assert.isTrue(className.isDefined, "className must be set")
		Assert.isTrue(classId.isDefined, "classId must be set")
	}
}
