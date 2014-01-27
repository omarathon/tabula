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
		val supervisorUniId = "0070790"

		val relationshipType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")
		relationshipType.defaultSource = StudentRelationshipSource.SITS
		relationshipType.defaultRdxType = "SUP"
		session.saveOrUpdate(relationshipType)

		val department = new Department
		session.saveOrUpdate(department)

		// set up and persist student
		val supervisee = new StudentMember(uniId)
		supervisee.userId = "xxxxx"

		val studentCourseDetails = new StudentCourseDetails(supervisee, scjCode)
		studentCourseDetails.sprCode = sprCode
		studentCourseDetails.department = department

		supervisee.attachStudentCourseDetails(studentCourseDetails)

		val route = new Route
		route.degreeType = Postgraduate
		studentCourseDetails.route = route
		session.saveOrUpdate(route)

		session.saveOrUpdate(supervisee)

		// create and persist supervisor
		val supervisorMember = new StaffMember(supervisorUniId)
		supervisorMember.userId = "cusdx"
		session.saveOrUpdate(supervisorMember)
		val savedSup = session.get(classOf[StaffMember], supervisorUniId)
		logger.info("saved supervisor is " + savedSup)



	}

	@Transactional
	@Test def testCaptureValidSupervisor() {
		new Environment {
			// set up importer to return supervisor
			val codes = Seq((supervisorUniId, new java.math.BigDecimal("100")))
			val importer = smartMock[SupervisorImporter]
			importer.getSupervisorUniversityIds(scjCode, relationshipType) returns codes

			// test command
			val command = new ImportSupervisorsForStudentCommand()
			command.studentCourseDetails = studentCourseDetails
			command.supervisorImporter = importer
			command.applyInternal

			// check results
			val supRels = supervisee.freshStudentCourseDetails.head.relationships(relationshipType)
			supRels.size should be (1)
			val rel = supRels.head

			rel.agent should be (supervisorUniId)
			rel.targetSprCode should be (sprCode)
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
			command.studentCourseDetails = studentCourseDetails
			command.supervisorImporter = importer
			command.applyInternal

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
			val existingRelationship = StudentRelationship("1234", relationshipType, sprCode)
			existingRelationship.startDate = new DateTime
			session.saveOrUpdate(existingRelationship)

			// set up importer to return supervisor
			val codes = Seq((supervisorUniId, null))
			val importer = smartMock[SupervisorImporter]
			importer.getSupervisorUniversityIds(scjCode, relationshipType) returns codes


			// test command
			val command = new ImportSupervisorsForStudentCommand()
			command.studentCourseDetails = studentCourseDetails
			command.supervisorImporter = importer
			command.applyInternal

			// check results
			val supRels = supervisee.freshStudentCourseDetails.head.relationships(relationshipType)
			supRels.size should be (1)
			val rel = supRels.head

			rel.agent should be (supervisorUniId)
			rel.targetSprCode should be (sprCode)
			rel.relationshipType should be (relationshipType)
			rel.percentage should be (null)
		}
	}
}
