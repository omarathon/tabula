package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet
import java.sql.ResultSetMetaData
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.junit.{Ignore, Test}
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.Gender._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.scheduling.services.MembershipMember
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.scheduling.services.MembershipMember
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.CourseAndRouteService
import uk.ac.warwick.tabula.scheduling.services.MembershipMember
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD
import java.sql.Date
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceImporter
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDao
import uk.ac.warwick.tabula.services.ProfileServiceImpl
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceImporterImpl
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDaoImpl
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.scheduling.services.SitsStatusesImporter
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.StudentCourseDetailsDao

// scalastyle:off magic.number
@DirtiesContext(classMode=AFTER_EACH_TEST_METHOD)
class ImportSingleStudentRowCommandTest extends AppContextTestBase with Mockito with Logging {

	trait Environment {
		val blobBytes = Array[Byte](1,2,3,4,5)

		val fileDao = smartMock[FileDao]

		val route = new Route
		val mds = smartMock[ModuleAndDepartmentService]
		val crService = smartMock[CourseAndRouteService]
		crService.getRouteByCode("c100") returns (Some(route))

		val department = new Department
		department.code = "ph"
		department.name = "Philosophy"
		department.personalTutorSource = Department.Settings.PersonalTutorSourceValues.Sits
		mds.getDepartmentByCode("ph") returns (Some(department))
		mds.getDepartmentByCode("PH") returns (Some(department))
		val rs = smartMock[ResultSet]
		val md = smartMock[ResultSetMetaData]
		rs.getMetaData() returns(md)
		md.getColumnCount() returns(4)
		md.getColumnName(1) returns("gender")
		md.getColumnName(2) returns("year_of_study")
		md.getColumnName(3) returns("spr_code")
		md.getColumnName(4) returns("route_code")

		rs.getString("gender") returns("M")
		rs.getInt("year_of_study") returns(3)
		rs.getString("spr_code") returns("0672089/2")
		rs.getString("route_code") returns("C100")
		rs.getString("spr_tutor1") returns ("IN0070790")
		rs.getString("homeDepartmentCode") returns ("PH")
		rs.getString("scj_code") returns ("0672089/2")
		rs.getDate("begin_date") returns new Date(new java.util.Date("12 May 2011").getTime())
		rs.getDate("end_date") returns new Date(new java.util.Date("12 May 2014").getTime())
		rs.getDate("expected_end_date") returns new Date(new java.util.Date("12 May 2015").getTime())
		rs.getInt("sce_sequence_number") returns (1)
		rs.getString("enrolment_status_code") returns ("F")
		rs.getString("mode_of_attendance_code") returns ("P")
		rs.getString("sce_academic_year") returns ("10/11")

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
			userType				= Student		)

		val mac = MembershipInformation(mm, () => Some(blobBytes))

	}

	@Test def testImportSingleStudentCourseYearCommand {
		new Environment {
			val modeOfAttendanceImporter = smartMock[ModeOfAttendanceImporter]
			val profileService = smartMock[ProfileService]
			val studentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]
			val sitsStatusesImporter = smartMock[SitsStatusesImporter]

			modeOfAttendanceImporter.modeOfAttendanceMap returns Map("F" -> new ModeOfAttendance("F", "FT", "Full Time"), "P" -> new ModeOfAttendance("P", "PT", "Part Time"))
			modeOfAttendanceImporter.getModeOfAttendanceForCode("P") returns Some(new ModeOfAttendance("P", "PT", "Part Time"))
			sitsStatusesImporter.sitsStatusMap returns Map("F" -> new SitsStatus("F", "F", "Fully Enrolled"), "P" -> new SitsStatus("P", "P", "Permanently Withdrawn"))


			val command = new ImportSingleStudentCourseYearCommand(rs)
			command.modeOfAttendanceImporter = modeOfAttendanceImporter
			command.profileService = profileService
			command.studentCourseYearDetailsDao = studentCourseYearDetailsDao
			command.sitsStatusesImporter = sitsStatusesImporter

			val studentCourseDetails = new StudentCourseDetails
			studentCourseDetails.scjCode = "0672089/2"
			studentCourseDetails.sprCode = "0672089/2"

			command.studentCourseDetails = studentCourseDetails

			val studentCourseYearDetails = command.applyInternal
			studentCourseYearDetails.academicYear.toString should be ("10/11")
			studentCourseYearDetails.sceSequenceNumber should be (1)
			studentCourseYearDetails.enrolmentStatus.code should be ("F")
			studentCourseYearDetails.lastUpdatedDate should not be null
			studentCourseYearDetails.modeOfAttendance.code should be ("P")
			studentCourseYearDetails.yearOfStudy should be (3)

			there was one(studentCourseYearDetailsDao).saveOrUpdate(any[StudentCourseYearDetails]);
		}
	}

	@Test def testImportSingleStudentCourseCommand {
		new Environment {
			// first set up studentCouresYearDetails, because the apply for that command is included in the apply for this command
			val modeOfAttendanceImporter = smartMock[ModeOfAttendanceImporter]

			val profileService = smartMock[ProfileService]
			val studentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]
			val sitsStatusesImporter = smartMock[SitsStatusesImporter]

			modeOfAttendanceImporter.modeOfAttendanceMap returns Map("F" -> new ModeOfAttendance("F", "FT", "Full Time"), "P" -> new ModeOfAttendance("P", "PT", "Part Time"))
			modeOfAttendanceImporter.getModeOfAttendanceForCode("P") returns Some(new ModeOfAttendance("P", "PT", "Part Time"))
			sitsStatusesImporter.sitsStatusMap returns Map("F" -> new SitsStatus("F", "F", "Fully Enrolled"), "P" -> new SitsStatus("P", "P", "Permanently Withdrawn"))

			val yearCommand = new ImportSingleStudentCourseYearCommand(rs)
			yearCommand.modeOfAttendanceImporter = modeOfAttendanceImporter
			yearCommand.profileService = profileService
			yearCommand.studentCourseYearDetailsDao = studentCourseYearDetailsDao
			yearCommand.sitsStatusesImporter = sitsStatusesImporter

			// end of stuff to set up studentCourseYearDetails

			val studentCourseDetailsDao = smartMock[StudentCourseDetailsDao]

			val command = new ImportSingleStudentCourseCommand(rs, yearCommand)
			command.studentCourseDetailsDao = studentCourseDetailsDao
			command.sitsStatusesImporter = sitsStatusesImporter

			val studentCourseDetails = command.applyInternal

			studentCourseDetails.scjCode should be ("0672089/2")
			studentCourseDetails.beginDate.toString should be ("2011-05-12")
			studentCourseDetails.endDate.toString should be ("2014-05-12")
			studentCourseDetails.expectedEndDate.toString should be ("2015-05-12")

			there was one(studentCourseDetailsDao).saveOrUpdate(any[StudentCourseDetails]);
		}

	}

	// Just a simple test to make sure all the properties that we use BeanWrappers for actually exist, really
	@Ignore("broken")
	@Test def worksWithNew {
		new Environment {
			val memberDao = smartMock[MemberDao]
			memberDao.getByUniversityId("0672089") returns(None)

			val command = new ImportSingleStudentRowCommand(mac, new AnonymousUser(), rs)
			command.memberDao = memberDao
			command.fileDao = fileDao
			command.moduleAndDepartmentService = mds

			val member = command.applyInternal
			member.title should be ("Mr")
			member.universityId should be ("0672089")
			member.userId should be ("cuscav")
			member.email should be ("M.Mannion@warwick.ac.uk")
			member.gender should be (Male)
			member.firstName should be ("Mathew")
			member.lastName should be ("Mannion")
			member.photo should not be (null)
			member.dateOfBirth should be (new LocalDate(1984, DateTimeConstants.AUGUST, 19))

			there was one(fileDao).savePermanent(any[FileAttachment])
			there was no(fileDao).saveTemporary(any[FileAttachment])

			there was one(memberDao).saveOrUpdate(any[Member])
		}
	}

	@Ignore("broken") @Test def worksWithExisting {
		new Environment {
			val existing = new StudentMember("0672089")

			val memberDao = smartMock[MemberDao]
			memberDao.getByUniversityId("0672089") returns(Some(existing))


			val command = new ImportSingleStudentRowCommand(mac, new AnonymousUser(), rs)
			command.memberDao = memberDao
			command.fileDao = fileDao
			command.moduleAndDepartmentService = mds

			val member = command.applyInternal
			member.title should be ("Mr")
			member.universityId should be ("0672089")
			member.userId should be ("cuscav")
			member.email should be ("M.Mannion@warwick.ac.uk")
			member.gender should be (Male)
			member.firstName should be ("Mathew")
			member.lastName should be ("Mannion")
			member.photo should not be (null)
			member.dateOfBirth should be (new LocalDate(1984, DateTimeConstants.AUGUST, 19))

			there was one(fileDao).savePermanent(any[FileAttachment])
			there was no(fileDao).saveTemporary(any[FileAttachment])

			there was one(memberDao).saveOrUpdate(existing)
		}
	}

	@Ignore("broken") @Transactional
	@Test def testCaptureTutorIfSourceIsLocal {

		new Environment {
			val existing = new StudentMember("0672089")
			val existingStaffMember = new StaffMember("0070790")
			val memberDao = smartMock[MemberDao]
			val relationshipService = smartMock[RelationshipService]
			val profileService = smartMock[ProfileService]

			memberDao.getByUniversityId("0070790") returns(Some(existingStaffMember))
			memberDao.getByUniversityId("0672089") returns(Some(existing))

			// if personalTutorSource is "local", there should be no update
			department.personalTutorSource = "local"

			val command = new ImportSingleStudentRowCommand(mac, new AnonymousUser(), rs)
			command.memberDao = memberDao
			command.fileDao = fileDao
			command.moduleAndDepartmentService = mds
			command.profileService = profileService

			val member = command.applyInternal match {
				case stu: StudentMember => Some(stu)
				case _ => None
			}

			val studentMember = member.get

			studentMember.studentCourseDetails.size should not be (0)

			there was no(relationshipService).saveStudentRelationship(PersonalTutor, "0672089/2","0070790");
		}
	}

	@Ignore("broken") @Transactional
	@Test def testCaptureTutorIfSourceIsSits {

		new Environment {
			val existing = new StudentMember("0672089")
			val existingStaffMember = new StaffMember("0070790")
			val memberDao = smartMock[MemberDao]
			val profileService = smartMock[ProfileService]
			val relationshipService = smartMock[RelationshipService]


			memberDao.getByUniversityId("0070790") returns(Some(existingStaffMember))
			memberDao.getByUniversityId("0672089") returns(Some(existing))
			relationshipService.findCurrentRelationships(RelationshipType.PersonalTutor, "0672089/2") returns (Nil)

			// if personalTutorSource is "SITS", there *should* an update
			department.personalTutorSource = Department.Settings.PersonalTutorSourceValues.Sits

			val command = new ImportSingleStudentRowCommand(mac, new AnonymousUser(), rs)
			command.memberDao = memberDao
			command.fileDao = fileDao
			command.moduleAndDepartmentService = mds
			command.profileService = profileService


			val member = command.applyInternal match {
				case stu: StudentMember => Some(stu)
				case _ => None
			}

			val studentMember = member.get

			studentMember.mostSignificantCourseDetails should not be (null)

			there was one(relationshipService).saveStudentRelationship(PersonalTutor, "0672089/2","0070790");
		}
	}
}

