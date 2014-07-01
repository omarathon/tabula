package uk.ac.warwick.tabula.attendance.web.controllers.profile

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.profile.AttendanceProfileCommand
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.attendance.web.controllers.{HasMonthNames, AttendanceController}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.JavaImports._

@Controller
@RequestMapping(value = Array("/profile"))
class ProfileHomeController extends AttendanceController {

	@RequestMapping
	def render() = user.profile match {
		case Some(student: StudentMember) =>
			Redirect(Routes.Profile.profileForYear(student, AcademicYear.guessByDate(DateTime.now)))
		case _ if user.isStaff =>
			Mav("profile/staff").noLayoutIf(ajax)
		case _ =>
			Mav("profile/unknown").noLayoutIf(ajax)
	}
}

@Controller
@RequestMapping(value = Array("/profile/{student}"))
class ProfileChooseYearController extends AttendanceController {

	@RequestMapping
	def render(@PathVariable student: StudentMember) =
		Mav("profile/years").noLayoutIf(ajax)
}

@Controller
@RequestMapping(Array("/profile/{student}/{academicYear}"))
class ProfileController extends AttendanceController with HasMonthNames {

	@ModelAttribute("command")
	def command(@PathVariable student: StudentMember, @PathVariable academicYear: AcademicYear) =
		AttendanceProfileCommand(mandatory(student), mandatory(academicYear))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]]],
		@PathVariable student: StudentMember,
		@RequestParam(value="expand", required=false) expand: JBoolean
	) = {
		val groupedPointMap = cmd.apply()
		val missedPointCountByTerm = groupedPointMap.map{ case(period, pointCheckpointPairs) =>
			period -> pointCheckpointPairs.count{ case(point, checkpoint) => checkpoint != null && checkpoint.state == AttendanceState.MissedUnauthorised}
		}
		val modelMap = Map(
			"groupedPointMap" -> groupedPointMap,
			"missedPointCountByTerm" -> missedPointCountByTerm,
			"hasAnyMissed" -> missedPointCountByTerm.exists(_._2 > 0),
			"department" -> currentMember.homeDepartment,
			"is_the_student" -> (user.apparentId == student.userId),
			"expand" -> expand
		)
		if (ajax)
			Mav("profile/_profile", modelMap).noLayout()
		else
			Mav("profile/profile", modelMap).crumbs(
				Breadcrumbs.Profile.Years(mandatory(student))
			)
	}
}
