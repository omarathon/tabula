package uk.ac.warwick.tabula.scheduling.commands.imports

import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.DegreeType.Postgraduate
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.scheduling.services.SupervisorImporter
import uk.ac.warwick.tabula.helpers.Logging
import org.joda.time.DateTime
import uk.ac.warwick.tabula.scheduling.helpers.ImportCommandFactory
import uk.ac.warwick.tabula.services.MaintenanceModeService
import uk.ac.warwick.tabula.data.MemberDao


class ImportSupervisorsForStudentCommandTest extends AppContextTestBase with Mockito with Logging {

	// I don't know why I need to duplicate these 2 traits here rather than importing them from ImportStudentRowCommandTest
	trait ImportCommandFactoryForTesting {
		val importCommandFactory = new ImportCommandFactory
		importCommandFactory.test = true

		var maintenanceModeService = smartMock[MaintenanceModeService]
		maintenanceModeService.enabled returns false
		importCommandFactory.maintenanceModeService = maintenanceModeService

		// needed for importCommandFactor for ImportStudentCourseCommand and also needed for ImportStudentRowCommand
		val memberDao = smartMock[MemberDao]
	}

	trait ImportSupervisorsCommandSetup extends ImportCommandFactoryForTesting {
		importCommandFactory.supervisorImporter = smartMock[SupervisorImporter]
	}

	trait Environment extends ImportSupervisorsCommandSetup {
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
			importCommandFactory.supervisorImporter.getSupervisorUniversityIds(scjCode, relationshipType) returns codes

			// test command
			val command = importCommandFactory.createImportSupervisorsForStudentCommand(studentCourseDetails)
			command.applyInternal()

			// check results
			val supRels = supervisee.freshStudentCourseDetails.head.relationships(relationshipType)
			supRels.size should be (1)
			val rel = supRels.head

			rel.agent should be (supervisorUniId)
			rel.studentMember should be (Some(supervisee))
			rel.relationshipType should be (relationshipType)
			(rel.percentage: BigDecimal) should be (100)
		}
	}

	@Transactional
	@Test def testCaptureInvalidSupervisor() {
		new Environment {
			// set up importer to return supervisor
			importCommandFactory.supervisorImporter.getSupervisorUniversityIds(scjCode, relationshipType) returns Seq()

			// test command
			val command = importCommandFactory.createImportSupervisorsForStudentCommand(studentCourseDetails)
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
			val codes = Seq((supervisorUniId, null))
			importCommandFactory.supervisorImporter.getSupervisorUniversityIds(scjCode, relationshipType) returns codes

			// test command
			val command = importCommandFactory.createImportSupervisorsForStudentCommand(studentCourseDetails)
			command.applyInternal()

			// check results
			val supRels = supervisee.freshStudentCourseDetails.head.relationships(relationshipType)
			supRels.size should be (1)
			val rel = supRels.head

			rel.agent should be (supervisorUniId)
			rel.studentMember should be (Some(supervisee))
			rel.relationshipType should be (relationshipType)
			rel.percentage should be (null)
		}
	}
}
