package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentServiceComponent, RelationshipServiceComponent, RelationshipService, ProfileServiceComponent, ProfileService}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.ScalaRestriction


class ViewRelatedStudentsCommandTest extends TestBase with Mockito {

	val member = new StaffMember("test")

	trait Fixture {
		val testDepartment = new Department
		testDepartment.name = "Department of Architecture and Explosions"
		testDepartment.code = "DA"

		val course = new Course
		course.code = "DA1"
		course.name = "Beginners Building Things"

		val testRoute1, testRoute2 = new Route

		testRoute1.code = "DA101"
		testRoute1.name = "101 Explosives"

		testRoute2.code = "DA102"
		testRoute2.name = "102 Clearing up"

		val courseDetails1, courseDetails2 = new StudentCourseDetails

		courseDetails1.department = testDepartment
		courseDetails1.route = testRoute1
		courseDetails1.course = course

		courseDetails2.department = testDepartment
		courseDetails2.route = testRoute2
		courseDetails2.course = course

		val member1, member2, member3  = new StudentMember()

		member1.mostSignificantCourse = courseDetails1
		member2.mostSignificantCourse = courseDetails1
		member3.mostSignificantCourse = courseDetails2

		val members = Seq(member1, member2, member3)
	}

	@Test
	def listsAllStudentsWithTutorRelationship() { new Fixture {

 			val mockProfileService = mock[ProfileService]
			val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
			val restrictions : Seq[ScalaRestriction] = Seq()

			mockProfileService.getStudentsByAgentRelationshipAndRestrictions(relationshipType, member, restrictions) returns members

			val command = new ViewRelatedStudentsCommandInternal(member, relationshipType) with ProfileServiceComponent {
				var profileService = mockProfileService
			}

			val result = command.applyInternal()

			result should be (members)
		}
	}

	@Test
	def listsAllStudentsWithSupervisorRelationship() { new Fixture {

			val mockProfileService = mock[ProfileService]

			val restrictions : Seq[ScalaRestriction] = Seq()
			val relationshipType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")

			mockProfileService.getStudentsByAgentRelationshipAndRestrictions(relationshipType, member, restrictions) returns members

			val command = new ViewRelatedStudentsCommandInternal(member, relationshipType) with ProfileServiceComponent {
				var profileService = mockProfileService
			}

		val result = command.applyInternal()

		result should be (members)

	}
}

}
