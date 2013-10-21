package uk.ac.warwick.tabula.profiles

import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.Mockito
import org.mockito.Mockito.when
import uk.ac.warwick.tabula.services.ProfileService
import scala.Some

trait TutorFixture extends Mockito {
	
	val tutorRelationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

	val department = new Department
	department.setStudentRelationshipSource(tutorRelationshipType, StudentRelationshipSource.Local)
	val actor = new User
	val recipient = new User
	recipient.setWarwickId("recipient")

	val student = new StudentMember
	student.universityId = "student"
		
	val studentCourseDetails = new StudentCourseDetails
	studentCourseDetails.student = student
	studentCourseDetails.department = department
	studentCourseDetails.sprCode = "0000001/1"
	studentCourseDetails.mostSignificant = true
	student.studentCourseDetails.add(studentCourseDetails)
	student.mostSignificantCourse = studentCourseDetails

//	val courseDetails = new StudentCourseDetails()
//	courseDetails.sprCode = "spr-123"
//	courseDetails.mostSignificant = true
//	courseDetails.department = department
//	student.studentCourseDetails.add(courseDetails)

	val newTutor = new StaffMember
	newTutor.universityId = "0000001"
	val oldTutor = new StaffMember
	oldTutor.universityId = "0000002"

	val profileService = mock[ProfileService]
	profileService.getStudentBySprCode("student") returns Some(student)
	profileService.getMemberByUniversityId("0000001") returns Some(newTutor)
	profileService.getMemberByUniversityId("0000002") returns Some(oldTutor)

	val relationship = new StudentRelationship
	relationship.targetSprCode = "student"
	relationship.agent = "0000001"
	relationship.profileService = profileService
}
