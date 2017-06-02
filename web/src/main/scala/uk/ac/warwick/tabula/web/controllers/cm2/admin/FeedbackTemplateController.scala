package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.cm2.departments.{BulkFeedbackTemplateCommand, DeleteFeedbackTemplateCommand, EditFeedbackTemplateCommand}
import uk.ac.warwick.tabula.data.model.{Department, FeedbackTemplate}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/settings/feedback-templates"))
class FeedbackTemplateController extends CourseworkController
	with DepartmentScopedController with AutowiringUserLookupComponent
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringCourseAndRouteServiceComponent with AutowiringMaintenanceModeServiceComponent {

	validatesSelf[SelfValidating]

	override def departmentPermission: Permission = Permissions.FeedbackTemplate.Manage

	@ModelAttribute("bulkFeedbackTemplateCommand")
	def bulkFeedbackTemplateCommand(@PathVariable department: Department) =
		new BulkFeedbackTemplateCommand(mandatory(department))

	@ModelAttribute("activeDepartment")
	def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@RequestMapping
	def list(@PathVariable department: Department): Mav =
		Mav("cm2/admin/feedbackforms/manage-feedback-templates")
			.crumbsList(Breadcrumbs.department(department, None))

	@RequestMapping(method = Array(POST))
	def saveBulk(@Valid @ModelAttribute("bulkFeedbackTemplateCommand") cmd: BulkFeedbackTemplateCommand, errors: Errors, @PathVariable department: Department): Mav =
		if (errors.hasErrors) list(department)
		else {
			cmd.apply()
			Redirect(Routes.admin.feedbackTemplates(cmd.department))
		}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/settings/feedback-templates/edit/{template}"))
class EditFeedbackTemplateController extends CourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("editFeedbackTemplateCommand")
	def editFeedbackTemplateCommand(@PathVariable department: Department, @PathVariable template: FeedbackTemplate) =
		new EditFeedbackTemplateCommand(department, template)

	@RequestMapping
	def edit(): Mav =
		Mav("cm2/admin/feedbackforms/edit-feedback-template").noNavigation()

	@RequestMapping(method = Array(POST))
	def save(@Valid @ModelAttribute("editFeedbackTemplateCommand") cmd: EditFeedbackTemplateCommand, errors: Errors, @PathVariable department: Department): Mav =
		if (errors.hasErrors) edit()
		else {
			cmd.apply()
			Mav("ajax_success").noNavigation()
		}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/settings/feedback-templates/delete/{template}"))
class DeleteFeedbackTemplateController extends CourseworkController {

	@ModelAttribute("deleteFeedbackTemplateCommand")
	def deleteFeedbackTemplateCommand(@PathVariable department: Department, @PathVariable template: FeedbackTemplate) =
		new DeleteFeedbackTemplateCommand(department, template)

	@RequestMapping
	def deleteCheck(): Mav =
		Mav("cm2/admin/feedbackforms/delete-feedback-template").noNavigation()

	@RequestMapping(method = Array(POST))
	def delete(@Valid @ModelAttribute("deleteFeedbackTemplateCommand") cmd: DeleteFeedbackTemplateCommand, errors: Errors): Mav =
		if (errors.hasErrors) deleteCheck()
		else {
			cmd.apply()
			Mav("ajax_success").noNavigation()
		}

}