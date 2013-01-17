package uk.ac.warwick.tabula.coursework.commands.departments

import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.{Autowired, Configurable}
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.{FeedbackTemplate, Department}
import org.springframework.transaction.annotation.Transactional
import reflect.BeanProperty
import uk.ac.warwick.tabula.helpers.{Logging, ArrayList}
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.actions.Manage

abstract class FeedbackTemplateCommand(val department:Department)
	extends Command[Unit] with Daoisms with BindListener {
	
	PermissionsCheck(Manage(department))

	@BeanProperty var file:UploadedFile = new UploadedFile
	@BeanProperty var feedbackTemplates:JList[FeedbackTemplate] = ArrayList()

	override def onBind {
		transactional() {
			file.onBind
		}
	}

	// describe the thing that's happening.
	override def describe(d:Description) {
		d.properties("department" -> department.code)
	}
}

class BulkFeedbackTemplateCommand(department:Department) extends FeedbackTemplateCommand(department){

	override def applyInternal() {
		transactional() {
			if (!file.attached.isEmpty) {
				for (attachment <- file.attached) {
					val feedbackForm = new FeedbackTemplate
					feedbackForm.name = attachment.name
					feedbackForm.department = department
					feedbackForm.attachFile(attachment)
					feedbackTemplates.add(feedbackForm)
					session.saveOrUpdate(feedbackForm)
				}
			}
			department.feedbackTemplates = feedbackTemplates
			session.saveOrUpdate(department)
		}
	}
}

class EditFeedbackTemplateCommand(department:Department, val template: FeedbackTemplate) extends FeedbackTemplateCommand(department) {

	var zipService = Wire.auto[ZipService]

	@BeanProperty var id:String = _
	@BeanProperty var name:String = _
	@BeanProperty var description:String = _

	override def applyInternal() {
		transactional() {
			val feedbackTemplate= department.feedbackTemplates.find(_.id == id).get
			feedbackTemplate.name = name
			feedbackTemplate.description = description
			feedbackTemplate.department = department
			if (!file.attached.isEmpty) {
				for (attachment <- file.attached) {
					feedbackTemplate.attachFile(attachment)
				}
			}
			session.update(feedbackTemplate)
			// invalidate any zip files for linked assignments
			feedbackTemplate.assignments.foreach(zipService.invalidateSubmissionZip(_))
		}
	}
}

class DeleteFeedbackTemplateCommand(department:Department, val template: FeedbackTemplate) extends FeedbackTemplateCommand(department) with Logging {

	@BeanProperty var id:String = _

	override def applyInternal() {
		transactional() {
			val feedbackTemplate= department.feedbackTemplates.find(_.id == id).get
			if (feedbackTemplate.hasAssignments)
				logger.error("Cannot delete feedbackt template "+feedbackTemplate.id+" - it is still linked to assignments")
			else
				session.delete(feedbackTemplate)
		}
	}
}