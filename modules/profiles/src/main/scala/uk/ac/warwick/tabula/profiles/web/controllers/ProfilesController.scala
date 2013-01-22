package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.profiles.web.ProfileBreadcrumbs
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire

abstract class ProfilesController extends BaseController with ProfileBreadcrumbs {
	
	hideDeletedItems
	activeProfilesOnly
	studentProfilesOnly
	
	var profileService = Wire.auto[ProfileService]

	/**
	 * Enables the Hibernate filter for this session to exclude
	 * non-student entities.
	 */
	private var _studentProfilesOnly = false
	def studentProfilesOnly = { _studentProfilesOnly = true }
	def notStudentProfilesOnly = { _studentProfilesOnly = false }
	
	private var _activeProfilesOnly = false
	def activeProfilesOnly = { _activeProfilesOnly = true }
	def notActiveProfilesOnly = { _activeProfilesOnly = false }
	
	final override def onPreRequest {
		// if studentsOnly has been called, activate the studentsOnly filter
		if (_studentProfilesOnly) {
			session.enableFilter(Member.StudentsOnlyFilter)
		}
		
		if (_activeProfilesOnly) {
			session.enableFilter(Member.ActiveOnlyFilter)
		}
	}
	
	final def optionalCurrentMember = profileService.getMemberByUserId(user.apparentId, true)
	final def currentMember = optionalCurrentMember getOrElse(new Member(user))
	
}