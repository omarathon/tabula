package uk.ac.warwick.tabula.scheduling.commands.imports

import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.DegreeType.Postgraduate
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.scheduling.services.SupervisorImporter
import uk.ac.warwick.tabula.helpers.Logging
import org.joda.time.DateTime
import org.junit.Ignore


class ImportSupervisorsForStudentCommandTest extends AppContextTestBase with Mockito with Logging {

	trait Environment {
		val scjCode = "1111111/1"
		val sprCode = "1111111/1"
		val uniId = "1111111"

		val relationshipType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")
		relationshipType.defaultSource = StudentRelationshipSource.SITS
		relationshipType.defaultRdxType = "SUP"
		session.saveOrUpdate(relationshipType)

		val department = new Department
		session.saveOrUpdate(department)

		// set up and persist student
		val supervisee = new StudentMember(uniId)
		supervisee.userId = "xxxxx"

		val course1 = new CourseFixture(scjCode, sprCode)

		session.saveOrUpdate(supervisee)

		val supervisor = newSupervisor("0070790", "cusdx")

		// create and persist supervisor
		def newSupervisor(uniId: String, userId: String) = {
			val supervisorMember = new StaffMember(uniId)
			supervisorMember.userId = userId
			session.saveOrUpdate(supervisorMember)
			supervisorMember
		}

		// Asserts that this course details is current and has exactly one relationship with
		// the given supervisor member.
		def assertRelationshipIsValid(scd: StudentCourseDetails, expectedSupervisor: StaffMember) {
			val supRels = scd.relationships(relationshipType)
			withClue(s"should be 1 item in ${supRels}") { supRels.size should be (1) }
			val rel = supRels.head

			rel.endDate should be (null)
			rel.agent should be (expectedSupervisor.universityId)
			rel.studentMember should be (Some(supervisee))
			rel.relationshipType should be (relationshipType)
			(rel.percentage: BigDecimal) should be (100)
		}

		class CourseFixture (scjCode: String, sprCode: String) {

			val studentCourseDetails = new StudentCourseDetails(supervisee, scjCode)
			studentCourseDetails.sprCode = sprCode
			studentCourseDetails.department = department

			supervisee.attachStudentCourseDetails(studentCourseDetails)

			val route = new Route
			route.degreeType = Postgraduate
			studentCourseDetails.route = route
			session.saveOrUpdate(route)


		}

	}

	@Transactional
	@Test def testImportConcurrentCourseSupervisors() {

		new Environment {
			val course2 = new CourseFixture("1111111/2", "1111111/2")
			session.save(course2.studentCourseDetails)

			val supervisor2 = newSupervisor("1273455","cusmbg")

			val importer = smartMock[SupervisorImporter]

			val codes1 = Seq((supervisor.universityId, new java.math.BigDecimal("100")))
			val codes2 = Seq((supervisor2.universityId, new java.math.BigDecimal("100")))

			importer.getSupervisorUniversityIds(course1.studentCourseDetails.scjCode, relationshipType) returns codes1
			importer.getSupervisorUniversityIds(course2.studentCourseDetails.scjCode, relationshipType) returns codes2

			val command = new ImportSupervisorsForStudentCommand()
			command.studentCourseDetails = course1.studentCourseDetails
			command.supervisorImporter = importer
			command.applyInternal()

			// check results
			assertRelationshipIsValid(course1.studentCourseDetails, supervisor)

			val command2 = new ImportSupervisorsForStudentCommand()
			command2.studentCourseDetails = course2.studentCourseDetails
			command2.supervisorImporter = importer
			command2.applyInternal()

			// check results
			assertRelationshipIsValid(course2.studentCourseDetails, supervisor2)

			// now check that course1's relationships have not been mangled
			// clear session so we're definitely getting it out of the DB
			session.flush()
			session.clear()
			val retrievedScd = session.get(classOf[StudentCourseDetails], course1.studentCourseDetails.id).asInstanceOf[StudentCourseDetails]
			assertRelationshipIsValid(retrievedScd, supervisor)

		}

	}

	@Transactional
	@Test def testCaptureValidSupervisor() {
		new Environment {
			// set up importer to return supervisor
			val codes = Seq((supervisor.universityId, new java.math.BigDecimal("100")))
			val importer = smartMock[SupervisorImporter]
			importer.getSupervisorUniversityIds(scjCode, relationshipType) returns codes

			// test command
			val command = new ImportSupervisorsForStudentCommand()
			command.studentCourseDetails = course1.studentCourseDetails
			command.supervisorImporter = importer
			command.applyInternal()

			// check results
			val supRels = supervisee.freshStudentCourseDetails.head.relationships(relationshipType)
			supRels.size should be (1)
			val rel = supRels.head

			rel.agent should be (supervisor.universityId)
			rel.studentMember should be (Some(supervisee))
			rel.relationshipType should be (relationshipType)
			(rel.percentage: BigDecimal) should be (100)
		}
	}

	@Transactional
	@Test def testCaptureInvalidSupervisor() {
		new Environment {
			// set up importer to return supervisor
			val importer = smartMock[SupervisorImporter]

			importer.getSupervisorUniversityIds(scjCode, relationshipType) returns Seq()

			// test command
			val command = new ImportSupervisorsForStudentCommand()
			command.studentCourseDetails = course1.studentCourseDetails
			command.supervisorImporter = importer
			command.applyInternal()

			// check results
			val supRels = supervisee.freshStudentCourseDetails.head.relationships(relationshipType)
			supRels.size should be (0)
		}
	}

	@Transactional
	@Test def testCaptureExistingOtherSupervisor() {
		new Environment {
			// create and persist existing supervisor
			val existingSupervisorMember = new StaffMember("1234")
			existingSupervisorMember.userId = "cusfal"
			session.saveOrUpdate(existingSupervisorMember)

			// create and persist existing relationship
			val existingRelationship = StudentRelationship(existingSupervisorMember, relationshipType, supervisee)
			existingRelationship.startDate = new DateTime
			session.saveOrUpdate(existingRelationship)

			// set up importer to return supervisor
			val codes = Seq((supervisor.universityId, null))
			val importer = smartMock[SupervisorImporter]
			importer.getSupervisorUniversityIds(scjCode, relationshipType) returns codes


			// test command
			val command = new ImportSupervisorsForStudentCommand()
			command.studentCourseDetails = course1.studentCourseDetails
			command.supervisorImporter = importer
			command.applyInternal()

			// check results
			val supRels = supervisee.freshStudentCourseDetails.head.relationships(relationshipType)
			supRels.size should be (1)
			val rel = supRels.head

			rel.agent should be (supervisor.universityId)
			rel.studentMember should be (Some(supervisee))
			rel.relationshipType should be (relationshipType)
			rel.percentage should be (null)
		}
	}
}
