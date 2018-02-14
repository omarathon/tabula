package uk.ac.warwick.tabula.web.controllers.exams.grids

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.{StudentAssessmentCommand, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.{BreadCrumb, Mav, Routes, Breadcrumbs => BaseBreadcumbs}


@Controller
@RequestMapping(Array("/exams/grids/{academicYear}/{studentCourseDetails}/assessmentdetails"))
class StudentAssessmentBreakdownController extends ExamsController
	with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent with AutowiringJobServiceComponent
	with AutowiringCourseAndRouteServiceComponent with AutowiringModuleRegistrationServiceComponent
	with TaskBenchmarking {


	type CommandType = Appliable[Seq[GridAssessmentComponentDetails]] with StudentAssessmentCommandState

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))


	protected def fetchScyd(scd: StudentCourseDetails, gridAcademicYear: AcademicYear): StudentCourseYearDetails = {
		val scyds = scd.freshStudentCourseYearDetails match {
			case Nil =>
				scd.freshOrStaleStudentCourseYearDetails
			case fresh =>
				fresh
		}
		scyds.find(_.academicYear == gridAcademicYear) match {
			case Some(scyd) => scyd
			case _ => throw new UnsupportedOperationException("Not valid StudentCourseYearDetails for given academic year ")
		}
	}


	@ModelAttribute("command")
	def command(@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear): CommandType = {
		StudentAssessmentCommand(fetchScyd(studentCourseDetails, academicYear), mandatory(academicYear))
	}

	@ModelAttribute("weightings")
	def weightings(@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear): IndexedSeq[CourseYearWeighting] = {
		(1 to FilterStudentsOrRelationships.MaxYearsOfStudy).flatMap(year =>
			courseAndRouteService.getCourseYearWeighting(mandatory(studentCourseDetails).course.code, mandatory(academicYear), year)
		).sorted
	}

	@RequestMapping()
	def viewStudentAssessmentDetails(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear,
		@ModelAttribute("command") cmd: CommandType
	): Mav = {
		Mav("exams/grids/generate/studentAssessmentComponentDetails",
			"assessmentComponents" -> cmd.apply(),
			"member" -> studentCourseDetails.student
		).crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(mandatory(cmd.studentCourseYearDetails.enrolmentDepartment), mandatory(academicYear)))
			.secondCrumbs(secondBreadcrumbs(academicYear, studentCourseDetails)(scyd => Routes.exams.Grids.assessmentdetails(scyd)): _*)

	}

	def secondBreadcrumbs(activeAcademicYear: AcademicYear, scd: StudentCourseDetails)(urlGenerator: (StudentCourseYearDetails) => String): Seq[BreadCrumb] = {
		val chooseScyd = fetchScyd(scd, activeAcademicYear)
		val scyds = scd.student.freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails) match {
			case Nil =>
				scd.student.freshOrStaleStudentCourseDetails.flatMap(_.freshOrStaleStudentCourseYearDetails)
			case fresh =>
				fresh
		}
		scyds.map(scyd =>
			BaseBreadcumbs.Standard(
				title = "%s %s".format(scyd.studentCourseDetails.course.code, scyd.academicYear.getLabel),
				url = Some(urlGenerator(scyd)),
				tooltip = "%s %s".format(
					scyd.studentCourseDetails.course.name,
					scyd.academicYear.getLabel
				)
			).setActive(scyd == chooseScyd)
		).toSeq
	}
}
