package uk.ac.warwick.tabula.commands.scheduling.imports

import java.sql.{Date, ResultSetMetaData, ResultSet}

import org.joda.time.{DateTimeConstants, LocalDate, DateTime}
import org.springframework.beans.BeanWrapperImpl
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.model.Gender.Male
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{ModeOfAttendanceDao, StudentCourseYearDetailsDao, StudentCourseDetailsDao, MemberDao}
import uk.ac.warwick.tabula.helpers.scheduling.{SitsStudentRow, ImportCommandFactory}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.scheduling._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.AnonymousUser

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
	relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns None
	importCommandFactory.relationshipService = relationshipService

	importCommandFactory.studentCourseDetailsDao = smartMock[StudentCourseDetailsDao]

	val courseAndRouteService = smartMock[CourseAndRouteService]
	val route = smartMock[Route]
	courseAndRouteService.getRouteByCode("C100") returns Some(new Route("c100", smartMock[Department]))
	courseAndRouteService.getRouteByCode(null) returns None
	importCommandFactory.courseAndRouteService = courseAndRouteService

	val courseImporter = smartMock[CourseImporter]
	courseImporter.getCourseByCodeCached("UESA-H612") returns Some(new Course("UESA-H612", "Computer Systems Engineering MEng"))
	courseImporter.getCourseByCodeCached(null) returns None
	importCommandFactory.courseImporter = courseImporter

	val awardImporter: AwardImporter = smartMock[AwardImporter]
	awardImporter.getAwardByCodeCached("BA") returns Some(new Award("BA", "Bachelor of Arts"))
	awardImporter.getAwardByCodeCached("") returns None
	importCommandFactory.awardImporter = awardImporter
}

trait PropertyCopyingSetup extends ImportCommandFactoryForTesting {
	val sitsStatusImporter = smartMock[SitsStatusImporter]
	sitsStatusImporter.getSitsStatusForCode("F") returns  Some(new SitsStatus("F", "F", "Fully Enrolled"))
	sitsStatusImporter.getSitsStatusForCode("P") returns  Some(new SitsStatus("P", "P", "Permanently Withdrawn"))
	importCommandFactory.sitsStatusImporter = sitsStatusImporter

	val department = new Department
	department.code = "ph"
	department.fullName = "Philosophy"

	val modAndDeptService = smartMock[ModuleAndDepartmentService]
	modAndDeptService.getDepartmentByCode("ph") returns Some(department)
	modAndDeptService.getDepartmentByCode("PH") returns Some(department)
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

trait ImportCommandFactorySetup
	extends ComponentMixins
	with ImportStudentCourseCommandSetup
	with ImportStudentCourseYearCommandSetup {}

trait MockedResultSet extends Mockito {
	val rs = smartMock[ResultSet]
	val rsMetaData = smartMock[ResultSetMetaData]
	rs.getMetaData returns rsMetaData

	rsMetaData.getColumnCount returns 4
	rsMetaData.getColumnName(1) returns "gender"
	rsMetaData.getColumnName(2) returns "year_of_study"
	rsMetaData.getColumnName(3) returns "spr_code"
	rsMetaData.getColumnName(4) returns "route_code"

	rs.getString("gender") returns "M"
	rs.getInt("year_of_study") returns 3
	rs.getString("spr_code") returns "0672089/2"
	rs.getString("route_code") returns "C100"
	rs.getString("spr_tutor1") returns "0070790"
	rs.getString("homeDepartmentCode") returns "PH"
	rs.getString("department_code") returns "PH"
	rs.getString("scj_code") returns "0672089/2"
	rs.getDate("begin_date") returns new Date(DateTime.now.minusYears(2).getMillis)
	rs.getDate("end_date") returns new Date(DateTime.now.plusYears(1).getMillis)
	rs.getDate("expected_end_date") returns new Date(DateTime.now.plusYears(2).getMillis)
	rs.getInt("sce_sequence_number") returns 1
	rs.getString("enrolment_status_code") returns "F"
	rs.getString("mode_of_attendance_code") returns "P"
	rs.getString("sce_academic_year") returns "10/11"
	rs.getString("most_signif_indicator") returns "Y"
	rs.getString("mod_reg_status") returns "CON"
	rs.getString("course_code") returns "UESA-H612"
	rs.getString("disability") returns "Q"
	rs.getString("award_code") returns "BA"
	rs.getBigDecimal("sce_agreed_mark") returns new JBigDecimal(66.666666)
}

// scalastyle:off magic.number
class ImportStudentRowCommandTest extends TestBase with Mockito with Logging {

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

	trait EnvironmentWithoutResultSet extends ImportCommandFactorySetup
	with MemberSetup {
		val rs: ResultSet

		val mac = MembershipInformation(mm)

		// only return a known disability for code Q
		val disabilityQ = new Disability("Q", "Test disability")
		profileService.getDisability(any[String]) returns None
		profileService.getDisability(null) returns None
		profileService.getDisability("Q") returns Some(disabilityQ)

		tier4RequirementImporter.hasTier4Requirement("0672089") returns false

		val rowCommand = new ImportStudentRowCommandInternal(mac, new AnonymousUser(), rs, importCommandFactory) with ComponentMixins
		rowCommand.memberDao = memberDao
		rowCommand.moduleAndDepartmentService = modAndDeptService
		rowCommand.profileService = profileService
		rowCommand.tier4RequirementImporter = tier4RequirementImporter

		val row = new SitsStudentRow(rs)
	}

	trait Environment extends MockedResultSet with EnvironmentWithoutResultSet

	/** When a SPR is (P)ermanently withdrawn, end relationships
		* FOR THAT ROUTE ONLY
		*/
	@Test def endingWithdrawnRouteRelationships() {
		new Environment {
			val student = new StudentMember()

			def createRelationship(sprCode: String, scjCode: String) = {
				val rel = new MemberStudentRelationship()
				rel.studentMember = student
				val scd = new StudentCourseDetails()
				scd.scjCode = scjCode
				scd.sprCode = sprCode
				rel.studentCourseDetails = scd
				rel
			}

			val rel1 = createRelationship(sprCode="1111111/1", scjCode="1111111/1")
			val rel2 = createRelationship(sprCode="1111111/2", scjCode="1111111/2")
			val rel3 = createRelationship(sprCode="1111111/1", scjCode="1111111/3")
			relationshipService.getAllCurrentRelationships(student) returns Seq(rel1, rel2, rel3)

			row.sprCode = "1111111/1"
			row.sprStatusCode = "P"
			row.endDate = new DateTime().minusMonths(6).toLocalDate

			val courseCommand = importCommandFactory.createImportStudentCourseCommand(row, student)
			courseCommand.applyInternal()

			rel1.endDate.toLocalDate should be (row.endDate)
			assertResult(null, "Shouldn't end course that's on a different route")( rel2.endDate )
			rel3.endDate.toLocalDate should be (row.endDate)
		}
	}

	@Test def testImportStudentCourseYearCommand() {
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
			studentCourseYearDetails.agreedMark.toPlainString should be ("66.7") // Should be rounded up

			verify(importCommandFactory.studentCourseYearDetailsDao, times(1)).saveOrUpdate(any[StudentCourseYearDetails])
		}
	}

	@Test def testImportStudentCourseCommand() {
		new Environment {
			// first set up the studentCourseYearDetails as above
			var studentCourseDetails = new StudentCourseDetails
			studentCourseDetails.scjCode = "0672089/2"
			studentCourseDetails.sprCode = "0672089/2"

			val courseCommand = importCommandFactory.createImportStudentCourseCommand(row, smartMock[StudentMember])

			importCommandFactory.relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns None

			// now the set up is done, run the apply command and test it:
			studentCourseDetails = courseCommand.applyInternal()

			// now test some stuff
			studentCourseDetails.scjCode should be ("0672089/2")
			studentCourseDetails.beginDate should be (row.beginDate)
			studentCourseDetails.endDate should be (row.endDate)
			studentCourseDetails.expectedEndDate should be (row.expectedEndDate)

			studentCourseDetails.freshStudentCourseYearDetails.size should be (1)

			verify(importCommandFactory.studentCourseDetailsDao, times(1)).saveOrUpdate(any[StudentCourseDetails])
		}
	}

	@Test def testMarkAsSeenInSits() {
		new Environment {

			// first set up the studentCourseYearDetails as above
			var studentCourseDetails = new StudentCourseDetails
			studentCourseDetails.scjCode = "0672089/2"
			studentCourseDetails.sprCode = "0672089/2"

			val studentCourseDetailsBean = new BeanWrapperImpl(studentCourseDetails)

			studentCourseDetails.missingFromImportSince should be (null)

			rowCommand.applyInternal() match {
				case stuMem: StudentMember =>
					val courseCommand = importCommandFactory.createImportStudentCourseCommand(row, stuMem)
					courseCommand.markAsSeenInSits(studentCourseDetailsBean) should be {false}
					studentCourseDetails.missingFromImportSince should be (null)
					studentCourseDetails.missingFromImportSince = DateTime.now
					studentCourseDetails.missingFromImportSince should not be null
					courseCommand.markAsSeenInSits(studentCourseDetailsBean) should be {true}
					studentCourseDetails.missingFromImportSince should be (null)
				case _ => 1 should be (0)
			}
		}
	}

	@Test
	def testImportStudentRowCommandWorksWithNew() {
		new Environment {
			relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns None

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
			member.dateOfBirth should be (new LocalDate(1984, DateTimeConstants.AUGUST, 19))

			member match {
				case stu: StudentMember =>
					stu.disability.get.code should be ("Q")
					stu.freshStudentCourseDetails.size should be (1)
					stu.freshStudentCourseDetails.head.freshStudentCourseYearDetails.size should be (1)
					stu.mostSignificantCourse.sprCode should be ("0672089/2")
					val scd = stu.freshStudentCourseDetails.head
					scd.course.code should be ("UESA-H612")
					scd.scjCode should be ("0672089/2")
					scd.department.code should be ("ph")
					scd.route.code should be ("c100")
					scd.sprCode should be ("0672089/2")
					scd.beginDate should be (row.beginDate)
					scd.endDate should be (row.endDate)
					scd.expectedEndDate should be (row.expectedEndDate)

					val scyd = scd.freshStudentCourseYearDetails.head
					scyd.yearOfStudy should be (3)
					scyd.sceSequenceNumber should be (1)
					scyd.enrolmentStatus.code should be ("F")
					scyd.modeOfAttendance.code should be ("P")
					scyd.academicYear.toString should be ("10/11")
					scyd.moduleRegistrationStatus.dbValue should be ("CON")
				case _ => false should be {true}
			}

			verify(memberDao, times(1)).saveOrUpdate(any[Member])
		}
	}

	trait MockedResultSetWithNulls extends Mockito {
		val rs = smartMock[ResultSet]
		val rsMetaData = smartMock[ResultSetMetaData]
		rs.getMetaData returns rsMetaData

		rsMetaData.getColumnCount returns 4
		rsMetaData.getColumnName(1) returns "gender"
		rsMetaData.getColumnName(2) returns "year_of_study"
		rsMetaData.getColumnName(3) returns "spr_code"
		rsMetaData.getColumnName(4) returns "route_code"

		rs.getString("gender") returns "M"
		rs.getInt("year_of_study") returns 3
		rs.getString("spr_code") returns "0672089/2"
		rs.getString("route_code") returns null
		rs.getString("spr_tutor1") returns null
		rs.getString("homeDepartmentCode") returns null
		rs.getString("department_code") returns null
		rs.getString("scj_code") returns "0672089/2"
		rs.getDate("begin_date") returns null
		rs.getDate("end_date") returns null
		rs.getDate("expected_end_date") returns null
		rs.getInt("sce_sequence_number") returns 1
		rs.getString("enrolment_status_code") returns null
		rs.getString("mode_of_attendance_code") returns null
		rs.getString("sce_academic_year") returns "10/11"
		rs.getString("most_signif_indicator") returns "Y"
		rs.getString("mod_reg_status") returns null
		rs.getString("course_code") returns null
		rs.getString("disability") returns null
		rs.getBigDecimal("sce_agreed_mark") returns null
	}

	trait EnvironmentWithNulls extends MockedResultSetWithNulls with EnvironmentWithoutResultSet

	@Test
	def testImportStudentRowCommandWorksWithNulls() {
		new EnvironmentWithNulls {
			relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns None

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
			member.dateOfBirth should be (new LocalDate(1984, DateTimeConstants.AUGUST, 19))

			member match {
				case stu: StudentMember =>
					stu.disability should be (None)
					stu.freshStudentCourseDetails.size should be (1)
					stu.freshStudentCourseDetails.head.freshStudentCourseYearDetails.size should be (1)
					stu.mostSignificantCourse.sprCode should be ("0672089/2")
					val scd = stu.freshStudentCourseDetails.head
					scd.course should be (null)
					scd.department should be (null)
					scd.route should be (null)
					scd.beginDate should be (null)
					scd.endDate should be (null)
					scd.expectedEndDate should be (null)

					val scyd = scd.freshStudentCourseYearDetails.head
					scyd.enrolmentStatus should be (null)
					scyd.modeOfAttendance should be (null)
					scyd.moduleRegistrationStatus should be (null)
				case _ => false should be {true}
			}

			verify(memberDao, times(1)).saveOrUpdate(any[Member])
		}
	}

	@Test
	def worksWithExistingMember() {
		new Environment {
			val existing = new StudentMember("0672089")
			memberDao.getByUniversityId("0672089") returns Some(existing)

			relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns None

			// now the set-up is done, run the apply command for member, which should cascade and run the other apply commands:
			val member = rowCommand.applyInternal()
			member match {
				case stu: StudentMember =>
					stu.freshStudentCourseDetails.size should be (1)
					stu.freshStudentCourseDetails.head.freshStudentCourseYearDetails.size should be (1)
				case _ => false should be {true}
			}

			verify(memberDao, times(1)).saveOrUpdate(any[Member])
		}
	}

	@Transactional
	@Test def testCaptureTutorIfSourceIsLocal() {

		new Environment {
			val existing = new StudentMember("0672089")
			val existingStaffMember = new StaffMember("0070790")

			memberDao.getByUniversityId("0070790") returns Some(existingStaffMember)
			memberDao.getByUniversityId("0672089") returns Some(existing)

			val tutorRelationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

			relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns Some(tutorRelationshipType)

			// if personalTutorSource is "local", there should be no update
			department.setStudentRelationshipSource(tutorRelationshipType, StudentRelationshipSource.Local)

			val member = rowCommand.applyInternal() match {
				case stu: StudentMember => Some(stu)
				case _ => None
			}

			val studentMember = member.get

			studentMember.freshStudentCourseDetails.size should not be 0

			verify(relationshipService, times(0)).replaceStudentRelationships(tutorRelationshipType, studentMember.mostSignificantCourseDetails.get, Seq(existingStaffMember))
		}
	}

	@Transactional
	@Test def testCaptureTutorIfSourceIsSits() {

		new Environment {
			val existing = new StudentMember("0672089")
			val existingStaffMember = new StaffMember("0070790")

			val tutorRelationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

			importCommandFactory.relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns Some(tutorRelationshipType)

			// if personalTutorSource is "SITS", there *should* an update
			department.setStudentRelationshipSource(tutorRelationshipType, StudentRelationshipSource.SITS)

			memberDao.getByUniversityId("0070790") returns Some(existingStaffMember)
			memberDao.getByUniversityId("0672089") returns Some(existing)

			importCommandFactory.relationshipService.findCurrentRelationships(tutorRelationshipType, existing) returns Nil

			val member = rowCommand.applyInternal() match {
				case stu: StudentMember => Some(stu)
				case _ => None
			}

			val studentMember = member.get

			studentMember.mostSignificantCourseDetails should not be null

			verify(relationshipService, times(1)).replaceStudentRelationships(tutorRelationshipType, studentMember.mostSignificantCourseDetails.get, Seq(existingStaffMember))
		}
	}

	@Test def testDisabilityHandling() {
		new Environment {
			var student = rowCommand.applyInternal().asInstanceOf[StudentMember]
			student.disability should be (Some(disabilityQ))

			// override to test for attempted import of unknown disability
			rs.getString("disability") returns "Mystery"
			student = rowCommand.applyInternal().asInstanceOf[StudentMember]
			student.disability should be (None)
		}
	}
}
