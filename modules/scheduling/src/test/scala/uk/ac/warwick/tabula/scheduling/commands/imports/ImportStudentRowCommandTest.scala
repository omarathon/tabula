package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.{Date, ResultSet, ResultSetMetaData}

import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import org.springframework.beans.BeanWrapperImpl
import org.springframework.transaction.annotation.Transactional

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.{FileDao, MemberDao, ModeOfAttendanceDao, SitsStatusDao, StudentCourseDetailsDao, StudentCourseYearDetailsDao}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.Gender.Male
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.helpers.{ImportCommandFactory, ImportRowTracker}
import uk.ac.warwick.tabula.scheduling.services._
import uk.ac.warwick.tabula.services.{CourseAndRouteService, MaintenanceModeService, ModuleAndDepartmentService, ProfileService, ProfileServiceComponent, RelationshipService}
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.scheduling.services.MembershipMember
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation

trait ComponentMixins extends Mockito
		with ProfileServiceComponent
		with Tier4RequirementImporterComponent
		with ModeOfAttendanceImporterComponent {
	var profileService = smartMock[ProfileService]
	var tier4RequirementImporter = smartMock[Tier4RequirementImporter]
	var modeOfAttendanceImporter = smartMock[ModeOfAttendanceImporter]
}

trait ImportCommandFactoryForTesting extends ComponentMixins {
	val importCommandFactory = new ImportCommandFactory
	importCommandFactory.test = true

	var maintenanceModeService = smartMock[MaintenanceModeService]
	maintenanceModeService.enabled returns false
	importCommandFactory.maintenanceModeService = maintenanceModeService

	// needed for importCommandFactor for ImportStudentCourseCommand and also needed for ImportStudentRowCommand
	val memberDao = smartMock[MemberDao]
}

trait ImportStudentCourseCommandSetup extends ImportCommandFactoryForTesting with PropertyCopyingSetup {
	importCommandFactory.memberDao = memberDao

	val relationshipService = smartMock[RelationshipService]
	relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns (None)
	importCommandFactory.relationshipService = relationshipService

	importCommandFactory.studentCourseDetailsDao = smartMock[StudentCourseDetailsDao]

	val courseAndRouteService = smartMock[CourseAndRouteService]
	val route = smartMock[Route]
	courseAndRouteService.getRouteByCode("c100") returns (Some(route))
	importCommandFactory.courseAndRouteService = courseAndRouteService

	val courseImporter = smartMock[CourseImporter]
	courseImporter.getCourseForCode("UESA-H612") returns new Course("UESA-H612", "Computer Systems Engineering MEng")
	importCommandFactory.courseImporter = courseImporter

	val awardImporter: AwardImporter = smartMock[AwardImporter]
}

trait PropertyCopyingSetup extends ImportCommandFactoryForTesting {
	val sitsStatusImporter = smartMock[SitsStatusImporter]
	sitsStatusImporter.getSitsStatusForCode("F") returns  Some(new SitsStatus("F", "F", "Fully Enrolled"))
	sitsStatusImporter.getSitsStatusForCode("P") returns  Some(new SitsStatus("P", "P", "Permanently Withdrawn"))
	importCommandFactory.sitsStatusImporter = sitsStatusImporter

	val department = new Department
	department.code = "ph"
	department.name = "Philosophy"

	val modAndDeptService = smartMock[ModuleAndDepartmentService]
	modAndDeptService.getDepartmentByCode("ph") returns (Some(department))
	importCommandFactory.modAndDeptService = modAndDeptService
}

trait ImportStudentCourseYearCommandSetup extends ImportCommandFactoryForTesting {
	modeOfAttendanceImporter.modeOfAttendanceMap returns Map(
		"F" -> new ModeOfAttendance("F", "FT", "Full Time"),
		"P" -> new ModeOfAttendance("P", "PT", "Part Time")
	)
	modeOfAttendanceImporter.getModeOfAttendanceForCode("P") returns Some(new ModeOfAttendance("P", "PT", "Part Time"))
	importCommandFactory.modeOfAttendanceImporter = modeOfAttendanceImporter

	importCommandFactory.profileService = smartMock[ProfileService]

	importCommandFactory.studentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]

	importCommandFactory.moaDao = smartMock[ModeOfAttendanceDao]

}

trait ImportSupervisorsCommandSetup extends ImportCommandFactoryForTesting {
	importCommandFactory.supervisorImporter = smartMock[SupervisorImporter]
}

trait ImportCommandFactorySetup
	extends ComponentMixins
	with ImportStudentCourseCommandSetup
	with ImportStudentCourseYearCommandSetup
	with ImportSupervisorsCommandSetup {}

trait MockedResultSet extends Mockito {
	val rs = smartMock[ResultSet]
	val rsMetaData = smartMock[ResultSetMetaData]
	rs.getMetaData() returns(rsMetaData)

	rsMetaData.getColumnCount() returns(4)
	rsMetaData.getColumnName(1) returns("gender")
	rsMetaData.getColumnName(2) returns("year_of_study")
	rsMetaData.getColumnName(3) returns("spr_code")
	rsMetaData.getColumnName(4) returns("route_code")

	rs.getString("gender") returns("M")
	rs.getInt("year_of_study") returns(3)
	rs.getString("spr_code") returns("0672089/2")
	rs.getString("route_code") returns("C100")
	rs.getString("spr_tutor1") returns ("0070790")
	rs.getString("homeDepartmentCode") returns ("PH")
	rs.getString("department_code") returns ("PH")
	rs.getString("scj_code") returns ("0672089/2")
	rs.getDate("begin_date") returns Date.valueOf("2011-05-12")
	rs.getDate("end_date") returns Date.valueOf("2014-05-12")
	rs.getDate("expected_end_date") returns Date.valueOf("2015-05-12")
	rs.getInt("sce_sequence_number") returns (1)
	rs.getString("enrolment_status_code") returns ("F")
	rs.getString("mode_of_attendance_code") returns ("P")
	rs.getString("sce_academic_year") returns ("10/11")
	rs.getString("most_signif_indicator") returns ("Y")
	rs.getString("mod_reg_status") returns ("CON")
	rs.getString("course_code") returns ("UESA-H612")
	rs.getString("disability") returns ("Q")
}

// scalastyle:off magic.number
class ImportStudentRowCommandTest extends TestBase with Mockito with Logging {
	EventHandling.enabled = false

	trait MemberSetup {
		val mm = MembershipMember(
			universityId 			= "0672089",
			departmentCode			= "ph",
			email					= "M.Mannion@warwick.ac.uk",
			targetGroup				= null,
			title					= "Mr",
			preferredForenames		= "Mathew",
			preferredSurname		= "Mannion",
			position				= null,
			dateOfBirth				= new LocalDate(1984, DateTimeConstants.AUGUST, 19),
			usercode				= "cuscav",
			startDate				= null,
			endDate					= null,
			modified				= null,
			phoneNumber				= null,
			gender					= null,
			alternativeEmailAddress	= null,
			userType				= Student)
	}

	trait Environment extends ImportCommandFactorySetup
			with MockedResultSet
			with MemberSetup {

		val blobBytes = Array[Byte](1,2,3,4,5)
		val mac = MembershipInformation(mm, () => Some(blobBytes))

		val rowCommand = new ImportStudentRowCommandInternal(mac, new AnonymousUser(), rs, importCommandFactory) with ComponentMixins
		rowCommand.memberDao = memberDao
		rowCommand.fileDao = smartMock[FileDao]
		rowCommand.moduleAndDepartmentService = modAndDeptService

		val row = new SitsStudentRow(rs)

		// only return a known disability for code Q
		val disabilityQ = new Disability("Q", "Test disability")
		profileService.getDisability(any[String]) returns (None)
		profileService.getDisability("Q") returns (Some(disabilityQ))
		rowCommand.profileService = profileService

		tier4RequirementImporter.hasTier4Requirement("0672089") returns (false)
		rowCommand.tier4RequirementImporter = tier4RequirementImporter
	}

	@Test def testImportStudentCourseYearCommand {
		new Environment {
			val studentCourseDetails = new StudentCourseDetails
			studentCourseDetails.scjCode = "0672089/2"
			studentCourseDetails.sprCode = "0672089/2"

			val yearCommand = importCommandFactory.createImportStudentCourseYearCommand(row, studentCourseDetails)

			// now the set up is done, run the apply command and test it:
			val studentCourseYearDetails = yearCommand.applyInternal()

			// and check stuff:
			studentCourseYearDetails.academicYear.toString should be ("10/11")
			studentCourseYearDetails.sceSequenceNumber should be (1)
			studentCourseYearDetails.enrolmentStatus.code should be ("F")
			studentCourseYearDetails.lastUpdatedDate should not be null
			studentCourseYearDetails.modeOfAttendance.code should be ("P")
			studentCourseYearDetails.yearOfStudy should be (3)

			there was one(importCommandFactory.studentCourseYearDetailsDao).saveOrUpdate(any[StudentCourseYearDetails]);
		}
	}

	@Test def testImportStudentCourseCommand {
		new Environment {
			// first set up the studentCourseYearDetails as above
			var studentCourseDetails = new StudentCourseDetails
			studentCourseDetails.scjCode = "0672089/2"
			studentCourseDetails.sprCode = "0672089/2"

			val courseCommand = importCommandFactory.createImportStudentCourseCommand(row, smartMock[StudentMember])

			importCommandFactory.relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns (None)

			// now the set up is done, run the apply command and test it:
			studentCourseDetails = courseCommand.applyInternal()

			// now test some stuff
			studentCourseDetails.scjCode should be ("0672089/2")
			studentCourseDetails.beginDate.toString should be ("2011-05-12")
			studentCourseDetails.endDate.toString should be ("2014-05-12")
			studentCourseDetails.expectedEndDate.toString should be ("2015-05-12")

			studentCourseDetails.freshStudentCourseYearDetails.size should be (1)

			there was one(importCommandFactory.studentCourseDetailsDao).saveOrUpdate(any[StudentCourseDetails]);
		}
	}

	@Test def testMarkAsSeenInSits {
		new Environment {

			// first set up the studentCourseYearDetails as above
			var studentCourseDetails = new StudentCourseDetails
			studentCourseDetails.scjCode = "0672089/2"
			studentCourseDetails.sprCode = "0672089/2"

			val studentCourseDetailsBean = new BeanWrapperImpl(studentCourseDetails)

			studentCourseDetails.missingFromImportSince should be (null)

			rowCommand.applyInternal() match {
				case stuMem: StudentMember => {
					val courseCommand = importCommandFactory.createImportStudentCourseCommand(row, stuMem)
					courseCommand.markAsSeenInSits(studentCourseDetailsBean) should be (false)
					studentCourseDetails.missingFromImportSince should be (null)
					studentCourseDetails.missingFromImportSince = DateTime.now
					studentCourseDetails.missingFromImportSince should not be (null)
					courseCommand.markAsSeenInSits(studentCourseDetailsBean) should be (true)
					studentCourseDetails.missingFromImportSince should be (null)
				}
				case _ => 1 should be (0)
			}
		}
	}

	@Test
	def testImportStudentRowCommandWorksWithNew {
		new Environment {
			relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns (None)

			// now the set-up is done, run the apply command for member, which should cascade and run the other apply commands:
			val member = rowCommand.applyInternal()

			// test that member contains the expected data:
			member.title should be ("Mr")
			member.universityId should be ("0672089")
			member.userId should be ("cuscav")
			member.email should be ("M.Mannion@warwick.ac.uk")
			member.gender should be (Male)
			member.firstName should be ("Mathew")
			member.lastName should be ("Mannion")
			member.photo should not be (null)
			member.dateOfBirth should be (new LocalDate(1984, DateTimeConstants.AUGUST, 19))

			member match {
				case stu: StudentMember => {
					stu.freshStudentCourseDetails.size should be (1)
					stu.freshStudentCourseDetails.head.freshStudentCourseYearDetails.size should be (1)
				}
				case _ => false should be (true)
			}

			//there was one(fileDao).savePermanent(any[FileAttachment])
			//there was no(fileDao).saveTemporary(any[FileAttachment])
			there was one(memberDao).saveOrUpdate(any[Member])
		}
	}

	@Test
	def worksWithExistingMember {
		new Environment {
			val existing = new StudentMember("0672089")
			memberDao.getByUniversityId("0672089") returns(Some(existing))

			relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns (None)

			// now the set-up is done, run the apply command for member, which should cascade and run the other apply commands:
			val member = rowCommand.applyInternal()
			member match {
				case stu: StudentMember => {
					stu.freshStudentCourseDetails.size should be (1)
					stu.freshStudentCourseDetails.head.freshStudentCourseYearDetails.size should be (1)
				}
				case _ => false should be (true)
			}

			//there was one(fileDao).savePermanent(any[FileAttachment])
			//there was no(fileDao).saveTemporary(any[FileAttachment])
			there was one(memberDao).saveOrUpdate(any[Member])
		}
	}

	@Transactional
	@Test def testCaptureTutorIfSourceIsLocal {

		new Environment {
			val existing = new StudentMember("0672089")
			val existingStaffMember = new StaffMember("0070790")

			memberDao.getByUniversityId("0070790") returns(Some(existingStaffMember))
			memberDao.getByUniversityId("0672089") returns(Some(existing))

			val tutorRelationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

			relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns (Some(tutorRelationshipType))

			// if personalTutorSource is "local", there should be no update
			department.setStudentRelationshipSource(tutorRelationshipType, StudentRelationshipSource.Local)

			val member = rowCommand.applyInternal() match {
				case stu: StudentMember => Some(stu)
				case _ => None
			}

			val studentMember = member.get

			studentMember.freshStudentCourseDetails.size should not be (0)

			there was no(relationshipService).replaceStudentRelationships(tutorRelationshipType, studentMember.mostSignificantCourseDetails.get, Seq(existingStaffMember))
		}
	}

	@Transactional
	@Test def testCaptureTutorIfSourceIsSits {

		new Environment {
			val existing = new StudentMember("0672089")
			val existingStaffMember = new StaffMember("0070790")

			val tutorRelationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

			importCommandFactory.relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns (Some(tutorRelationshipType))

			// if personalTutorSource is "SITS", there *should* an update
			department.setStudentRelationshipSource(tutorRelationshipType, StudentRelationshipSource.SITS)

			memberDao.getByUniversityId("0070790") returns(Some(existingStaffMember))
			memberDao.getByUniversityId("0672089") returns(Some(existing))
			
			importCommandFactory.relationshipService.findCurrentRelationships(tutorRelationshipType, existing) returns (Nil)

			val member = rowCommand.applyInternal() match {
				case stu: StudentMember => Some(stu)
				case _ => None
			}
			
			val studentMember = member.get

			studentMember.mostSignificantCourseDetails should not be (null)

			there was one(relationshipService).replaceStudentRelationships(tutorRelationshipType, studentMember.mostSignificantCourseDetails.get, Seq(existingStaffMember))
		}
	}

	@Test def testDisabilityHandling {
		new Environment {
			var student = rowCommand.applyInternal().asInstanceOf[StudentMember]
			student.disability should be (disabilityQ)

			// override to test for attempted import of unknown disability
			rs.getString("disability") returns ("Mystery")
			student = rowCommand.applyInternal().asInstanceOf[StudentMember]
			student.disability should be (null)
		}
	}
}

