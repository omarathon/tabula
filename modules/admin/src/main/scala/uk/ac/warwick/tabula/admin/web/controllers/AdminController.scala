package uk.ac.warwick.tabula.admin.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.admin.web.AdminBreadcrumbs
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.web.Mav

abstract class AdminController extends BaseController with AdminBreadcrumbs {
	
	hideDeletedItems
	
	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, dept:Department):Mav = mav.crumbs(Breadcrumbs.Department(dept))

	final override def onPreRequest {

	}
	
}