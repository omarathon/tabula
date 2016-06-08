package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.{ListStudentGroupAttendanceCommand, StudentGroupAttendance}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat.Example
import uk.ac.warwick.tabula.web.Mav

/**
 * Displays a student's attendance at small groups.
 */
@Controller
@RequestMapping(Array("/groups/student/{member}/attendance/{academicYear}"))
class StudentGroupAttendanceController extends GroupsController {

	@ModelAttribute("command") def command(@PathVariable member: Member, @PathVariable academicYear: AcademicYear) =
		ListStudentGroupAttendanceCommand(member, academicYear)

	@RequestMapping
	def showAttendance(@ModelAttribute("command") cmd: Appliable[StudentGroupAttendance], @PathVariable member: Member): Mav = {
		val info = cmd.apply()

		val hasGroups = info.attendance.values.nonEmpty

		val title = {
			val smallGroupSets = info.attendance.values.toSeq.flatMap(_.keys.map(_.groupSet))

			val formats = smallGroupSets.map(_.format.description).distinct
			val pluralisedFormats = formats.map {
				case s:String if s == Example.description => s + "es"
				case s:String => s + "s"
				case _ =>
			}
			pluralisedFormats.mkString(", ")
		}

		Mav("groups/students_group_attendance",
			"hasGroups" -> hasGroups,
			"title" -> title,
			"terms" -> info.attendance,
			"attendanceNotes" -> info.notes,
			"missedCount" -> info.missedCount,
			"missedCountByTerm" -> info.missedCountByTerm,
			"termWeeks" -> info.termWeeks,
			"student" -> member,
			"defaultExpand" -> !ajax
		).noLayoutIf(ajax)
	}

}
