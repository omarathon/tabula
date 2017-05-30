package uk.ac.warwick.tabula.web.controllers.coursework

import uk.ac.warwick.tabula.data.model.{Member, RuntimeMember}
import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class OldCourseworkController extends BaseController with CourseworkBreadcrumbs with CurrentMemberComponent {
	final def optionalCurrentMember: Option[Member] = user.profile
	final def currentMember: Member = optionalCurrentMember.getOrElse(new RuntimeMember(user))
}

trait CurrentMemberComponent {
	def optionalCurrentMember: Option[Member]
	def currentMember: Member
}