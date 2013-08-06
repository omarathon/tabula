package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import scala.collection.JavaConverters._

class AllocateStudentsToGroupsCommandTest extends TestBase with Mockito {
	
	val service = mock[SmallGroupService]
	val membershipService = mock[AssignmentMembershipService]	
	val userLookup = new MockUserLookup
	
	@Test def itWorks = withUser("boombastic") {
		val module = Fixtures.module("in101", "Introduction to Scala")
		val set = Fixtures.smallGroupSet("My small groups")
		set.module = module
		set.membershipService = membershipService
		set.members.userLookup = userLookup
		
		val user1 = new User("cuscav")
		user1.setFoundUser(true)
		user1.setFirstName("Mathew")
		user1.setLastName("Mannion")
		user1.setWarwickId("0672089")
		
		val user2 = new User("cusebr")
		user2.setFoundUser(true)
		user2.setFirstName("Nick")
		user2.setLastName("Howes")
		user2.setWarwickId("0672088")
		
		val user3 = new User("cusfal")
		user3.setFoundUser(true)
		user3.setFirstName("Matthew")
		user3.setLastName("Jones")
		user3.setWarwickId("9293883")
		
		val user4 = new User("curef")
		user4.setFoundUser(true)
		user4.setFirstName("John")
		user4.setLastName("Dale")
		user4.setWarwickId("0200202")
		
		val user5 = new User("cusmab")
		user5.setFoundUser(true)
		user5.setFirstName("Steven")
		user5.setLastName("Carpenter")
		user5.setWarwickId("8888888")
		
		userLookup.users += (
			user1.getUserId -> user1,
			user2.getUserId -> user2,
			user3.getUserId -> user3,
			user4.getUserId -> user4,
			user5.getUserId -> user5
		)
		
		val group1 = Fixtures.smallGroup("Group 1")
		val group2 = Fixtures.smallGroup("Group 2")
		
		set.groups.add(group1)
		set.groups.add(group2)
		group1.groupSet = set
		group2.groupSet = set
		group1._studentsGroup.userLookup = userLookup
		group2._studentsGroup.userLookup = userLookup
		
		set.members.addUser(user1.getUserId)
		set.members.addUser(user2.getUserId)
		set.members.addUser(user3.getUserId)
		set.members.addUser(user4.getUserId)
		set.members.addUser(user5.getUserId)
		
		membershipService.determineMembershipUsers(Seq.empty, Some(set.members)) returns (Seq(user1, user2, user3, user4, user5))
		
		val cmd = new AllocateStudentsToGroupsCommand(module, set, currentUser)
		cmd.service = service
		
		cmd.unallocated should be (JList())
		cmd.mapping should be (JMap(group1 -> JArrayList(), group2 -> JArrayList())) 
		
		cmd.populate()
		cmd.sort()
		
		cmd.unallocated should be (JList(user5, user4, user2, user3, user1))
		cmd.mapping should be (JMap(group1 -> JArrayList(), group2 -> JArrayList()))
		
		cmd.mapping.get(group1).addAll(Seq(user4, user2).asJavaCollection)
		cmd.mapping.get(group2).addAll(Seq(user1, user5).asJavaCollection)
		
		cmd.sort()
		
		cmd.mapping should be (JMap(group1 -> JArrayList(user4, user2), group2 -> JArrayList(user5, user1)))
		
		cmd.applyInternal() should be (set)
		
		there was one(service).saveOrUpdate(group1)
		there was one(service).saveOrUpdate(group2)
		
		group1._studentsGroup.includeUsers should be (JArrayList("0200202", "0672088"))
		group2._studentsGroup.includeUsers should be (JArrayList("8888888", "0672089"))
	}

}
