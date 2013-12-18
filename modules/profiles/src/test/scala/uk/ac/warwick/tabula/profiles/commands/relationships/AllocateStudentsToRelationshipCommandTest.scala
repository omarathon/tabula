package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import org.springframework.validation.BindException

class AllocateStudentsToRelationshipCommandTest extends TestBase with Mockito {
	
	val service = smartMock[RelationshipService]
	val profileService = smartMock[ProfileService]
	
	@Test def itWorks = withUser("boombastic") {
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
		
		val rel1 = StudentRelationship(staff1.universityId, relationshipType, student1.universityId + "/1")
		val rel2 = StudentRelationship(staff1.universityId, relationshipType, student2.universityId + "/1")
		val rel3 = StudentRelationship(staff2.universityId, relationshipType, student3.universityId + "/1")
		
		Seq(staff1, staff2, staff3).foreach { staff => 
			profileService.getMemberByUniversityId(staff.universityId) returns (Some(staff))
			profileService.getAllMembersWithUserId(staff.userId) returns (Seq(staff))
		}
		
		Seq(student1, student2, student3, student4, student5, student6, student7).foreach { student => 
			profileService.getStudentBySprCode(student.universityId + "/1") returns (Some(student))
			profileService.getMemberByUniversityId(student.universityId) returns (Some(student))
		}
		
		service.listStudentRelationshipsByDepartment(relationshipType, department) returns (Seq(rel1, rel2, rel3))
		service.listStudentsWithoutRelationship(relationshipType, department) returns (Seq(student4, student5, student6, student7))
		
		val cmd = new AllocateStudentsToRelationshipCommand(department, relationshipType, currentUser)
		cmd.service = service
		cmd.profileService = profileService
		
		cmd.unallocated should be (JList())
		cmd.mapping should be (JMap()) 
		
		cmd.populate()
		cmd.sort()
		
		cmd.unallocated should be (JList(student4, student5, student6, student7))
		cmd.mapping should be (JMap(staff1 -> JArrayList(student1, student2), staff2 -> JArrayList(student3)))
		
		cmd.additionalAgents = JArrayList(staff3.userId)
		cmd.onBind(new BindException(cmd, "cmd"))
		
		cmd.unallocated should be (JList(student4, student5, student6, student7))
		cmd.mapping should be (JMap(staff1 -> JArrayList(student1, student2), staff2 -> JArrayList(student3), staff3 -> JArrayList()))
		
		cmd.mapping.get(staff1).addAll(Seq(student4).asJavaCollection)
		cmd.mapping.get(staff3).addAll(Seq(student5, student7).asJavaCollection)
		
		cmd.sort()
		
		cmd.mapping should be (JMap(staff1 -> JArrayList(student1, student2, student4), staff2 -> JArrayList(student3), staff3 -> JArrayList(student5, student7)))
		
		// TODO test applyInternal, though that's fiddly.
	}

}
