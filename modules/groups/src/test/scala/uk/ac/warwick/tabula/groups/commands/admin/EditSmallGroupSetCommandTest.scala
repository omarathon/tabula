package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{UnspecifiedTypeUserGroup, Department, Module, UserGroup}
import uk.ac.warwick.tabula.services.{UserGroupCacheManager, AssignmentMembershipService, MaintenanceModeServiceImpl, SmallGroupService}
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.MockUserLookup

class EditSmallGroupSetCommandTest extends TestBase with Mockito {
	
	@Test
	def autoDeregister() {
		val (user1, user2, user3, user4, user5) = (new User("user1"), new User("user2"), new User("user3"), new User("user4"), new User("user5"))
		
		val mockUserLookup = new MockUserLookup
		mockUserLookup.registerUserObjects(user1, user2, user3, user4, user5)

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = mockUserLookup
		}
		
		val dept = new Department()
		val module = new Module()
		module.department = dept
		
		val set = new SmallGroupSet()
		set.members = UserGroup.ofUsercodes
		set.membershipService = mock[AssignmentMembershipService]
		set.module = module
		
		set.members.add(user1)
		set.members.add(user2)
		set.members.add(user3)
		set.members.add(user4)
		
		val group1 = new SmallGroup()
		group1.students = UserGroup.ofUsercodes
		group1.groupSet = set
		group1.students.add(user1)
		group1.students.add(user2)
		
		val group2 = new SmallGroup()
		group2.students = UserGroup.ofUsercodes
		group2.groupSet = set
		group2.students.add(user3)
		group2.students.add(user4)
		group2.students.add(user5)
		
		set.groups.add(group1)
		set.groups.add(group2)

		wireUserLookup(set.members)
		wireUserLookup(group1.students)
		wireUserLookup(group2.students)
		
		EventHandling.enabled = false
		
		val cmd = new EditSmallGroupSetCommand(set, new User("me"))
		cmd.service = mock[SmallGroupService]
		cmd.groups.asScala.foreach { cmd => cmd.maintenanceMode = new MaintenanceModeServiceImpl() }
		
		cmd.membershipService = mock[AssignmentMembershipService] // Intentionally different to the SmallGroupSet's one
		cmd.membershipService.determineMembershipUsers(Seq(), Some(set.members)) returns (set.members.users)
		cmd.membershipService.determineMembershipUsers(Seq(), Some(cmd.members)) returns (cmd.members.users)
		cmd.applyInternal()
		
		set.members.users.toSet should be (Set(user1, user2, user3, user4))
		group1.students.users.toSet should be (Set(user1, user2))
		group2.students.users.toSet should be (Set(user3, user4, user5))
		
		there was one (cmd.service).saveOrUpdate(set)
		
		cmd.members.remove(user2)
		cmd.members.remove(user3)
		cmd.members.remove(user4)
		cmd.members.add(user5)
		
		cmd.membershipService = mock[AssignmentMembershipService] // Intentionally different to the SmallGroupSet's one
		cmd.membershipService.determineMembershipUsers(Seq(), Some(set.members)) returns (set.members.users)
		cmd.membershipService.determineMembershipUsers(Seq(), Some(cmd.members)) returns (cmd.members.users)
		cmd.applyInternal()
		
		set.members.users.toSet should be (Set(user1, user5))
		group1.students.users.toSet should be (Set(user1))
		group2.students.users.toSet should be (Set(user5))
		
		// Two now, because it includes the one from before
		there were two (cmd.service).saveOrUpdate(set)
	}

}