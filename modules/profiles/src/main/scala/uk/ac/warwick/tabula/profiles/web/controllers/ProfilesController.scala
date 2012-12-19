package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.profiles.web.ProfileBreadcrumbs
import uk.ac.warwick.tabula.data.model.Member

abstract class ProfilesController extends BaseController with ProfileBreadcrumbs {

	/**
	 * Enables the Hibernate filter for this session to exclude
	 * non-student entities.
	 */
	private var _studentProfilesOnly = false
	def studentProfilesOnly = { _studentProfilesOnly = true }
	def notStudentProfilesOnly = { _studentProfilesOnly = false }
	
	final override def onPreRequest {
		// if studentsOnly has been called, activate the studentsOnly filter
		if (_studentProfilesOnly) {
			session.enableFilter(Member.StudentsOnlyFilter)
		}
	}
	
}