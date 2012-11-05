package uk.ac.warwick.courses.commands.departments

import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.{Autowired, Configurable}
import uk.ac.warwick.courses.commands.{UploadedFile, Description, Command}
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.model.{FeedbackTemplate, Department}
import org.springframework.transaction.annotation.Transactional
import reflect.BeanProperty
import uk.ac.warwick.courses.helpers.{Logging, ArrayList}
import uk.ac.warwick.courses.services.ZipService

@Configurable
abstract class FeedbackTemplateCommand(val department:Department)
	extends Command[Unit] with Daoisms{

	@BeanProperty var file:UploadedFile = new UploadedFile
	@BeanProperty var feedbackTemplates:JList[FeedbackTemplate] = ArrayList()

	@Transactional
	def onBind() {
		file.onBind
	}

	// describe the thing that's happening.
	override def describe(d:Description) {
		d.properties("department" -> department.code)
	}
}

class BulkFeedbackTemplateCommand(department:Department) extends FeedbackTemplateCommand(department){

	@Transactional
	override def apply() {
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

class EditFeedbackTemplateCommand(department:Department) extends FeedbackTemplateCommand(department) {

	@Autowired var zipService: ZipService = _

	@BeanProperty var id:String = _
	@BeanProperty var name:String = _
	@BeanProperty var description:String = _
	@BeanProperty var template:FeedbackTemplate = _  // retained in case errors are found

	@Transactional
	override def apply() {
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

class DeleteFeedbackTemplateCommand(department:Department) extends FeedbackTemplateCommand(department) with Logging {

	@BeanProperty var id:String = _

	@Transactional
	override def apply() {
		val feedbackTemplate= department.feedbackTemplates.find(_.id == id).get
		if (feedbackTemplate.hasAssignments)
			logger.error("Cannot delete feedbackt template "+feedbackTemplate.id+" - it is still linked to assignments")
		else
			session.delete(feedbackTemplate)
	}
}