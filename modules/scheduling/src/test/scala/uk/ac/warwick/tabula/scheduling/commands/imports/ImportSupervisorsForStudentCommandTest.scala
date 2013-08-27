package uk.ac.warwick.tabula.scheduling.commands.imports

import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model.DegreeType.Postgraduate
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.scheduling.services.SupervisorImporter
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.data.model.StudentRelationshipSource
import uk.ac.warwick.tabula.data.model.Department


class ImportSupervisorsForStudentCommandTest extends AppContextTestBase with Mockito with Logging {

	trait Environment {
		val scjCode = "1111111/1"
		val sprCode = "1111111/1"
		val uniId = "1111111"
		val prsCode = "IN0070790"
		val supervisorUniId = "0070790"
			
		val relationshipType = new StudentRelationshipType
		relationshipType.id = "supervisor"
		relationshipType.defaultSource = StudentRelationshipSource.SITS
		session.saveOrUpdate(relationshipType)
		
		val department = new Department
		session.saveOrUpdate(department)

		// set up and persist student
		val supervisee = new StudentMember(uniId)
		supervisee.userId = "xxxxx"
		//supervisee.studyDetails.scjCode = scjCode
		//supervisee.studyDetails.sprCode = sprCode

		val studentCourseDetails = new StudentCourseDetails(supervisee, scjCode)
		studentCourseDetails.sprCode = sprCode
		studentCourseDetails.department = department
		
		supervisee.studentCourseDetails.add(studentCourseDetails)

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
	@Test def testCaptureValidSupervisor {
		new Environment {
			// set up importer to return supervisor
			val codes = Seq(prsCode)
			val importer = smartMock[SupervisorImporter]
			importer.getSupervisorPrsCodes(scjCode) returns codes

			// test command
			val command = new ImportSupervisorsForStudentCommand()
			command.studentCourseDetails = studentCourseDetails
			command.supervisorImporter = importer
			command.applyInternal

			// check results
			val supRels = supervisee.studentCourseDetails.get(0).agents(relationshipType)
			supRels.size should be (1)
			val rel = supRels.head

			rel.agent should be (supervisorUniId)
			rel.targetSprCode should be (sprCode)
			rel.relationshipType should be (relationshipType)
		}
	}

	@Transactional
	@Test def testCaptureInvalidSupervisor {
		new Environment {
			// set up importer to return supervisor
			val importer = smartMock[SupervisorImporter]
			importer.getSupervisorPrsCodes(scjCode) returns Seq()

			// test command
			val command = new ImportSupervisorsForStudentCommand()
			command.studentCourseDetails = studentCourseDetails
			command.supervisorImporter = importer
			command.applyInternal

			// check results
			val supRels = supervisee.studentCourseDetails.get(0).agents(relationshipType)
			supRels.size should be (0)
		}
	}
}
