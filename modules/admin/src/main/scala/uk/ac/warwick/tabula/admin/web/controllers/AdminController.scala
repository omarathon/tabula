package uk.ac.warwick.tabula.admin.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.admin.web.AdminBreadcrumbs
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire

abstract class AdminController extends BaseController with AdminBreadcrumbs {
	
	hideDeletedItems

	final override def onPreRequest {

	}
	
}