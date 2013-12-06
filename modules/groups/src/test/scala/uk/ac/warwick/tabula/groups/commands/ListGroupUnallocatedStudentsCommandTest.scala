package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.{CurrentUser, AcademicYear, MockUserLookup, TestBase, Mockito}
import uk.ac.warwick.tabula.services.{AssignmentMembershipService, ProfileServiceComponent, ProfileService}
import uk.ac.warwick.tabula.groups.{SmallGroupSetBuilder, SmallGroupBuilder}
import uk.ac.warwick.tabula.data.model.{StudentMember, UserGroup, Member}
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.helpers.Tap
import Tap.tap
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroup}
import org.joda.time.DateTime

class ListGroupUnallocatedStudentsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends ProfileServiceComponent {
		val profileService = mock[ProfileService]
		val membershipService = mock[AssignmentMembershipService]
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
		val userLookup = new MockUserLookup
		val membershipService = mock[AssignmentMembershipService]
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

		group1.name = "Group 1"
		group1.id = "abcdefgh1"
		group2.name = "Group 2"
		group2.id = "abcdefgh2"
		group3.name = "Group 3"
		group3.id = "abcdefgh3"
		group4.name = "Group 4"
		group4.id = "abcdefgh4"

		set.groups.add(group1)
		set.groups.add(group2)
		set.groups.add(group3)
		set.groups.add(group4)
		group1.groupSet = set
		group2.groupSet = set
		group3.groupSet = set
		group4.groupSet = set
		group1._studentsGroup.userLookup = userLookup
		group2._studentsGroup.userLookup = userLookup
		group3._studentsGroup.userLookup = userLookup
		group4._studentsGroup.userLookup = userLookup

		set.members.add(user1)
		set.members.add(user2)
		set.members.add(user3)
		set.members.add(user4)
		set.members.add(user5)
		set.members.add(user6)
		set.members.add(user7)
		set.members.add(user8)

		set.membershipService = membershipService
		set._membersGroup.userLookup = userLookup

		membershipService.determineMembershipUsers(set.upstreamAssessmentGroups, Some(set._membersGroup)) returns (set._membersGroup.users)
	}

	@Test
	def emptyGroups() = withUser("snow") { new Fixture() {
		val command = new ListGroupUnallocatedStudentsCommandInternal(set, currentUser) with CommandTestSupport
		val info = command.applyInternal()

		info.smallGroupSet should be (set)
		info.studentsNotInGroups.length should be (8)
		group1.students.add(user1)
		}
	}

	@Test
	def oneStudentInAGroup() = withUser("cutrue") { new Fixture() {
		group1.students.add(user1)

		var fakeCurrentUser = new CurrentUser(user8, user8)
		val command = new ListGroupUnallocatedStudentsCommandInternal(set, currentUser) with CommandTestSupport
		val info = command.applyInternal()

		info.smallGroupSet should be (set)
		info.studentsNotInGroups.length should be (7)
		info.studentsNotInGroups.exists(_.getWarwickId == user1.getWarwickId) should be (false)
		}
	}

	@Test
	def checkCurrentUserIsInGroup() = withUser("cutrue") { new Fixture() {
		var fakeCurrentUser = new CurrentUser(user8, user8)
		val command = new ListGroupUnallocatedStudentsCommandInternal(set, fakeCurrentUser) with CommandTestSupport

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

			val command = new ListGroupUnallocatedStudentsCommandInternal(set, currentUser) with CommandTestSupport
			val info = command.applyInternal()

			info.studentsNotInGroups.length should be (1)
			info.studentsNotInGroups.exists(_.getWarwickId == user8.getWarwickId) should be (true)
			info.studentsNotInGroups.exists(_.getWarwickId == user4.getWarwickId) should be (false)
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

			val command = new ListGroupUnallocatedStudentsCommandInternal(set, currentUser) with CommandTestSupport
			val info = command.applyInternal()

			info.studentsNotInGroups.length should be (2)
			info.studentsNotInGroups.exists(_.getWarwickId == user8.getWarwickId) should be (true)
			info.studentsNotInGroups.exists(_.getWarwickId == user7.getWarwickId) should be (true)
			info.studentsNotInGroups.exists(_.getWarwickId == user1.getWarwickId) should be (false)
		}
	}



}