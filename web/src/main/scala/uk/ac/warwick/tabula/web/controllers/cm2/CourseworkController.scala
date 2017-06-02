package uk.ac.warwick.tabula.web.controllers.cm2

import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model.{Assignment, Member, RuntimeMember}
import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class CourseworkController extends BaseController with CourseworkBreadcrumbs {
	final def optionalCurrentMember: Option[Member] = user.profile
	final def currentMember: Member = optionalCurrentMember getOrElse new RuntimeMember(user)

	def mustBeCM2(assignment: Assignment): Assignment =
		if (!assignment.cm2Assignment) throw new ItemNotFoundException(assignment, "Tried to edit a CM1 assignment using the CM2 edit pages")
		else assignment
}
