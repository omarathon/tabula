package uk.ac.warwick.tabula.attendance.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.attendance.web.AttendanceBreadcrumbs
import uk.ac.warwick.tabula.data.model.{RuntimeMember, Member}
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent

/**
 * Base class for controllers in Attendance Monitoring.
 */
abstract class AttendanceController extends BaseController with AttendanceBreadcrumbs
	with CurrentMemberComponent with AutowiringProfileServiceComponent {

	final def optionalCurrentMember = profileService.getMemberByUserId(user.apparentId, disableFilter = true)
	final def currentMember = optionalCurrentMember getOrElse new RuntimeMember(user)

}

trait CurrentMemberComponent {
	def optionalCurrentMember: Option[Member]
	def currentMember: Member
}
