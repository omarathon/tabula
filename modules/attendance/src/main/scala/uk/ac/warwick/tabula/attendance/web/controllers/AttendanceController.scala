package uk.ac.warwick.tabula.attendance.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.attendance.web.AttendanceBreadcrumbs
import uk.ac.warwick.tabula.data.model.{RuntimeMember, Member}
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.AcademicYear
import java.text.DateFormatSymbols

/**
 * Base class for controllers in Attendance Monitoring.
 */
abstract class AttendanceController extends BaseController with AttendanceBreadcrumbs
	with CurrentMemberComponent {

	final def optionalCurrentMember = user.profile
	final def currentMember = optionalCurrentMember getOrElse new RuntimeMember(user)

}

trait CurrentMemberComponent {
	def optionalCurrentMember: Option[Member]
	def currentMember: Member
}

trait HasMonthNames {

	@ModelAttribute("monthNames")
	def monthNames(@PathVariable academicYear: AcademicYear) = {
		val monthNames = new DateFormatSymbols().getMonths.array
		(8 to 11).map{ i => monthNames(i) + " " + academicYear.startYear.toString } ++
			(0 to 9).map{ i => monthNames(i) + " " + academicYear.endYear.toString }
	}

}
