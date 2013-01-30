package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMethod, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{FeedbackTemplate, Department}
import uk.ac.warwick.tabula.coursework.commands.departments.{DeleteFeedbackTemplateCommand, EditFeedbackTemplateCommand, BulkFeedbackTemplateCommand}
import uk.ac.warwick.tabula.web.Mav
import scala.Array
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(Array("/admin/department/{dept}/settings/feedback-templates"))
class FeedbackTemplateController extends CourseworkController {

	@ModelAttribute def bulkFeedbackTemplateCommand(@PathVariable("dept") dept:Department)
		= new BulkFeedbackTemplateCommand(dept)

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, dept:Department):Mav = mav.crumbs(Breadcrumbs.Department(dept))

	@RequestMapping(method=Array(GET, HEAD))
	def list(cmd:BulkFeedbackTemplateCommand, errors:Errors) = {
		val dept = cmd.department
		
		val model = Mav("admin/feedbackforms/manage-feedback-templates",
			"department" -> dept
		)
		crumbed(model, dept)
	}

	@RequestMapping(method=Array(POST))
	def saveBulk(cmd:BulkFeedbackTemplateCommand, errors:Errors) = {
		if (errors.hasErrors){
			list(cmd, errors)
		}
		else{
			cmd.apply()
			Redirect(Routes.admin.feedbackTemplates(cmd.department))
		}
	}

}

@Controller
@RequestMapping(Array("/admin/department/{dept}/settings/feedback-templates/edit/{template}"))
class EditFeedbackTemplateController extends CourseworkController {
	
	@ModelAttribute def editFeedbackTemplateCommand(@PathVariable("dept") dept:Department, @PathVariable("template") template:FeedbackTemplate)
		= new EditFeedbackTemplateCommand(dept, template)
	
	@RequestMapping(method=Array(GET))
	def edit(cmd:EditFeedbackTemplateCommand, errors:Errors) = {
		val dept = cmd.department
		val template = cmd.template
		
		cmd.id = template.id
		cmd.name = template.name
		cmd.description = template.description

		val model = Mav("admin/feedbackforms/edit-feedback-template",
			"department" -> dept,
			"template" -> template
		).noNavigation()
		model
	}

	@RequestMapping(method=Array(POST))
	def save(cmd:EditFeedbackTemplateCommand, errors:Errors) = {
		if (errors.hasErrors){
			edit(cmd, errors)
		}
		else{
			cmd.apply()
			val model = Mav("ajax_success").noNavigation()
			model
		}
	}
	
}

@Controller
@RequestMapping(Array("/admin/department/{dept}/settings/feedback-templates/delete/{template}"))
class DeleteFeedbackTemplateController extends CourseworkController {
	
	@ModelAttribute def deleteFeedbackTemplateCommand(@PathVariable("dept") dept:Department, @PathVariable("template") template:FeedbackTemplate)
		= new DeleteFeedbackTemplateCommand(dept, template)

	@RequestMapping(method=Array(GET))
	def deleteCheck(cmd:DeleteFeedbackTemplateCommand, errors:Errors) = {
		val template = cmd.template
		val dept = cmd.department
		
		cmd.id = template.id
		val model = Mav("admin/feedbackforms/delete-feedback-template",
			"department" -> dept,
			"template" -> template
		).noNavigation()
		model

	}

	@RequestMapping(method=Array(POST))
	def delete(cmd:DeleteFeedbackTemplateCommand, errors:Errors) = {
		cmd.apply()
		val model = Mav("ajax_success").noNavigation()
		model
	}
	
}