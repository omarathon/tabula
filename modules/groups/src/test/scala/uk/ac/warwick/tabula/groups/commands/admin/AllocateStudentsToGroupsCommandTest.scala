package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.services.{UserGroupCacheManager, ProfileService, SmallGroupService, AssignmentMembershipService}
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.MockUserLookup
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.{UnspecifiedTypeUserGroup, SitsStatus, UserGroup}

class AllocateStudentsToGroupsCommandTest extends TestBase with Mockito {

	trait Environment {
		val service = mock[SmallGroupService]
		val membershipService = mock[AssignmentMembershipService]
		val userLookup = new MockUserLookup
		val profileService = smartMock[ProfileService]

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}

		val module = Fixtures.module("in101", "Introduction to Scala")
		val set = Fixtures.smallGroupSet("My small groups")
		set.module = module
		set.membershipService = membershipService
		wireUserLookup(set.members)

		val user1 = new User("cuscav")
		user1.setFoundUser(true)
		user1.setFirstName("Mathew")
		user1.setLastName("Mannion")
		user1.setWarwickId("0672089")

		val sitsStatus = Fixtures.sitsStatus() // defaults to fully enrolled
		val department = Fixtures.department("CE", "Centre for Lifelong Learning")

		val student1 = Fixtures.student("0672089", "cuscav", department, department, sitsStatus)
		student1.firstName = "Mathew"
		student1.lastName = "Mannion"

		val user2 = new User("cusebr")
		user2.setFoundUser(true)
		user2.setFirstName("Nick")
		user2.setLastName("Howes")
		user2.setWarwickId("0672088")

		val student2 = Fixtures.student("0672088", "cusebr", department, department, sitsStatus)
		student2.firstName = "Nick"
		student2.lastName = "Howes"

		val user3 = new User("cusfal")
		user3.setFoundUser(true)
		user3.setFirstName("Matthew")
		user3.setLastName("Jones")
		user3.setWarwickId("9293883")

		val student3 = Fixtures.student("9293883", "cusfal", department, department, sitsStatus)
		student3.firstName = "Matthew"
		student3.lastName = "Jones"

		val user4 = new User("curef")
		user4.setFoundUser(true)
		user4.setFirstName("John")
		user4.setLastName("Dale")
		user4.setWarwickId("0200202")

		val student4 = Fixtures.student("0200202", "curef", department, department, sitsStatus)
		student4.firstName = "John"
		student4.lastName = "Dale"

		val user5 = new User("cusmab")
		user5.setFoundUser(true)
		user5.setFirstName("Steven")
		user5.setLastName("Carpenter")
		user5.setWarwickId("8888888")

		val student5 = Fixtures.student("8888888", "cusmab", department, department, sitsStatus)
		student5.firstName = "Steven"
		student5.lastName = "Carpenter"

		userLookup.users +=(
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
		wireUserLookup(group1.students)
		wireUserLookup(group2.students)

		set.members.add(user1)
		set.members.add(user2)
		set.members.add(user3)
		set.members.add(user4)
		set.members.add(user5)

		membershipService.determineMembershipUsers(Seq.empty, Some(set.members)) returns (Seq(user1, user2, user3, user4, user5))
		profileService.getMemberByUser(user1) returns (Option(student1))
		profileService.getMemberByUser(user2) returns (Option(student2))
		profileService.getMemberByUser(user3) returns (Option(student3))
		profileService.getMemberByUser(user4) returns (Option(student4))
		profileService.getMemberByUser(user5) returns (Option(student5))

	}

	@Test def itWorks = withUser("boombastic") {
		new Environment {
			val cmd = new AllocateStudentsToGroupsCommand(module, set, currentUser)
			cmd.service = service
			cmd.profileService = profileService

			cmd.unallocated should be(JList())
			cmd.mapping should be(JMap(group1 -> JArrayList(), group2 -> JArrayList()))

			cmd.populate()
			cmd.sort()

			student1.freshStudentCourseDetails.head.statusOnRoute.code should be ("F")

			cmd.unallocated should be(JList(user5, user4, user2, user3, user1))
			cmd.mapping should be(JMap(group1 -> JArrayList(), group2 -> JArrayList()))

			cmd.mapping.get(group1).addAll(Seq(user4, user2).asJavaCollection)
			cmd.mapping.get(group2).addAll(Seq(user1, user5).asJavaCollection)

			cmd.sort()

			cmd.mapping should be(JMap(group1 -> JArrayList(user4, user2), group2 -> JArrayList(user5, user1)))

			cmd.applyInternal() should be(set)

			there was one(service).saveOrUpdate(group1)
			there was one(service).saveOrUpdate(group2)

			group1.students.asInstanceOf[UserGroup].includedUserIds should be(Seq("0200202", "0672088"))
			group2.students.asInstanceOf[UserGroup].includedUserIds should be(Seq("8888888", "0672089"))
		}
	}

	@Test def testRemovePermanentlyWithdrawn = withUser("boombastic") {
		new Environment {
			val cmd = new AllocateStudentsToGroupsCommand(module, set, currentUser)
			cmd.service = service
			cmd.profileService = profileService

			val usersWithoutPermWithdrawn = cmd.removePermanentlyWithdrawn(Seq(user1, user2, user3, user4, user5))
			student1.freshStudentCourseDetails.size should be (1)

			cmd.removePermanentlyWithdrawn(Seq(user1, user2, user3, user4, user5)).size should be (5)

			student1.freshStudentCourseDetails.head.statusOnRoute = new SitsStatus("P", "PWD", "Permanently Withdrawn")

			cmd.removePermanentlyWithdrawn(Seq(user1, user2, user3)).size should be (2)

			cmd.populate()
			cmd.unallocated.size should be (4)
		}
	}
}
