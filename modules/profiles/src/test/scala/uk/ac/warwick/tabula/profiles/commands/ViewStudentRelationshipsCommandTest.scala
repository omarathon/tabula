package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{StaffMember, StudentRelationship, StudentRelationshipType, Department}
import uk.ac.warwick.tabula.helpers.Tap
import Tap.tap
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}

class ViewStudentRelationshipsCommandTest extends TestBase with Mockito {

	private trait Fixture {
		val profileService = mock[ProfileService]
		val relationshipService =  mock[RelationshipService]

		val departmentOfXXX = new Department().tap(_.code = "xxx")
		val tutorRelType = new StudentRelationshipType().tap(_.description = "tutor")

		val studentRel1 = new StudentRelationship().tap(r => {
			r.id = "1"
			r.agent = "111"
			r.targetSprCode = "1"
			r.profileService = profileService
		})

		val studentRel2 = new StudentRelationship().tap(r => {
			r.id = "2"
			r.agent = "222"
			r.targetSprCode = "2"
			r.profileService = profileService
		})

		// give them both the same surname, just to prove that we don't accidentally merge users when sorting
		val staff1 = new StaffMember(id = "111").tap(_.lastName = "Smith")
		val staff2 = new StaffMember(id = "222").tap(_.lastName = "Smith")

		profileService.getMemberByUniversityId("111") returns Some(staff1)
		profileService.getMemberByUniversityId("222") returns Some(staff2)
	}

	@Test //thisTestHasARidculouslyLongNameButICantThinkOfASensibleWayToShortenItWhichProbablyMeansTheCommandNeedsRefactoring...
	def applyCombinesStudentsInDepartmentWithStudentsWhoHaveTutorsInDepartment() {
		new Fixture {

			// two students in the department (sprCode 1, plus another one that we never meet)
			profileService.countStudentsByDepartment(departmentOfXXX) returns (2)

			// sprCode 1 is in the department, and has a tutor
			relationshipService.listStudentRelationshipsByDepartment(tutorRelType, departmentOfXXX) returns Seq(studentRel1)

			// sprCode 2 is not in the department, but does have a tutor in the department
			relationshipService.listStudentRelationshipsByStaffDepartment(tutorRelType, departmentOfXXX) returns Seq(studentRel2)

			val command = new ViewStudentRelationshipsCommand(departmentOfXXX, tutorRelType)

			command.profileService = profileService
			command.relationshipService = relationshipService


			//When I invoke the command
			val relationshipInfo = command.applyInternal()

			//Then I should get the results I expect
			relationshipInfo.studentMap(SortableAgentIdentifier("111",Some("Smith"))) should be(Seq(studentRel1))
			relationshipInfo.studentMap(SortableAgentIdentifier("222",Some("Smith"))) should be(Seq(studentRel2))
			relationshipInfo.studentCount should be(3) // sprCode1, sprCode2, and the other one
			relationshipInfo.missingCount should be(1) // the other one
		}
	}

}
