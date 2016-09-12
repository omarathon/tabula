package uk.ac.warwick.tabula.web.controllers.coursework

import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.data.model.{Member, RuntimeMember}
import uk.ac.warwick.tabula.helpers.EnvironmentAwareness

abstract class OldCourseworkController extends BaseController with CourseworkBreadcrumbs with CurrentMemberComponent
with EnvironmentAwareness {
	final def optionalCurrentMember = user.profile
	final def currentMember = optionalCurrentMember getOrElse(new RuntimeMember(user))
	val courseworkPrefix = "/coursework"
}

trait CurrentMemberComponent {
	def optionalCurrentMember: Option[Member]
	def currentMember: Member
}