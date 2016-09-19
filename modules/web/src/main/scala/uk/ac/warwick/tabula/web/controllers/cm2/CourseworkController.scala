package uk.ac.warwick.tabula.web.controllers.cm2

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.RuntimeMember
import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class CourseworkController extends BaseController {

	final def optionalCurrentMember = user.profile
	final def currentMember = optionalCurrentMember getOrElse(new RuntimeMember(user))

	final val urlPrefix: String = Wire.property("${cm2.prefix}")

}
