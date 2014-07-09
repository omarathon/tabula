package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.events.EventHandling
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.services.{MaintenanceModeService, MaintenanceModeServiceImpl, RelationshipService, ProfileService}
import uk.ac.warwick.tabula.data.model.{StudentMember, Member, StudentRelationship, StudentRelationshipType}
import org.springframework.validation.BindException

class AllocateStudentsToRelationshipCommandTest extends TestBase with Mockito {

	EventHandling.enabled = false

	trait Environment {
		val service = smartMock[RelationshipService]
		val profileService = smartMock[ProfileService]

		val department = Fixtures.department("in", "IT Services")
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		val student1 = Fixtures.student("0000001", "student1", department)
		val student2 = Fixtures.student("0000002", "student2", department)
		val student3 = Fixtures.student("0000003", "student3", department)
		val student4 = Fixtures.student("0000004", "student4", department)
		val student5 = Fixtures.student("0000005", "student5", department)
		val student6 = Fixtures.student("0000006", "student6", department)
		val student7 = Fixtures.student("0000007", "student7", department)

		val staff1 = Fixtures.staff("1000001", "staff1", department)
		val staff2 = Fixtures.staff("1000002", "staff2", department)
		val staff3 = Fixtures.staff("1000003", "staff3", department)
		val staff4 = Fixtures.staff("1000004", "staff4", department)

		val rel1 = StudentRelationship(staff1, relationshipType, student1)
		val rel2 = StudentRelationship(staff1, relationshipType, student2)
		val rel3 = StudentRelationship(staff2, relationshipType, student3)

		Seq(staff1, staff2, staff3).foreach { staff =>
			profileService.getMemberByUniversityId(staff.universityId) returns (Some(staff))
			profileService.getAllMembersWithUserId(staff.userId) returns (Seq(staff))
		}

		Seq(student1, student2, student3, student4, student5, student6, student7).foreach { student =>
			profileService.getStudentBySprCode(student.universityId + "/1") returns (Some(student))
			profileService.getMemberByUniversityId(student.universityId) returns (Some(student))
			student.mostSignificantCourseDetails.get.department = department
		}

		service.listStudentRelationshipsByDepartment(relationshipType, department) returns (Seq(rel1, rel2, rel3))
		service.listStudentsWithoutRelationship(relationshipType, department) returns (Seq(student4, student5, student6, student7))

		service.findCurrentRelationships(relationshipType, student1.mostSignificantCourseDetails.get)	returns (Seq(rel1))
		service.findCurrentRelationships(relationshipType, student2.mostSignificantCourseDetails.get)	returns (Seq(rel2))
		service.findCurrentRelationships(relationshipType, student3.mostSignificantCourseDetails.get)	returns (Seq(rel3))
		service.findCurrentRelationships(relationshipType, student4.mostSignificantCourseDetails.get)	returns (Seq())
		service.findCurrentRelationships(relationshipType, student5.mostSignificantCourseDetails.get)	returns (Seq())
		service.findCurrentRelationships(relationshipType, student6.mostSignificantCourseDetails.get)	returns (Seq())
		service.findCurrentRelationships(relationshipType, student7.mostSignificantCourseDetails.get)	returns (Seq())

		val maintenanceModeService = mock[MaintenanceModeService]
		maintenanceModeService.enabled returns false

		val cmd = new AllocateStudentsToRelationshipCommand(department, relationshipType, currentUser)
		cmd.service = service
		cmd.profileService = profileService
		cmd.maintenanceMode = maintenanceModeService
	}

	@Test def itWorks = withUser("boombastic") {
		new Environment {
			cmd.unallocated should be(JList())
			cmd.mapping should be(JMap())

			cmd.populate()
			cmd.sort()

			cmd.unallocated should be(JList(student4, student5, student6, student7))
			cmd.mapping should be(JMap(staff1 -> JArrayList(student1, student2), staff2 -> JArrayList(student3)))

			cmd.additionalAgents = JArrayList(staff3.userId)
			cmd.onBind(new BindException(cmd, "cmd"))

			cmd.unallocated should be(JList(student4, student5, student6, student7))
			cmd.mapping should be(JMap(staff1 -> JArrayList(student1, student2), staff2 -> JArrayList(student3), staff3 -> JArrayList()))

			cmd.mapping.get(staff1).addAll(Seq(student4).asJavaCollection)
			cmd.mapping.get(staff3).addAll(Seq(student5, student7).asJavaCollection)

			cmd.sort()

			cmd.mapping should be(JMap(staff1 -> JArrayList(student1, student2, student4), staff2 -> JArrayList(student3), staff3 -> JArrayList(student5, student7)))
		}
	}

	@Test def testGetMemberAgentMappingBefore = withUser("boombastic") {
		new Environment {
			val agentMappings = cmd.getMemberAgentMappingsFromDatabase
			agentMappings.size should be (2)
			agentMappings.get(staff1) should be (Some(Set(student1, student2)))
			agentMappings.get(staff2) should be (Some(Set(student3)))
		}
	}

	@Test def testGetRemoveCommands = withUser("boombastic") {
		new Environment {
			service.listStudentRelationshipsWithMember(relationshipType, staff1) returns (Seq(rel1, rel2))
			service.listStudentRelationshipsWithMember(relationshipType, staff2) returns (Seq(rel3))

			val droppedAgents = Set(staff1.asInstanceOf[Member], staff2.asInstanceOf[Member])

			var removeCommands = cmd.getRemoveCommandsForDroppedAgents(droppedAgents)
			removeCommands.size should be (3)
			removeCommands.map(_.relationship).toSet should be (Set(rel1, rel2, rel3))
			removeCommands.head.relationshipType should be (relationshipType)

			removeCommands = cmd.getRemoveCommandsForDroppedAgents(Set(staff2.asInstanceOf[Member]))
			removeCommands.size should be (1)
			removeCommands.map(_.relationship).toSeq should be (Seq(rel3))

		}
	}

	@Test def testGetEditCommands = withUser("boombastic") {
		new Environment {
			service.listStudentRelationshipsWithMember(relationshipType, staff1) returns (Seq(rel1, rel2))
			service.listStudentRelationshipsWithMember(relationshipType, staff2) returns (Seq(rel3))

			// we're going to look to see if we need any edit commands for staff1 or staff2
			var agentsToEdit = Set(staff1.asInstanceOf[Member], staff2.asInstanceOf[Member])

			// putting students 5, 6 and 7 in a set
			val newStudentSet = Set(student5, student6, student7)

			// and assigning that set, perversely and irrelevantly, to agent 4
			val newAgentMappings = scala.collection.mutable.Map[Member, Set[StudentMember]]()
			newAgentMappings(staff4) = newStudentSet
			val mappings = newAgentMappings.toMap

			// now with that set of mappings and the unconnected agents, we should see no commands coming back:
			var editCommands = cmd.getEditStudentCommands(mappings, agentsToEdit)
			editCommands.size should be (0)

			// now, more sensibly, get the edit commands for staff 4 which should return commands we can inspect:
			service.findCurrentRelationships(relationshipType, student5.mostSignificantCourseDetails.head) returns Seq()
			service.findCurrentRelationships(relationshipType, student6.mostSignificantCourseDetails.head) returns Seq()
			service.findCurrentRelationships(relationshipType, student7.mostSignificantCourseDetails.head) returns Seq()

			editCommands = cmd.getEditStudentCommands(mappings, Set(staff4.asInstanceOf[Member]))

			editCommands.size should be (3)
			editCommands.map(_.oldAgent).head should be (None)
			editCommands.map(_.agent) should be (Set(staff4))
			editCommands.map(_.studentCourseDetails) should be (Set(student5.mostSignificantCourseDetails.head, student6.mostSignificantCourseDetails.head, student7.mostSignificantCourseDetails.head))
			editCommands.map(_.relationshipType) should be (Set(relationshipType))
		}
	}

	@Test def testApplyInternal = withUser("boombastic") {
		new Environment {

			// set up 'mapping' in the command as if from the db
			cmd.populate()
			cmd.sort()

			// set up 3 new relationships to add
			val rel41 = StudentRelationship(staff1, relationshipType, student4)
			val rel35 = StudentRelationship(staff3, relationshipType, student5)
			val rel37 = StudentRelationship(staff3, relationshipType, student7)

			service.saveStudentRelationships(relationshipType, student4.mostSignificantCourseDetails.head, Seq(staff1)) returns (Seq(rel41))
			service.saveStudentRelationships(relationshipType, student5.mostSignificantCourseDetails.head, Seq(staff3)) returns (Seq(rel35))
			service.saveStudentRelationships(relationshipType, student7.mostSignificantCourseDetails.head, Seq(staff3)) returns (Seq(rel37))

			cmd.additionalAgents = JArrayList(staff3.userId)
			cmd.onBind(new BindException(cmd, "cmd"))

			// add relationships staff1 -> student4 and staff3 -> students 5 and 7
			cmd.mapping.get(staff1).addAll(Seq(student4).asJavaCollection)
			cmd.mapping.get(staff3).addAll(Seq(student5, student7).asJavaCollection)

			val commandResults = cmd.applyInternal()
			commandResults.size should be (3)
			commandResults.map(_.modifiedRelationship).toSet should be (Set(rel41, rel35, rel37))
		}
	}

	@Test def testChangeStudentsForAgent = withUser("boombastic") {
		new Environment {
			// set up a new relationship to add
			val rel14 = StudentRelationship(staff1, relationshipType, student4)
			service.saveStudentRelationships(relationshipType, student4.mostSignificantCourseDetails.head, Seq(staff1)) returns (Seq(rel14))

			service.findCurrentRelationships(relationshipType, student4.mostSignificantCourseDetails.get)	returns (Seq(rel14))

			service.listStudentRelationshipsWithMember(relationshipType, staff1) returns (Seq(rel1))

			// set up 'mapping' in the command as if from the db
			cmd.populate()
			cmd.sort()

			cmd.onBind(new BindException(cmd, "cmd"))

			// already got relationships staff 1 -> student 1, staff2 -> student 1, staff 3 -> student 2
			// now change staff 1 to map to student 4 instead of student 1:
			cmd.mapping.remove(staff1)
			cmd.mapping.get(staff1).addAll(Seq(student4).asJavaCollection)
			cmd.mapping.get(staff1).removeAll(Seq(student1).asJavaCollection)

			val commandResults = cmd.applyInternal()
			commandResults.size should be (2) // one to remove, one to add
			commandResults.map(_.modifiedRelationship).toSet should be (Set(rel1))
		}
	}

	@Test def testRemoveAgent = withUser("boombastic") {
		new Environment {
			service.listStudentRelationshipsWithMember(relationshipType, staff1) returns (Seq(rel1))

			// set up 'mapping' in the command as if from the db
			cmd.populate()
			cmd.sort()

			cmd.onBind(new BindException(cmd, "cmd"))

			cmd.mapping.remove(staff1)

			val commandResults = cmd.applyInternal()
			commandResults.size should be (1)
			commandResults.map(_.modifiedRelationship).toSet should be (Set(rel1))
		}
	}

}
