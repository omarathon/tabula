package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{Mockito, TestBase}


class ViewRelatedStudentsCommandTest extends TestBase with Mockito {

	val staffMember = new StaffMember("test")

	trait Fixture {
		val testDepartment = new Department
		testDepartment.fullName = "Department of Architecture and Explosions"
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
		courseDetails1.currentRoute = testRoute1
		courseDetails1.course = course

		courseDetails2.department = testDepartment
		courseDetails2.currentRoute = testRoute2
		courseDetails2.course = course
	}

	@Test
	def listsAllStudentsWithTutorRelationship() { new Fixture {
		val mockProfileService = mock[ProfileService]
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val restrictions : Seq[ScalaRestriction] = Seq()

		mockProfileService.getSCDsByAgentRelationshipAndRestrictions(relationshipType, staffMember, restrictions) returns Seq(courseDetails1, courseDetails2)

		val command = new ViewRelatedStudentsCommandInternal(staffMember, relationshipType) with ProfileServiceComponent {
			var profileService = mockProfileService
		}

		val result = command.applyInternal()

		result should be (Seq(courseDetails1, courseDetails2))
	}}

	@Test
	def listsAllStudentsWithSupervisorRelationship() { new Fixture {
		val mockProfileService = mock[ProfileService]

		val restrictions : Seq[ScalaRestriction] = Seq()
		val relationshipType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")

		mockProfileService.getSCDsByAgentRelationshipAndRestrictions(relationshipType, staffMember, restrictions) returns Seq(courseDetails1, courseDetails2)

		val command = new ViewRelatedStudentsCommandInternal(staffMember, relationshipType) with ProfileServiceComponent {
			var profileService = mockProfileService
		}

		val result = command.applyInternal()

		result should be (Seq(courseDetails1, courseDetails2))

	}}

	@Test
	def helperFunctions() { new Fixture {
		val mockProfileService = mock[ProfileService]

		val relationshipType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")

		mockProfileService.getSCDsByAgentRelationshipAndRestrictions(relationshipType, staffMember, Nil) returns Seq(courseDetails1, courseDetails2)

		val command = new ViewRelatedStudentsCommandInternal(staffMember, relationshipType) with ProfileServiceComponent {
			var profileService = mockProfileService
		}

		command.allCourses should be (Seq(courseDetails1, courseDetails2))
		command.allDepartments should be (Seq(testDepartment))
		command.allRoutes should be (Seq(testRoute1, testRoute2))
	}}

}
