package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.cm2.departments.FeedbackReportCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/reports/feedback"))
class FeedbackReportController extends CourseworkController
	with DepartmentScopedController
	with AutowiringUserSettingsServiceComponent
	with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent
	with AutowiringJobServiceComponent {

	override val departmentPermission: Permission = FeedbackReportCommand.AdminPermission

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] =
		retrieveActiveDepartment(Option(department))

	validatesSelf[SelfValidating]

	@ModelAttribute("feedbackReportCommand")
	def command(@PathVariable department: Department, user: CurrentUser): FeedbackReportCommand.Command =
		FeedbackReportCommand(mandatory(department), user)

	@RequestMapping(params = Array("!jobId"))
	def requestReport(@PathVariable department: Department): Mav =
		Mav("cm2/admin/assignments/feedbackreport/report_range")
			.crumbs(Breadcrumbs.Department(department))

	@RequestMapping(method = Array(POST), params = Array("!jobId"))
	def generateReport(@Valid @ModelAttribute("feedbackReportCommand") cmd: FeedbackReportCommand.Command, errors: Errors, @PathVariable department: Department): Mav = {
		if (errors.hasErrors) {
			requestReport(department)
		} else {
			val job = cmd.apply()
			Mav("cm2/admin/assignments/feedbackreport/progress", "job" -> job)
				.crumbs(Breadcrumbs.Department(department))
		}
	}

	@RequestMapping(params = Array("jobId"))
	def checkProgress(@RequestParam jobId: String, @PathVariable department: Department): Mav = {
		val job = jobService.getInstance(jobId)
		Mav("cm2/admin/assignments/feedbackreport/progress", "job" -> job)
			.crumbs(Breadcrumbs.Department(department))
			.noLayoutIf(ajax)
	}

}