package uk.ac.warwick.tabula.web.controllers.profiles.profile

import java.text.DateFormatSymbols

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.groups.ListStudentGroupAttendanceCommand
import uk.ac.warwick.tabula.commands.profiles.attendance.AttendanceMonitoringPointProfileCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat.Example
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs

@Controller
@RequestMapping(Array("/profiles/view"))
class ViewProfileAttendanceController extends AbstractViewProfileController {

	@RequestMapping(Array("/{member}/attendance"))
	def viewByMemberMapping(
		@PathVariable member: Member,
		@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]
	): Mav = {
		mandatory(member) match {
			case student: StudentMember if student.mostSignificantCourseDetails.isDefined =>
				viewByCourse(student.mostSignificantCourseDetails.get, activeAcademicYear)
			case _ =>
				Redirect(Routes.Profile.identity(member))
		}
	}

	@RequestMapping(Array("/course/{studentCourseDetails}/{academicYear}/attendance"))
	def viewByCourseMapping(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val activeAcademicYear: Option[AcademicYear] = Some(mandatory(academicYear))
		viewByCourse(studentCourseDetails, activeAcademicYear)
	}

	private def viewByCourse(
		studentCourseDetails: StudentCourseDetails,
		activeAcademicYear: Option[AcademicYear]
	): Mav = {
		val student = studentCourseDetails.student
		val monitoringPointAttendanceCommandResult = AttendanceMonitoringPointProfileCommand(mandatory(student), mandatory(activeAcademicYear)).apply()
		val seminarAttendanceCommandResult = ListStudentGroupAttendanceCommand(mandatory(student), mandatory(activeAcademicYear)).apply();
		val groupedPointMap = monitoringPointAttendanceCommandResult.monitoringPointAttendanceWithCheckPoint
		val missedPointCountByTerm = groupedPointMap.map{ case(period, pointCheckpointPairs) =>
			period -> pointCheckpointPairs.count{ case(point, checkpoint) => checkpoint != null && checkpoint.state == AttendanceState.MissedUnauthorised}
		}

		Mav("profiles/profile/attendance_student",
			"student" -> student,
			"groupedPointMap" -> groupedPointMap,
			"missedPointCountByTerm" -> missedPointCountByTerm,
			"hasAnyMissed" -> missedPointCountByTerm.exists(_._2 > 0),
			"is_the_student" -> (user.apparentId == student.userId),
			"allNotes" -> monitoringPointAttendanceCommandResult.allNotesWithSomeCheckPoints,
			"checkPointNotesMap" -> monitoringPointAttendanceCommandResult.checkPointNotes,
			"unrecordedNotes" -> monitoringPointAttendanceCommandResult.notesWithoutCheckPoints,
			"allCheckpointStates" -> AttendanceState.values.sortBy(state => state.description),
			"monthNames" -> monthNames(activeAcademicYear.get),
			"hasGroups" -> seminarAttendanceCommandResult.attendance.values.nonEmpty,
			"title" -> groupTitle(seminarAttendanceCommandResult.attendance),
			"terms" -> seminarAttendanceCommandResult.attendance,
			"attendanceNotes" -> seminarAttendanceCommandResult.notes,
			"missedCount" -> seminarAttendanceCommandResult.missedCount,
			"missedCountByTerm" -> seminarAttendanceCommandResult.missedCountByTerm,
			"termWeeks" -> seminarAttendanceCommandResult.termWeeks,
			"academicYear" -> activeAcademicYear

		).crumbs(breadcrumbsStudent(activeAcademicYear, studentCourseDetails, ProfileBreadcrumbs.Profile.AttendanceIdentifier): _*)
			.secondCrumbs(secondBreadcrumbs(activeAcademicYear, studentCourseDetails)(scyd => Routes.Profile.attendance(scyd)): _*)

	}

	private def monthNames(academicYear: AcademicYear) = {
		val monthNames = new DateFormatSymbols().getMonths.array
		(8 to 11).map{ i => monthNames(i) + " " + academicYear.startYear.toString } ++
			(0 to 9).map{ i => monthNames(i) + " " + academicYear.endYear.toString }
	}

	private def groupTitle( attendance: ListStudentGroupAttendanceCommand.PerTermAttendance) = {
		val title = {
			val smallGroupSets = attendance.values.toSeq.flatMap(_.keys.map(_.groupSet))

			val formats = smallGroupSets.map(_.format.description).distinct
			val pluralisedFormats = formats.map {
				case s:String if s == Example.description => s + "es"
				case s:String => s + "s"
				case _ =>
			}
			pluralisedFormats.mkString(", ")
		}
		title
	}
}
