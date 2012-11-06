package uk.ac.warwick.courses.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMethod, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.data.model.{FeedbackTemplate, Department}
import uk.ac.warwick.courses.commands.departments.{DeleteFeedbackTemplateCommand, EditFeedbackTemplateCommand, BulkFeedbackTemplateCommand}
import uk.ac.warwick.courses.web.Mav
import scala.Array
import org.springframework.validation.Errors
import uk.ac.warwick.courses.actions.Manage

@Controller
@RequestMapping(Array("/admin/department/{dept}/settings/feedback-templates"))
class FeedbackTemplateController extends BaseController {

	@ModelAttribute def bulkFeedbackTemplateCommand(@PathVariable dept:Department)
		= new BulkFeedbackTemplateCommand(dept)
	@ModelAttribute def editFeedbackTemplateCommand(@PathVariable dept:Department)
		= new EditFeedbackTemplateCommand(dept)
	@ModelAttribute def deleteFeedbackTemplateCommand(@PathVariable dept:Department)
		= new DeleteFeedbackTemplateCommand(dept)

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, dept:Department):Mav = mav.crumbs(Breadcrumbs.Department(dept))

	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def list(@PathVariable dept: Department, cmd:BulkFeedbackTemplateCommand, errors:Errors) = {
		cmd.feedbackTemplates = dept.feedbackTemplates
		mustBeAbleTo(Manage(dept))
		val model = Mav("admin/feedbackforms/manage-feedback-templates",
			"department" -> dept
		)
		crumbed(model, dept)
	}

	@RequestMapping(method=Array(RequestMethod.POST))
	def saveBulk(cmd:BulkFeedbackTemplateCommand, errors:Errors) = {
		mustBeAbleTo(Manage(cmd.department))
		cmd.onBind()
		if (errors.hasErrors){
			list(cmd.department, cmd, errors)
		}
		else{
			cmd.apply()
			Reload()
		}
	}

	@RequestMapping(value=Array("edit/{template}"), method=Array(GET))
	def edit(@PathVariable dept: Department, @PathVariable template:FeedbackTemplate,
	cmd:EditFeedbackTemplateCommand, errors:Errors) = {
		mustBeAbleTo(Manage(dept))

		cmd.template = template
		cmd.id = template.id
		cmd.name = template.name
		cmd.description = template.description

		val model = Mav("admin/feedbackforms/edit-feedback-template",
			"department" -> dept
		).noNavigation()
		model
	}

	@RequestMapping(value=Array("save"), method=Array(RequestMethod.POST))
	def save(cmd:EditFeedbackTemplateCommand, errors:Errors) = {
		mustBeAbleTo(Manage(cmd.department))
		cmd.onBind()
		if (errors.hasErrors){
			edit(cmd.department, cmd.template, cmd, errors)
		}
		else{
			cmd.apply()
			val model = Mav("ajax_success").noNavigation()
			model
		}
	}

	@RequestMapping(value=Array("delete/{template}"), method=Array(GET))
	def deleteCheck(@PathVariable dept: Department, @PathVariable template:FeedbackTemplate,
			   cmd:DeleteFeedbackTemplateCommand, errors:Errors) = {
		mustBeAbleTo(Manage(dept))

		cmd.id = template.id
		val model = Mav("admin/feedbackforms/delete-feedback-template",
			"department" -> dept
		).noNavigation()
		model

	}

	@RequestMapping(value=Array("delete"), method=Array(POST))
	def delete(@PathVariable dept: Department, cmd:DeleteFeedbackTemplateCommand, errors:Errors) = {
		mustBeAbleTo(Manage(dept))

		cmd.apply()
		val model = Mav("ajax_success").noNavigation()
		model
	}

}