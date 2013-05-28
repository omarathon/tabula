package uk.ac.warwick.tabula.groups.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.groups.web.GroupsBreadcrumbs
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire

abstract class GroupsController extends BaseController with GroupsBreadcrumbs {
	
	hideDeletedItems

	final override def onPreRequest {

	}
	
}