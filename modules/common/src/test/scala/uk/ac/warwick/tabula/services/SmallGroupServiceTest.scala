package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.AutowiringSmallGroupDaoComponent
import uk.ac.warwick.userlookup.User

class SmallGroupServiceTest extends TestBase with Mockito {

	val smallGroupSetMembershipHelper = mock[UserGroupMembershipHelper[SmallGroupSet]]
	val service = new AbstractSmallGroupService
		with AutowiringSmallGroupDaoComponent // don't need any of these components, so having them autowire to null is fine
		with SmallGroupMembershipHelpers
	{
		val eventTutorsHelper: UserGroupMembershipHelper[SmallGroupEvent] = null
		val groupTutorsHelper: UserGroupMembershipHelper[SmallGroup] = null
		val groupSetMembersHelper: UserGroupMembershipHelper[SmallGroupSet] = smallGroupSetMembershipHelper
	}

	@Test
	def findSmallGroupSetsByMemberCallsMembershipHelper() {

		val user  = new User
		val groupSet = new SmallGroupSet()

		smallGroupSetMembershipHelper.findBy(user) returns  Seq(groupSet)
		service.findSmallGroupSetsByMember(user) should be(Seq(groupSet))
		there was one (smallGroupSetMembershipHelper).findBy(user)
	}
}
