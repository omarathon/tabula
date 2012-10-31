package uk.ac.warwick.courses.commands.departments

import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.commands.{UploadedFile, Description, Command}
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.model.{FeedbackTemplate, Department}
import org.springframework.transaction.annotation.Transactional
import reflect.BeanProperty
import uk.ac.warwick.courses.helpers.ArrayList

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
	}
}
