package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.cm2.departments.FeedbackReportCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

abstract class AbstractFeedbackReportController extends CourseworkController
	with DepartmentScopedController with AcademicYearScopedController
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
	def command(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear], user: CurrentUser): FeedbackReportCommand.Command =
		FeedbackReportCommand(mandatory(department), activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)), user)

	@RequestMapping(params = Array("!jobId"))
	def requestReport(@ModelAttribute("feedbackReportCommand") cmd: FeedbackReportCommand.Command, @PathVariable department: Department): Mav =
		Mav("cm2/admin/assignments/feedbackreport/report_range", "academicYear" -> cmd.academicYear)
			.crumbsList(Breadcrumbs.department(department, Some(cmd.academicYear)))
			.secondCrumbs(academicYearBreadcrumbs(cmd.academicYear)(Routes.admin.feedbackReports(department, _)): _*)

	@RequestMapping(method = Array(POST), params = Array("!jobId"))
	def generateReport(@Valid @ModelAttribute("feedbackReportCommand") cmd: FeedbackReportCommand.Command, errors: Errors, @PathVariable department: Department): Mav = {
		if (errors.hasErrors) {
			requestReport(cmd, department)
		} else {
			val job = cmd.apply()
			Mav("cm2/admin/assignments/feedbackreport/progress", "job" -> job, "academicYear" -> cmd.academicYear)
				.crumbsList(Breadcrumbs.department(department, Some(cmd.academicYear)))
				.secondCrumbs(academicYearBreadcrumbs(cmd.academicYear)(Routes.admin.feedbackReports(department, _)): _*)
		}
	}

	@RequestMapping(params = Array("jobId"))
	def checkProgress(@RequestParam jobId: String, @PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): Mav = {
		val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
		val job = jobService.getInstance(jobId)
		Mav("cm2/admin/assignments/feedbackreport/progress", "job" -> job)
			.crumbsList(Breadcrumbs.department(department, Some(academicYear)))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(Routes.admin.feedbackReports(department, _)): _*)
			.noLayoutIf(ajax)
	}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/reports/feedback"))
class FeedbackReportController extends AbstractFeedbackReportController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] =
		retrieveActiveAcademicYear(None)

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/{academicYear:\\d{4}}/reports/feedback"))
class FeedbackReportForYearController extends AbstractFeedbackReportController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
		retrieveActiveAcademicYear(Option(academicYear))

}