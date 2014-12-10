package uk.ac.warwick.tabula.groups.commands.admin


import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class SmallGroupsMarkerAllocationCommandTest extends TestBase with Mockito {

	@Test
	def getsAllocations() = {
		val cmd = new SmallGroupsMarkerAllocationCommandInternal(assignment) with MockServices
		val allocations = cmd.applyInternal()
		allocations.map(_.set) should be (Seq(smallGroupSetA))
	}

	val marker1 = Fixtures.user("marker1", "marker1")
	val marker2 = Fixtures.user("marker2", "marker2")
	val marker3 = Fixtures.user("marker3", "marker3")
	val marker4 = Fixtures.user("marker4", "marker4")
	val markers = Seq(marker1, marker2, marker3, marker4)

	val student1 = Fixtures.user("student1", "student1")
	val student2 = Fixtures.user("student2", "student2")
	val student3 = Fixtures.user("student3", "student3")
	val student4 = Fixtures.user("student4", "student4")
	val students = Seq(student1, student2, student3, student4)

	def testUserGroup(users: User*) = {
		val ug = mock[UserGroup]
		ug.users returns users
		ug
	}

	val module = Fixtures.module("hz101")
	val assignment = Fixtures.assignment("Herons are foul")
	assignment.module = module
	assignment.academicYear = AcademicYear(2014)
	assignment.markingWorkflow = {
		val workflow = Fixtures.seenSecondMarkingWorkflow("wflow")
		workflow.firstMarkers = testUserGroup(marker1, marker2)
		workflow.secondMarkers = testUserGroup(marker3)
		workflow
	}

	val smallGroupSetA = Fixtures.smallGroupSet("A")
	val smallGroupA1 = Fixtures.smallGroup("A1")
	smallGroupA1.groupSet = smallGroupSetA
	smallGroupA1.addEvent({
		val event = Fixtures.smallGroupEvent("event A1")
		event.tutors = testUserGroup(marker1, marker2)
		event
	})

	val smallGroupA2 = Fixtures.smallGroup("A2")
	smallGroupA2.groupSet = smallGroupSetA
	smallGroupA2.addEvent({
		val event = Fixtures.smallGroupEvent("event A2")
		event.tutors = testUserGroup(marker3)
		event
	})

	trait MockServices extends SmallGroupServiceComponent with AssignmentMembershipServiceComponent {
		def smallGroupService: SmallGroupService =  {
			val groupsService = mock[SmallGroupService]
			groupsService.getSmallGroupSets(assignment.module, assignment.academicYear) returns Seq(smallGroupSetA)
			groupsService
		}
		def assignmentMembershipService: AssignmentMembershipService = {
			val membershipService = mock[AssignmentMembershipService]
			membershipService.determineMembershipUsers(assignment) returns Seq(student1, student2, student3)
			membershipService
		}
	}

}