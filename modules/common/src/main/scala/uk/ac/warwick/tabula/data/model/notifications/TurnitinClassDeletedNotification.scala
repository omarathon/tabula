package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, FreemarkerModel}
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import org.springframework.util.Assert

@Entity
@DiscriminatorValue(value="TurnitinClassDeleted")
class TurnitinClassDeletedNotification
		extends TurnitinReportNotification
		with SingleRecipientNotification
		with PreSaveBehaviour {

	def title = "Turnitin check has not completed successfully for %s - %s" format (assignment.module.code.toUpperCase, assignment.name)

	@transient val className = StringSetting("className")
	@transient val classId = StringSetting("classId")

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/turnitinclassdeleted.ftl", Map(
		"assignment" -> assignment,
		"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
		"path" -> url,
		"className" -> className.value,
		"classId" -> classId.value
	))

	def recipient = agent

	override def preSave(newRecord: Boolean) {
		Assert.isTrue(className.value.isDefined, "className must be set")
		Assert.isTrue(classId.value.isDefined, "classId must be set")
	}
}
