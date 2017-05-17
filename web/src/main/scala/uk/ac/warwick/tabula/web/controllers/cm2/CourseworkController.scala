package uk.ac.warwick.tabula.web.controllers.cm2

import uk.ac.warwick.tabula.data.model.{Member, RuntimeMember}
import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class CourseworkController extends BaseController {
	final def optionalCurrentMember: Option[Member] = user.profile
	final def currentMember: Member = optionalCurrentMember getOrElse new RuntimeMember(user)
}
