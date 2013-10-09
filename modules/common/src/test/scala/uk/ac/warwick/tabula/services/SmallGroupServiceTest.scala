package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.AutowiringSmallGroupDaoComponent
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.AutowiringAssignmentMembershipDaoComponent
import uk.ac.warwick.tabula.data.AutowiringUserGroupDaoComponent
import uk.ac.warwick.tabula.helpers.Logging

class SmallGroupServiceTest extends TestBase with Mockito {
	val studentGroupMembershipHelper = mock[UserGroupMembershipHelper[SmallGroup]]
	val service = new AbstractSmallGroupService
		with AutowiringSmallGroupDaoComponent // don't need any of these components, so having them autowire to null is fine
		with AutowiringAssignmentMembershipDaoComponent
		with SmallGroupMembershipHelpers
		with AutowiringUserGroupDaoComponent
		with AutowiringUserLookupComponent
		with Logging
	{
		val eventTutorsHelper: UserGroupMembershipHelper[SmallGroupEvent] = null
		val groupTutorsHelper: UserGroupMembershipHelper[SmallGroup] = null
		val studentGroupHelper: UserGroupMembershipHelper[SmallGroup] = studentGroupMembershipHelper
	}

	@Test
	def findSmallGroupsByMemberCallsMembershipHelper() {

		val user  = new User
		val group = new SmallGroup()

		studentGroupMembershipHelper.findBy(user) returns  Seq(group)
		service.findSmallGroupsByStudent(user) should be(Seq(group))
		there was one (studentGroupMembershipHelper).findBy(user)
	}
}
