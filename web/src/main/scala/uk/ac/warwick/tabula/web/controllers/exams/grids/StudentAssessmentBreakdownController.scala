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
import uk.ac.warwick.tabula.web.{Mav, Routes}

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


	//validatesSelf[SelfValidating]


	protected def fetchScyd(scd: StudentCourseDetails, academicYear: AcademicYear): StudentCourseYearDetails = {
		val scyds = scd.freshStudentCourseYearDetails match {
			case Nil =>
				scd.freshOrStaleStudentCourseYearDetails
			case fresh =>
				fresh
		}
		scyds.find(_.academicYear == academicYear) match {
			case Some(scyd) => scyd
			case None => throw new UnsupportedOperationException("Not a Option[StudentCourseYearDetails]")
		}
	}


	@ModelAttribute("command")
	def command(@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear): CommandType = {
		StudentAssessmentCommand(mandatory(fetchScyd(studentCourseDetails, academicYear)), academicYear)
	}

	@ModelAttribute("weightings")
	def weightings(@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear): IndexedSeq[CourseYearWeighting] = {
		(1 to FilterStudentsOrRelationships.MaxYearsOfStudy).flatMap(year =>
			courseAndRouteService.getCourseYearWeighting(studentCourseDetails.course.code, academicYear, year)
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
		).crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(studentCourseDetails.department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.exams.Grids.generate(studentCourseDetails.department, year)): _*)
	}

}
