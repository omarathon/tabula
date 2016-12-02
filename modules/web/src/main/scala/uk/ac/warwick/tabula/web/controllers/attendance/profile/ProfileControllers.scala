package uk.ac.warwick.tabula.web.controllers.attendance.profile

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.profile.{AttendanceProfileCommand, AttendanceProfileCommandResult}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}

@Controller
@RequestMapping(value = Array("/attendance/profile"))
class ProfileHomeController extends AttendanceController with AutowiringTermServiceComponent {

	@RequestMapping
	def render(): Mav = user.profile match {
		case Some(student: StudentMember) =>
			Redirect(Routes.Profile.profileForYear(student, AcademicYear.findAcademicYearContainingDate(DateTime.now)))
		case _ if user.isStaff =>
			Mav("attendance/profile/staff").noLayoutIf(ajax)
		case _ =>
			Mav("attendance/profile/unknown").noLayoutIf(ajax)
	}
}

@Controller
@RequestMapping(value = Array("/attendance/profile/{student}"))
class ProfileChooseYearController extends AttendanceController {

	@RequestMapping
	def render(@PathVariable student: StudentMember): Mav =
		Mav("attendance/profile/years",
			"years" ->
				student.freshOrStaleStudentCourseDetails
					.flatMap(_.freshOrStaleStudentCourseYearDetails)
					.map(_.academicYear.startYear)
		).noLayoutIf(ajax)
}

@Controller
@RequestMapping(Array("/attendance/profile/{student}/{academicYear}"))
class ProfileController extends AttendanceController with HasMonthNames {

	@ModelAttribute("command")
	def command(@PathVariable student: StudentMember, @PathVariable academicYear: AcademicYear) =
		AttendanceProfileCommand(mandatory(student), mandatory(academicYear))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[AttendanceProfileCommandResult],
		@PathVariable student: StudentMember,
		@PathVariable academicYear: AcademicYear,
		@RequestParam(value="expand", required=false) expand: JBoolean
	): Mav = {
		val commandResult = cmd.apply()
		val groupedPointMap = commandResult.attendanceMonitoringPointWithCheckPoint


		val allNotes = commandResult.allNotesWithSomeCheckPoints
		val checkPointNotesMap = commandResult.checkPointNotes
		val unrecordedNotes = commandResult.notesWithoutCheckPoints
		val missedPointCountByTerm = groupedPointMap.map{ case(period, pointCheckpointPairs) =>
			period -> pointCheckpointPairs.count{ case(point, checkpoint) => checkpoint != null && checkpoint.state == AttendanceState.MissedUnauthorised}
		}
		val modelMap = Map(
			"groupedPointMap" -> groupedPointMap,
			"missedPointCountByTerm" -> missedPointCountByTerm,
			"hasAnyMissed" -> missedPointCountByTerm.exists(_._2 > 0),
			"department" -> currentMember.homeDepartment,
			"is_the_student" -> (user.apparentId == student.userId),
			"expand" -> expand,
			"allNotes" -> allNotes,
			"checkPointNotesMap" -> checkPointNotesMap,
			"unrecordedNotes" -> unrecordedNotes,
			"allCheckpointStates" -> AttendanceState.values.sortBy(state => state.description) ,
			"returnTo" -> getReturnTo(Routes.Profile.profileForYear(mandatory(student), mandatory(academicYear)))
		)
		if (ajax)
			Mav("attendance/profile/_profile", modelMap).noLayout()
		else
			Mav("attendance/profile/profile", modelMap).crumbs(
				Breadcrumbs.Profile.Years(mandatory(student), user.apparentId == student.userId)
			)
	}
}
