package uk.ac.warwick.tabula.api.web.controllers

import org.springframework.beans.factory.annotation.Value
import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class ApiController extends BaseController {
	@Value("${toplevel.url}") var toplevelUrl: String = _
}