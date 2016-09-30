package uk.ac.warwick.tabula.web.controllers.cm2

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class CourseworkController extends BaseController {

	final val urlPrefix: String = Wire.property("${cm2.prefix}")

}
