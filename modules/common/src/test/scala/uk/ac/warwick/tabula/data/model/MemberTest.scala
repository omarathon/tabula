package uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.RelationshipServiceImpl

class MemberTest extends PersistenceTestBase with Mockito {

	val profileService = mock[ProfileService]
	val relationshipService = mock[RelationshipService]

	@Test def testAffiliatedDepartments {
		val member = new StudentMember
		member.universityId = "01234567"

		// create their home department
		val homeDept = new Department
		homeDept.code = "zx"

		// create another department where they're taking a module
		val extDept = new Department
		extDept.code = "pi"

		// mock profile service to fetch list of registered modules (from these depts)
		val mod1 = new Module
		val mod2 = new Module
		mod1.department = extDept
		mod2.department = homeDept
		member.profileService = mock[ProfileService]
		member.profileService.getRegisteredModules(member.universityId) returns (Seq(mod1, mod2))

		// set home department and test
		member.homeDepartment = homeDept
		member.affiliatedDepartments should be (Stream(homeDept))
		member.touchedDepartments should be (Stream(homeDept, extDept))

		// set course department and test
		// create their course department
		val courseDept = new Department
		courseDept.code = "mn"

		val studentCourseDetails = new StudentCourseDetails(member, "2222222/2")
		studentCourseDetails.department = courseDept
		studentCourseDetails.mostSignificant = true
		studentCourseDetails.relationshipService = relationshipService

		member.studentCourseDetails.add(studentCourseDetails)


		member.mostSignificantCourseDetails.get.department = courseDept
		member.affiliatedDepartments should be (Stream(homeDept, courseDept))
		member.touchedDepartments should be (Stream(homeDept, courseDept, extDept))

		// also set route department and test
		val routeDept = new Department
		routeDept.code = "ch"
		val route = new Route
		route.department = routeDept
		member.mostSignificantCourseDetails.get.route = route

		member.affiliatedDepartments should be (Stream(homeDept, courseDept, routeDept))
		member.touchedDepartments should be (Stream(homeDept, courseDept, routeDept, extDept))

		// reset route to home, and check it appears only once
		route.department = homeDept
		member.mostSignificantCourseDetails.get.route = route

		member.affiliatedDepartments should be (Stream(homeDept, courseDept))
		member.touchedDepartments should be (Stream(homeDept, courseDept, extDept))
	}

	@Test def nullUsers {
		val member = new StaffMember
		member.fullName should be (None)

		member.lastName = "Bono"
		member.fullName should be (Some("Bono"))

		member.firstName = "Sonny"
		member.fullName should be (Some("Sonny Bono"))

		member.lastName = null
		member.fullName should be (Some("Sonny"))
	}

	@Test def fromCurrentUser = withUser("cuscav", "0672089") {
		currentUser.realUser.setFirstName("Mat")
		currentUser.realUser.setLastName("Mannion")
		currentUser.realUser.setEmail("M.Mannion@warwick.ac.uk")
		currentUser.realUser.setStaff(true)

		val member = new RuntimeMember(currentUser)
		member.userType should be (MemberUserType.Staff)
		member.userId should be ("cuscav")
		member.universityId should be ("0672089")
		member.firstName should be ("Mat")
		member.lastName should be ("Mannion")
		member.fullName should be (Some("Mat Mannion"))
		member.email should be ("M.Mannion@warwick.ac.uk")
		member.description should be ("")
		member.isStaff should be (true)
		member.isStudent should be (false)

		val user = member.asSsoUser
		user.getUserId should be ("cuscav")
		user.getWarwickId should be ("0672089")
		user.getFirstName should be ("Mat")
		user.getLastName should be ("Mannion")
		user.getFullName should be ("Mat Mannion")
		user.getEmail should be ("M.Mannion@warwick.ac.uk")
		user.getDepartment should be (null)
		user.getDepartmentCode should be (null)
		user.isFoundUser should be (true)
	}

	@Test def description = {
		val dept = Fixtures.department("in", "IT Services")

		val staff = new StaffMember
		staff.jobTitle = "Web Developer"
		staff.homeDepartment = dept

		staff.description should be ("Web Developer, IT Services")

		val route = Fixtures.route("G503", "MEng Computer Science")

		val student = new StudentMember
		student.groupName = "Undergraduate student"

		val studentCourseDetails = new StudentCourseDetails(student, "1111111/1")
		studentCourseDetails.route = route
		studentCourseDetails.mostSignificant = true

		student.studentCourseDetails.add(studentCourseDetails)
		student.homeDepartment = dept

		val desc = student.description
		student.description should be ("Undergraduate student, MEng Computer Science, IT Services")
	}

	@Test def isRelationshipAgent {
		val staff = new StaffMember
		staff.profileService = profileService
		staff.relationshipService = relationshipService
		
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		relationshipService.listStudentRelationshipsWithMember(relationshipType, staff) returns (Seq())
		staff.isRelationshipAgent(relationshipType) should be (false)

		relationshipService.listStudentRelationshipsWithMember(relationshipType, staff) returns (Seq(StudentRelationship("0672089", relationshipType, "0205225/1")))
		staff.isRelationshipAgent(relationshipType) should be (true)
	}

	@Test def deleteFileAttachmentOnDelete = transactional { tx =>
		// TAB-667
		val orphanAttachment =flushing(session){
			val attachment = new FileAttachment

			session.save(attachment)
			attachment
		}

		val (member, memberAttachment) = flushing(session){
			val member = new StudentMember
			member.universityId = "01234567"
			member.userId = "steve"

			val attachment = new FileAttachment
			member.photo = attachment

			session.save(member)
			(member, attachment)
		}

		// Ensure everything's been persisted
		orphanAttachment.id should not be (null)
		member.id should not be (null)
		memberAttachment.id should not be (null)

		// Can fetch everything from db
		flushing(session){
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			// Can't do an exact equality check because of Hibernate polymorphism
			session.get(classOf[Member], member.id).asInstanceOf[Member].universityId should be (member.universityId)
			session.get(classOf[FileAttachment], memberAttachment.id).asInstanceOf[FileAttachment].id should be (memberAttachment.id)
		}

		flushing(session){session.delete(member) }

		// Ensure we can't fetch the FeedbackTemplate or attachment, but all the other objects are returned
		flushing(session){
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[Member], member.id) should be (null)
			session.get(classOf[FileAttachment], memberAttachment.id) should be (null)
		}
	}
}
