package uk.ac.warwick.tabula.api.web.controllers

import uk.ac.warwick.tabula.AutowiringTopLevelUrlComponent
import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class ApiController
	extends BaseController
		with AutowiringTopLevelUrlComponent