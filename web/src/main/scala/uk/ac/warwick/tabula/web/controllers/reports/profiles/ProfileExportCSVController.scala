package uk.ac.warwick.tabula.web.controllers.reports.profiles

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.reports.profiles.{AttendanceMonitoringStudentDataWithSCD, ProfileExportCSVCommand, ProfileExportCommand}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.jobs.reports.ProfileExportCSVJob
import uk.ac.warwick.tabula.services.ZipCreator
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.services.jobs.{AutowiringJobServiceComponent, JobInstance}
import uk.ac.warwick.tabula.services.objectstore.AutowiringObjectStorageServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.reports.{ReportsBreadcrumbs, ReportsController}
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/profiles/exportCSV"))
class ProfileExportCSVController extends ReportsController
	with AutowiringJobServiceComponent
	with AutowiringObjectStorageServiceComponent {

	//	@ModelAttribute("command")
	//	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
	//		ProfileExportCommand(department, academicYear, user)
	//
	//	@RequestMapping
	//	def generateReport(
	//		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringStudentDataWithSCD]],
	//		@PathVariable department: Department,
	//		@PathVariable academicYear: AcademicYear
	//	): Mav = {
	//		val results: Seq[AttendanceMonitoringStudentDataWithSCD] = cmd.apply()
	//		if (ajax) {
	//			Mav("reports/profiles/_filter", "results" -> results).noLayout()
	//		} else {
	//			Mav("reports/profiles/filter", "results" -> results).crumbs(
	//				ReportsBreadcrumbs.Home.Department(department),
	//				ReportsBreadcrumbs.Home.DepartmentForYear(department, academicYear),
	//				ReportsBreadcrumbs.Profiles.Home(department, academicYear)
	//			)
	//		}
	//	}


	var fileServer: FileServer = Wire[FileServer]

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		ProfileExportCSVCommand(department, academicYear, user)

	@RequestMapping(params = Array("!jobId"))
	def generateReport(
		@Valid @ModelAttribute("command") cmd: Appliable[JobInstance], errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			Mav("reports/profiles/exportCSV").crumbs(
				ReportsBreadcrumbs.Home.Department(department),
				ReportsBreadcrumbs.Home.DepartmentForYear(department, academicYear),
				ReportsBreadcrumbs.Profiles.Home(department, academicYear)
			)
		} else {
			val jobId = cmd.apply().id
			Mav("reports/profiles/exportCSV", "jobId" -> jobId).crumbs(
				ReportsBreadcrumbs.Home.Department(department),
				ReportsBreadcrumbs.Home.DepartmentForYear(department, academicYear),
				ReportsBreadcrumbs.Profiles.Home(department, academicYear)
			)
		}
	}

	@RequestMapping(params = Array("jobId"))
	def checkProgress(@RequestParam jobId: String): Mav = {
		jobService.getInstance(jobId) match {
			case Some(job: JobInstance) => Mav(new JSONView(Map(
				"progress" -> job.progress.toString,
				"status" -> job.status,
				"succeeded" -> job.succeeded
			)))
			case _ => throw new ItemNotFoundException()
		}

	}

	@RequestMapping(value = Array("/zip"), params = Array("jobId"))
	def serveZip(@RequestParam jobId: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		jobService.getInstance(jobId) match {
			case Some(job: JobInstance) =>
				val objectStoreKey = ZipCreator.objectKey(job.getString(ProfileExportCSVJob.ZipFilePathKey))

				objectStorageService.renderable(objectStoreKey, Some("tabula-profile-export.zip")) match {
					case Some(f) => fileServer.serve(f, Some("tabula-profile-export.zip"))
					case _ => throw new ItemNotFoundException()
				}
			case _ => throw new ItemNotFoundException()
		}
	}

}