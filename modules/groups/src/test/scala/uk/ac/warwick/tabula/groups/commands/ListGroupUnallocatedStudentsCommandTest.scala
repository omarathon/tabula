package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.{CurrentUser, MockUserLookup, TestBase, Mockito}
import uk.ac.warwick.tabula.services.{AssignmentMembershipService, ProfileServiceComponent, ProfileService}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroup}

class ListGroupUnallocatedStudentsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends ProfileServiceComponent {
		var profileService = mock[ProfileService]
	}

	def createUser(userId: String, firstName: String, lastName:String, warwickId:String) = {
		val user = new User(userId)
		user.setFoundUser(true)
		user.setFirstName(firstName)
		user.setLastName(lastName)
		user.setWarwickId(warwickId)
		user
	}

	trait Fixture {
		val department = Fixtures.department("in", "IT Services")
		val userLookup = new MockUserLookup
		val membershipService = mock[AssignmentMembershipService]
		val profileService = mock[ProfileService]
		val set = new SmallGroupSet

		val user1 = createUser("cuscav", "Mathew", "Mannion", "0672089")
		val user2 = createUser("cusebr", "Nick", "Howes", "0672088")
		val user3 = createUser("cusfal", "Matthew", "Jones", "9293883")
		val user4 = createUser("curef", "John", "Dale", "0200202")
		var user5 = createUser("cusmab", "Steven", "Carpenter", "8888888")
		val user6 = createUser("caaaaa", "Terence", "Trent D'arby", "6666666")
		val user7 = createUser("cutrue", "Fabrice", "Morvan", "7777777")
		val user8 = createUser("cugirl", "Robert", "Pilatus", "1111111")

		userLookup.users += (
			user1.getUserId -> user1,
			user2.getUserId -> user2,
			user3.getUserId -> user3,
			user4.getUserId -> user4,
			user5.getUserId -> user5,
			user6.getUserId -> user6,
			user7.getUserId -> user7,
			user8.getUserId -> user8
			)

		val group1 = new SmallGroup
		val group2 = new SmallGroup
		val group3 = new SmallGroup
		val group4 = new SmallGroup

		Seq(group1, group2, group3, group4).zipWithIndex.foreach {
			case(group, index) =>
				group.name = "Group " + index
		  	group.id = "abcdefgh" + index
				set.groups.add(group)
				group.groupSet = set
				group._studentsGroup.userLookup = userLookup
		}

		val student1 = Fixtures.student(user1.getWarwickId, user1.getUserId, department)
		val student2 = Fixtures.student(user2.getWarwickId, user2.getUserId, department)
		val student3 = Fixtures.student(user3.getWarwickId, user3.getUserId, department)
		val student4 = Fixtures.student(user4.getWarwickId, user4.getUserId, department)
		val student5 = Fixtures.student(user5.getWarwickId, user5.getUserId, department)
		val student6 = Fixtures.student(user6.getWarwickId, user6.getUserId, department)
		val student7 = Fixtures.student(user7.getWarwickId, user7.getUserId, department)
		val student8 = Fixtures.student(user8.getWarwickId, user8.getUserId, department)

		val allUsers = Seq(user1, user2, user3, user4, user5, user6, user7, user8)
		allUsers.foreach(set.members.add(_))

		val userToStudent =	Map(user1.getWarwickId -> student1,
														user2.getWarwickId -> student2,
														user3.getWarwickId -> student3,
														user4.getWarwickId -> student4,
														user5.getWarwickId -> student5,
														user6.getWarwickId -> student6,
														user7.getWarwickId -> student7,
														user8.getWarwickId -> student8
												)

		set.membershipService = membershipService
		set._membersGroup.userLookup = userLookup
		membershipService.determineMembershipUsers(set.upstreamAssessmentGroups, Some(set._membersGroup)) returns (set._membersGroup.users)

		allUsers.foreach {
			user => profileService.getMemberByUniversityId(user.getWarwickId) returns Some(userToStudent(user.getWarwickId))
		}
	}

	@Test
	def emptyGroups() = withUser("snow") { new Fixture() {
		var command = new ListGroupUnallocatedStudentsCommandInternal(set, currentUser) with CommandTestSupport
		command.profileService = profileService
		val info = command.applyInternal()

		info.smallGroupSet should be (set)
		info.membersNotInGroups.length should be (8)
		group1.students.add(user1)
		}
	}

	@Test
	def oneStudentInAGroup() = withUser("cutrue") { new Fixture() {
		group1.students.add(user1)

		var command = new ListGroupUnallocatedStudentsCommandInternal(set, currentUser) with CommandTestSupport
		command.profileService = profileService
		val info = command.applyInternal()

		info.smallGroupSet should be (set)
		info.membersNotInGroups.length should be (7)
		info.membersNotInGroups.exists(_.universityId == user1.getWarwickId) should be (false)
		}
	}

	@Test
	def checkCurrentUserIsInGroup() = withUser("cutrue") { new Fixture() {
		val fakeCurrentUser = new CurrentUser(user8, user8)
		var command = new ListGroupUnallocatedStudentsCommandInternal(set, fakeCurrentUser) with CommandTestSupport
		command.profileService = profileService

		val info = command.applyInternal()
		info.userIsMember should be (true)
		}
	}


	@Test
	def sevenStudentsInOneGroup() = withUser("cutrue") { new Fixture() {
			/* user8 should be unallocated */
			group1.students.add(user1)
			group1.students.add(user2)
			group1.students.add(user3)
			group1.students.add(user4)
			group1.students.add(user5)
			group1.students.add(user6)
			group1.students.add(user7)

			var command = new ListGroupUnallocatedStudentsCommandInternal(set, currentUser) with CommandTestSupport
			command.profileService = profileService
			val info = command.applyInternal()

			info.membersNotInGroups.length should be (1)
			info.membersNotInGroups.exists(_.universityId == user8.getWarwickId) should be (true)
			info.membersNotInGroups.exists(_.universityId == user4.getWarwickId) should be (false)
		}
	}


	@Test
	def sixStudentsAllocatedAcrossGroups() = withUser("cutrue") { new Fixture() {
			/* only user7 and user8 are unallocated */
			group1.students.add(user1)
			group2.students.add(user2)
			group2.students.add(user3)
			group3.students.add(user3)
			group4.students.add(user4)
			group1.students.add(user5)
			group2.students.add(user6)

			var command = new ListGroupUnallocatedStudentsCommandInternal(set, currentUser) with CommandTestSupport
			command.profileService = profileService
			val info = command.applyInternal()

			info.membersNotInGroups.length should be (2)
			info.membersNotInGroups.exists(_.universityId == user8.getWarwickId) should be (true)
			info.membersNotInGroups.exists(_.universityId == user7.getWarwickId) should be (true)
			info.membersNotInGroups.exists(_.universityId == user1.getWarwickId) should be (false)
		}
	}
}