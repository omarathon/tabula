package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.events.EventHandling
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.services.{MaintenanceModeService, RelationshipService, ProfileService}
import uk.ac.warwick.tabula.data.model.{StudentRelationship, StudentRelationshipType}
import org.springframework.validation.BindException

class AllocateStudentsToRelationshipCommandTest extends TestBase with Mockito {

	EventHandling.enabled = false

	trait Environment {
		val relationshipService = smartMock[RelationshipService]
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

		val rel11 = StudentRelationship(staff1, relationshipType, student1)
		val rel12 = StudentRelationship(staff1, relationshipType, student2)
		val rel23 = StudentRelationship(staff2, relationshipType, student3)

		Seq(staff1, staff2, staff3, staff4).foreach { staff =>
			profileService.getMemberByUniversityId(staff.universityId) returns Some(staff)
			profileService.getAllMembersWithUserId(staff.userId) returns Seq(staff)
		}

		Seq(student1, student2, student3, student4, student5, student6, student7).foreach { student =>
			profileService.getStudentBySprCode(student.universityId + "/1") returns Some(student)
			profileService.getMemberByUniversityId(student.universityId) returns Some(student)
			student.mostSignificantCourseDetails.get.department = department
		}

		relationshipService.listStudentRelationshipsByDepartment(relationshipType, department) returns Seq(rel11, rel12, rel23)
		relationshipService.listStudentsWithoutRelationship(relationshipType, department) returns Seq(student4, student5, student6, student7)

		relationshipService.findCurrentRelationships(relationshipType, student1.mostSignificantCourseDetails.get)	returns Seq(rel11)
		relationshipService.findCurrentRelationships(relationshipType, student2.mostSignificantCourseDetails.get)	returns Seq(rel12)
		relationshipService.findCurrentRelationships(relationshipType, student3.mostSignificantCourseDetails.get)	returns Seq(rel23)
		relationshipService.findCurrentRelationships(relationshipType, student4.mostSignificantCourseDetails.get)	returns Seq()
		relationshipService.findCurrentRelationships(relationshipType, student5.mostSignificantCourseDetails.get)	returns Seq()
		relationshipService.findCurrentRelationships(relationshipType, student6.mostSignificantCourseDetails.get)	returns Seq()
		relationshipService.findCurrentRelationships(relationshipType, student7.mostSignificantCourseDetails.get)	returns Seq()

		val maintenanceModeService = mock[MaintenanceModeService]
		maintenanceModeService.enabled returns false

		val cmd = new AllocateStudentsToRelationshipCommand(department, relationshipType, currentUser)
		cmd.relationshipService = relationshipService
		cmd.profileService = profileService
		cmd.maintenanceMode = maintenanceModeService
	}

	@Test def populatesData() = withUser("boombastic") {
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

	@Test def validateStudentTutorMappingNoChanges() = withUser("boombastic") {
		new Environment {
			cmd.populate()
			cmd.studentTutorMapping.isEmpty should be {true}
		}
	}

	@Test def validateStudentTutorMappingAddRel() = withUser("boombastic") {
		new Environment {
			cmd.populate()
			cmd.mapping.put(staff4, JList(student4))
			cmd.mapping.put(staff3, JList(student5))

			cmd.studentTutorMapping.size should be (2)
			cmd.studentTutorMapping(student4).tutorsBefore should be (Set())
			cmd.studentTutorMapping(student4).tutorsAfter should be (Set(staff4))
			cmd.studentTutorMapping(student4).oldTutors should be (Set())
			cmd.studentTutorMapping(student4).newTutors should be (Set(staff4))

			cmd.studentTutorMapping(student5).tutorsBefore should be (Set())
			cmd.studentTutorMapping(student5).tutorsAfter should be (Set(staff3))
			cmd.studentTutorMapping(student5).oldTutors should be (Set())
			cmd.studentTutorMapping(student5).newTutors should be (Set(staff3))

			cmd.studentsWithTutorRemoved.size should be (1)
			cmd.studentsWithTutorAdded.size should be (2)
		}
	}

	@Test def validateStudentTutorMappingRemoveRel() = withUser("boombastic") {
		new Environment {
			cmd.populate()
			cmd.mapping.get(staff2).removeAll(Seq(student3).asJavaCollection)

			cmd.studentTutorMapping.size should be (1)
			cmd.studentTutorMapping(student3).tutorsBefore should be (Set(staff2))
			cmd.studentTutorMapping(student3).tutorsAfter should be (Set())
			cmd.studentTutorMapping(student3).oldTutors should be (Set(staff2))
			cmd.studentTutorMapping(student3).newTutors should be (Set())
		}
	}

	@Test def validateStudentTutorMappingSwapTutor() = withUser("boombastic") {
		new Environment {
			cmd.populate()
			cmd.mapping.get(staff2).removeAll(Seq(student3).asJavaCollection)
			cmd.mapping.get(staff1).addAll(Seq(student3).asJavaCollection)

			cmd.studentTutorMapping.size should be (1)
			cmd.studentTutorMapping(student3).tutorsBefore should be (Set(staff2))
			cmd.studentTutorMapping(student3).tutorsAfter should be (Set(staff1))
			cmd.studentTutorMapping(student3).oldTutors should be (Set(staff2))
			cmd.studentTutorMapping(student3).newTutors should be (Set(staff1))
		}
	}

	// student 1 has three tutors to start - we remove one and expect to find two left
	@Test def validateStudentTutorMappingStudentWithMultiTutorsRemove() = withUser("boombastic") {
		new Environment {

			val rel21 = StudentRelationship(staff2, relationshipType, student1)
			val rel41 = StudentRelationship(staff4, relationshipType, student1)

			relationshipService.listStudentRelationshipsByDepartment(relationshipType, department) returns Seq(rel11, rel12, rel23, rel21, rel41)
			relationshipService.findCurrentRelationships(relationshipType, student1.mostSignificantCourseDetails.get)	returns Seq(rel11, rel21, rel41)

			cmd.populate()
			cmd.sort()

			cmd.mapping.get(staff1).removeAll(Seq(student1).asJavaCollection)

			cmd.studentTutorMapping.size should be (1)
			cmd.studentTutorMapping(student1).tutorsBefore should be (Set(staff1, staff2, staff4))
			cmd.studentTutorMapping(student1).tutorsAfter should be (Set(staff2, staff4))
			cmd.studentTutorMapping(student1).oldTutors should be (Set(staff1))
			cmd.studentTutorMapping(student1).newTutors should be (Set())
		}
	}

	// student 1 has three tutors to start - we add a fourth
	@Test def validateStudentTutorMappingStudentWithMultiTutorsAdd() = withUser("boombastic") {
		new Environment {

			val rel21 = StudentRelationship(staff2, relationshipType, student1)
			val rel41 = StudentRelationship(staff4, relationshipType, student1)

			relationshipService.listStudentRelationshipsByDepartment(relationshipType, department) returns Seq(rel11, rel12, rel23, rel21, rel41)
			relationshipService.findCurrentRelationships(relationshipType, student1.mostSignificantCourseDetails.get)	returns Seq(rel11, rel21, rel41)

			cmd.populate()
			cmd.sort()

			cmd.mapping.get(staff3).addAll(Seq(student1).asJavaCollection)

			cmd.studentTutorMapping.size should be (1)
			cmd.studentTutorMapping(student1).tutorsBefore should be (Set(staff1, staff2, staff4))
			cmd.studentTutorMapping(student1).tutorsAfter should be (Set(staff1, staff2, staff3, staff4))
			cmd.studentTutorMapping(student1).oldTutors should be (Set())
			cmd.studentTutorMapping(student1).newTutors should be (Set(staff3))
		}
	}
/*
	@Test def testGetEditCommands() = withUser("boombastic") {
		new Environment {
			bindCommand()
			val commands = cmd.getEditStudentRelationshipCommands
			commands.size should be (0)
			
			relationshipService.listStudentRelationshipsWithMember(relationshipType, staff1) returns (Seq(rel1, rel2))
			relationshipService.listStudentRelationshipsWithMember(relationshipType, staff2) returns (Seq(rel3))

			// we're going to look to see if we need any edit commands for staff1 or staff2
			var agentsToEdit = Set(staff1.asInstanceOf[Member], staff2.asInstanceOf[Member])

			// we already have a relationship from staff 2 to student 3.

			// now add a relationship from staff 2 to student 4 - first by creating the revised student set
			val newStudentSet1 = Set(student3, student4)

			// and then assigning that set to staff2:
			val newAgentMappingsMutable = scala.collection.mutable.Map[Member, Set[StudentMember]]()
			newAgentMappingsMutable(staff2) = newStudentSet1
			val newAgentMappings = newAgentMappingsMutable.toMap

			// now with that set of mappings we should just see a command for the new relationship coming back:
			val editCommands = cmd.getEditStudentRelationshipCommands(mappings, newAgentMappings, agentsToEdit)
			editCommands.size should be (1)
			val editCommand = editCommands.head
			editCommand.oldAgents.isEmpty should be (true)
			editCommand.agent should be (staff2)
			editCommand.studentCourseDetails should be (student4.mostSignificantCourseDetails.head)
			editCommand.relationshipType should be (relationshipType)

			// putting students 5, 6 and 7 in a set
			val newStudentSet2 = Set(student5, student6, student7)

			// and assigning that set, perversely and irrelevantly, to agent 4
			val newAgentMappingsMutable2 = scala.collection.mutable.Map[Member, Set[StudentMember]]()
			newAgentMappingsMutable2(staff4) = newStudentSet2
			val newAgentMappings2 = newAgentMappingsMutable2.toMap

			// now with that set of mappings and the unconnected agents, we should see no commands coming back:
			val editCommands2 = cmd.getEditStudentRelationshipCommands(mappings, newAgentMappings2, agentsToEdit)
			editCommands2.size should be (0)

			// now, more sensibly, get the edit commands for staff 4 which should return commands we can inspect:
			relationshipService.findCurrentRelationships(relationshipType, student5.mostSignificantCourseDetails.head) returns Seq()
			relationshipService.findCurrentRelationships(relationshipType, student6.mostSignificantCourseDetails.head) returns Seq()
			relationshipService.findCurrentRelationships(relationshipType, student7.mostSignificantCourseDetails.head) returns Seq()

			val editCommands3 = cmd.getEditStudentRelationshipCommands(mappings, newAgentMappings2, Set(staff4.asInstanceOf[Member]))

			editCommands3.size should be (3)
			editCommands3.flatMap(_.oldAgents).isEmpty should be (true)
			editCommands3.map(_.agent) should be (Set(staff4))
			editCommands3.map(_.studentCourseDetails) should be (Set(student5.mostSignificantCourseDetails.head, student6.mostSignificantCourseDetails.head, student7.mostSignificantCourseDetails.head))
			editCommands3.map(_.relationshipType) should be (Set(relationshipType))

		}
	}

	@Test def testApplyInternalSingleSwap = withUser("boombastic") {
		new Environment {

			// set up 'mapping' in the command as if from the db
			cmd.populate()
			cmd.sort()

			// set up new relationships to add
			val rel22 = StudentRelationship(staff2, relationshipType, student2)

			service.saveStudentRelationships(relationshipType, student2.mostSignificantCourseDetails.head, Seq(staff2)) returns (Seq(rel22))

			cmd.onBind(new BindException(cmd, "cmd"))

			// add relationship staff2 -> student2
			cmd.mapping.remove(staff1)
			cmd.mapping.put(staff1, JList(student1))
			cmd.mapping.get(staff2).addAll(Seq(student2).asJavaCollection)

			cmd.onBind(new BindException(cmd, "cmd"))

			val commandResults = cmd.applyInternal()
			commandResults.size should be (1)
			commandResults.map(_.modifiedRelationship).toSet should be (Set(rel22))
			val oldAgents = commandResults.map(_.oldAgents)
			oldAgents.size should be (1)
			oldAgents.head.head should be (staff1)
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

			cmd.onBind(new BindException(cmd, "cmd"))

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

			// set up 'mapping' in the command as if from the db
			cmd.populate()
			cmd.sort()

			cmd.onBind(new BindException(cmd, "cmd"))

			// pre-existing relationships are staff 1 -> student 1, staff2 -> student 1, staff 3 -> student 2
			// now change staff 1 to map to student 4 instead of student 1:
			cmd.mapping.get(staff1).addAll(Seq(student4).asJavaCollection)
			cmd.mapping.get(staff1).removeAll(Seq(student1).asJavaCollection)

			cmd.onBind(new BindException(cmd, "cmd"))

			val commandResults = cmd.applyInternal()
			commandResults.size should be (2) // one to remove, one to add
			commandResults.map(_.modifiedRelationship).toSet should be (Set(rel1, rel14))
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

			cmd.onBind(new BindException(cmd, "cmd"))

			val commandResults = cmd.applyInternal()
			commandResults.size should be (1)
			commandResults.map(_.modifiedRelationship).toSet should be (Set(rel1))
		}
	}*/

}
