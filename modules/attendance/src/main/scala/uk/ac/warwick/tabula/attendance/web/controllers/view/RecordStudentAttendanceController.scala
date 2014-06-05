package uk.ac.warwick.tabula.attendance.web.controllers.view

import uk.ac.warwick.tabula.attendance.web.controllers.{HasMonthNames, AttendanceController}
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{SelfValidating, PopulateOnForm, Appliable}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringNote, AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint}
import uk.ac.warwick.tabula.attendance.commands.view.RecordStudentAttendanceCommand
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, AttendanceMonitoringService}
import uk.ac.warwick.tabula.attendance.commands.GroupsPoints
import uk.ac.warwick.tabula.attendance.web.Routes
import org.springframework.stereotype.Controller
import javax.validation.Valid
import org.springframework.validation.Errors

@Controller
@RequestMapping(Array("/view/{department}/{academicYear}/students/{student}/record"))
class RecordStudentAttendanceController extends AttendanceController
	with HasMonthNames with GroupsPoints with AutowiringTermServiceComponent {

	@Autowired var attendanceMonitoringService: AttendanceMonitoringService = _

	validatesSelf[SelfValidating]

	var points: Seq[AttendanceMonitoringPoint] = _

	@InitBinder // do on each request
	def populatePoints(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable student: StudentMember) = {
			points = attendanceMonitoringService.listStudentsPoints(student, department, academicYear)
	}

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable student: StudentMember) =
		RecordStudentAttendanceCommand(department, academicYear, student, user)

	@ModelAttribute("attendanceNotes")
	def attendanceNotes(@PathVariable student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringNote] =
		attendanceMonitoringService.getAttendanceNoteMap(student)

	@ModelAttribute("groupedPointMap")
	def groupedPointMap(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	): Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]] = {
		val checkpointMap = attendanceMonitoringService.getCheckpoints(points, student)
		val groupedPoints = groupByTerm(points, groupSimilar = false) ++
			groupByMonth(points, groupSimilar = false)
		groupedPoints.map{case(period, thesePoints) =>
			period -> thesePoints.map{ groupedPoint =>
				groupedPoint.templatePoint -> checkpointMap.get(groupedPoint.templatePoint).getOrElse(null)
			}
		}
	}

	@ModelAttribute("reportedPointMap")
	def reportedPointMap(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	): Map[AttendanceMonitoringPoint, Boolean] = {
		val nonReportedTerms = attendanceMonitoringService.findNonReportedTerms(Seq(student), academicYear)
		points.map{ point => point ->
			!nonReportedTerms.contains(termService.getTermFromDateIncludingVacations(point.startDate.toDateTimeAtStartOfDay).getTermTypeAsString)
		}.toMap
	}

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringCheckpoint]] with PopulateOnForm,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	) = {
		cmd.populate()
		Mav("view/studentrecord",
			"returnTo" -> getReturnTo(Routes.View.student(department, academicYear, student))
		).crumbs(
			Breadcrumbs.View.Home,
			Breadcrumbs.View.Department(department),
			Breadcrumbs.View.DepartmentForYear(department, academicYear),
			Breadcrumbs.View.Students(department, academicYear),
			Breadcrumbs.View.Student(department, academicYear, student)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringCheckpoint]] with PopulateOnForm,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	) = {
		if (errors.hasErrors) {
			form(cmd, department, academicYear, student)
		} else {
			cmd.apply()
			Redirect(Routes.View.student(department, academicYear, student))
		}
	}

}
