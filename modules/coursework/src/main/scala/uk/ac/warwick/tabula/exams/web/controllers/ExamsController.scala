package uk.ac.warwick.tabula.exams.web.controllers

import uk.ac.warwick.tabula.data.model.{Member, RuntimeMember}
import uk.ac.warwick.tabula.exams.web.ExamsBreadcrumbs
import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class ExamsController extends BaseController with ExamsBreadcrumbs with CurrentMemberComponent {
	final def optionalCurrentMember = user.profile
	final def currentMember = optionalCurrentMember getOrElse new RuntimeMember(user)
}

trait CurrentMemberComponent {
	def optionalCurrentMember: Option[Member]
	def currentMember: Member
}