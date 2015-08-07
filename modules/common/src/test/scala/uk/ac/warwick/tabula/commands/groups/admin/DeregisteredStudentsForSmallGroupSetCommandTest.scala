package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.DeregisteredStudentsForSmallGroupSetCommand.StudentNotInMembership
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.{UnspecifiedTypeUserGroup, UserGroup}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

import scala.collection.JavaConverters._

class DeregisteredStudentsForSmallGroupSetCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent with ProfileServiceComponent {
		val smallGroupService = mock[SmallGroupService]
		val profileService = mock[ProfileService]
	}

	private trait Fixture {
		val userLookup = new MockUserLookup

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}

		userLookup.registerUsers("user1", "user2", "user3", "user4")
		val user1 = userLookup.getUserByUserId("user1")
		val user2 = userLookup.getUserByUserId("user2")
		val user3 = userLookup.getUserByUserId("user3")
		val user4 = userLookup.getUserByUserId("user4")

		val module = Fixtures.module("in101", "Introduction to Scala")
		module.id = "moduleId"

		val set = Fixtures.smallGroupSet("IN101 Seminars")
		wireUserLookup(set.members)
		set.id = "setId"
		set.module = module

		val group1 = Fixtures.smallGroup("Group 1")
		wireUserLookup(group1.students)
		group1.id = "group1Id"
		group1.groupSet = set
		set.groups.add(group1)

		val group2 = Fixtures.smallGroup("Group 2")
		wireUserLookup(group2.students)
		group2.id = "group2Id"
		group2.groupSet = set
		set.groups.add(group2)

		// user1, user2 and user3 are members of the assignment
		set.members.add(user1)
		set.members.add(user2)
		set.members.add(user3)

		// user2 is in group1
		group1.students.add(user2)

		// user3 and user4 are in group2
		group2.students.add(user3)
		group2.students.add(user4)

		set.membershipService = mock[AssessmentMembershipService]
		set.membershipService.determineMembershipUsers(Nil, Some(set.members)) returns (set.members.users)

		set.studentsNotInMembership should be (Seq(user4))
	}

	private trait CommandFixture extends Fixture {
		val command = new DeregisteredStudentsForSmallGroupSetCommandInternal(module, set) with CommandTestSupport
	}

	private trait PopulateFixture extends Fixture {
		val command = new PopulateDeregisteredStudentsForSmallGroupSetCommandState with DeregisteredStudentsForSmallGroupSetCommandState {
			val module = PopulateFixture.this.module
			val set = PopulateFixture.this.set
		}
	}

	@Test def populate { new PopulateFixture {
		command.students.asScala should be (Nil)

		command.populate()

		command.students.asScala should be (Seq(user4))
	}}

	@Test def apply { new CommandFixture {
		// Ignore profileService stuff
		command.profileService.getMemberByUser(user4, true) returns (None)

		command.students.add(user3) // user3 is a no-op because they are still in the group
		command.students.add(user4)

		val results = command.applyInternal()
		results.size should be (1)

		results.head.student.asUser should be (user4)
		results.head.group should be (group2)

		verify(command.smallGroupService, times(1)).removeUserFromGroup(user4, group2)
		verify(command.smallGroupService, times(0)).removeUserFromGroup(user3, group2)
	}}

	@Test def permissions { new Fixture {
		val (theModule, theSet) = (module, set)
		val command = new DeregisteredStudentsForSmallGroupSetPermissions with DeregisteredStudentsForSmallGroupSetCommandState {
			val module = theModule
			val set = theSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoModule {
		val command = new DeregisteredStudentsForSmallGroupSetPermissions with DeregisteredStudentsForSmallGroupSetCommandState {
			val module = null
			val set = new SmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet {
		val command = new DeregisteredStudentsForSmallGroupSetPermissions with DeregisteredStudentsForSmallGroupSetCommandState {
			val module = Fixtures.module("in101")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet {
		val command = new DeregisteredStudentsForSmallGroupSetPermissions with DeregisteredStudentsForSmallGroupSetCommandState {
			val module = Fixtures.module("in101")
			module.id = "set id"

			val set = new SmallGroupSet(Fixtures.module("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def describe { new Fixture {
		val (mod, s) = (module, set)
		val command = new DeregisteredStudentsForSmallGroupSetDescription with DeregisteredStudentsForSmallGroupSetCommandState {
			override val eventName = "test"
			val module = mod
			val set = s
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"module" -> "moduleId",
			"smallGroupSet" -> "setId"
		))
	}}

	@Test def describeResult { new Fixture {
		val (mod, s) = (module, set)
		val command = new DeregisteredStudentsForSmallGroupSetDescription with DeregisteredStudentsForSmallGroupSetCommandState {
			override val eventName = "test"
			val module = mod
			val set = s
		}

		val results = Seq(
			StudentNotInMembership(MemberOrUser(user3), group1),
			StudentNotInMembership(MemberOrUser(user4), group2)
		)

		val d = new DescriptionImpl
		command.describeResult(d, results)

		d.allProperties should be (Map(
			"module" -> "moduleId",
			"smallGroupSet" -> "setId",
			"students" -> Seq(user3.getWarwickId, user4.getWarwickId)
		))
	}}

	@Test def wires { new Fixture {
		val command = DeregisteredStudentsForSmallGroupSetCommand(module, set)

		command should be (anInstanceOf[Appliable[Seq[StudentNotInMembership]]])
		command should be (anInstanceOf[Describable[Seq[StudentNotInMembership]]])
		command should be (anInstanceOf[DeregisteredStudentsForSmallGroupSetPermissions])
		command should be (anInstanceOf[DeregisteredStudentsForSmallGroupSetCommandState])
		command should be (anInstanceOf[PopulateOnForm])
	}}

}
