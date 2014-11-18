package uk.ac.warwick.tabula.reports.web.controllers

import uk.ac.warwick.tabula.data.model.{Member, RuntimeMember}
import uk.ac.warwick.tabula.reports.web.ReportsBreadcrumbs
import uk.ac.warwick.tabula.web.controllers.BaseController

/**
 * Base class for controllers in Reports.
 */
abstract class ReportsController extends BaseController with ReportsBreadcrumbs with CurrentMemberComponent {

	final def optionalCurrentMember = user.profile
	final def currentMember = optionalCurrentMember getOrElse new RuntimeMember(user)

}

trait CurrentMemberComponent {
	def optionalCurrentMember: Option[Member]
	def currentMember: Member
}
