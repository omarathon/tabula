package uk.ac.warwick.tabula.web.controllers.admin

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class AdminController extends BaseController with AdminBreadcrumbs {

	hideDeletedItems

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, dept:Department):Mav = mav.crumbs(Breadcrumbs.Department(dept))

	final override def onPreRequest {

	}

}