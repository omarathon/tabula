package uk.ac.warwick.tabula.permissions

import uk.ac.warwick.tabula.{Mockito, CurrentUser, TestBase}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.services.permissions.SmallGroupSetMemberRoleProvider
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.roles.SmallGroupSetMember
import uk.ac.warwick.tabula.services.UserLookupService

class SmallGroupSetMemberRoleProviderTest extends TestBase with Mockito{

	private trait Fixture{
		val userLookup = mock[UserLookupService]
		val groupSet = new SmallGroupSet
		groupSet._membersGroup.userLookup = mock[UserLookupService]
		groupSet.id= "test"

		val memberUser = new User
		memberUser.setWarwickId("test")
		userLookup.getUserByWarwickUniId("test") returns memberUser

		val member = new CurrentUser(memberUser,memberUser)
		groupSet.members.add(memberUser)


		val nonMemberUser = new User
		nonMemberUser.setWarwickId("test2")
		val nonMember = new CurrentUser(nonMemberUser,nonMemberUser)
		userLookup.getUserByWarwickUniId("test2") returns nonMemberUser

		val roleProvider = new SmallGroupSetMemberRoleProvider
	}

	@Test
	def membersOfAGroupsetGetTheGroupsetMemberRole(){new Fixture {
		roleProvider.getRolesFor(member,groupSet) should be(Seq(SmallGroupSetMember(groupSet)))
	}}

	@Test
	def nonMembersDontGetTheGroupsetMemberRole(){new Fixture {
		roleProvider.getRolesFor(nonMember,groupSet) should be(Stream.Empty)
	}}


}
