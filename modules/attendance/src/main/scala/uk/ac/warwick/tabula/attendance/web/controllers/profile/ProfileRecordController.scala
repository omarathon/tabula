package uk.ac.warwick.tabula.attendance.web.controllers.profile

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.{GroupsPoints, StudentRecordCommand}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.attendance.web.controllers.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringNote, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.services.{AttendanceMonitoringService, AutowiringTermServiceComponent}

@Controller
@RequestMapping(Array("/profile/{student}/{academicYear}/record"))
class ProfileRecordController extends AttendanceController
	with HasMonthNames with GroupsPoints with AutowiringTermServiceComponent {

	@Autowired var attendanceMonitoringService: AttendanceMonitoringService = _

	validatesSelf[SelfValidating]

	var points: Seq[AttendanceMonitoringPoint] = _

	@InitBinder // do on each request
	def populatePoints(@PathVariable academicYear: AcademicYear, @PathVariable student: StudentMember) = {
			points = attendanceMonitoringService.listStudentsPoints(mandatory(student), None, mandatory(academicYear))
	}

	@ModelAttribute("command")
	def command(
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	) =
		StudentRecordCommand(mandatory(academicYear), mandatory(student), user)

	@ModelAttribute("attendanceNotes")
	def attendanceNotes(@PathVariable student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringNote] =
		attendanceMonitoringService.getAttendanceNoteMap(mandatory(student))

	@ModelAttribute("groupedPointMap")
	def groupedPointMap(@PathVariable student: StudentMember): Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]] = {
		val checkpointMap = attendanceMonitoringService.getCheckpoints(points, mandatory(student))
		val groupedPoints = groupByTerm(points, groupSimilar = false) ++
			groupByMonth(points, groupSimilar = false)
		groupedPoints.map{case(period, thesePoints) =>
			period -> thesePoints.map{ groupedPoint =>
				groupedPoint.templatePoint -> checkpointMap.getOrElse(groupedPoint.templatePoint, null)
			}
		}
	}

	@ModelAttribute("reportedPointMap")
	def reportedPointMap(@PathVariable academicYear: AcademicYear, @PathVariable student: StudentMember): Map[AttendanceMonitoringPoint, Boolean] = {
		val nonReportedTerms = attendanceMonitoringService.findNonReportedTerms(Seq(mandatory(student)), mandatory(academicYear))
		points.map{ point => point ->
			!nonReportedTerms.contains(termService.getTermFromDateIncludingVacations(point.startDate.toDateTimeAtStartOfDay).getTermTypeAsString)
		}.toMap
	}

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringCheckpoint]] with PopulateOnForm,
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	) = {
		cmd.populate()
		render(academicYear, student)
	}

	private def render(academicYear: AcademicYear, student: StudentMember) = {
		Mav("record",
			"department" -> currentMember.homeDepartment,
			"returnTo" -> getReturnTo(Routes.Profile.profileForYear(mandatory(student), mandatory(academicYear)))
		).crumbs(
			Breadcrumbs.Profile.Years(mandatory(student), user.apparentId == student.userId),
			Breadcrumbs.Profile.ProfileForYear(mandatory(student), mandatory(academicYear))
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringCheckpoint]] with PopulateOnForm,
		errors: Errors,
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	) = {
		if (errors.hasErrors) {
			render(academicYear, student)
		} else {
			cmd.apply()
			Redirect(Routes.Profile.profileForYear(mandatory(student), mandatory(academicYear)))
		}
	}

}
