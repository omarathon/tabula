package uk.ac.warwick.tabula.permissions

import uk.ac.warwick.tabula.{CurrentUser, TestBase}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.services.permissions.SmallGroupSetMemberRoleProvider
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.roles.SmallGroupSetMember

class SmallGroupSetMemberRoleProviderTest extends TestBase {

	private trait Fixture{
		val groupSet = new SmallGroupSet
		groupSet.id= "test"
		groupSet.members = new UserGroup
		groupSet.members.addUser("test")

		val memberUser = new User
		memberUser.setUserId("test")
		val member = new CurrentUser(memberUser,memberUser)

		val nonMemberUser = new User
		nonMemberUser.setUserId("test2")
		val nonMember = new CurrentUser(nonMemberUser,nonMemberUser)

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
