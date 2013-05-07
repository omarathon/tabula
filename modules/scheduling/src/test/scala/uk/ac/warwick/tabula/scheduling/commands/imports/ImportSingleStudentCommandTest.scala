package uk.ac.warwick.tabula.scheduling.commands.imports

import java.io.ByteArrayInputStream
import java.sql.Blob
import java.sql.Date
import java.sql.ResultSet
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.junit.Test
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.Gender._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.scheduling.services.MembershipMember
import uk.ac.warwick.tabula.data.model.StudentMember
import java.sql.ResultSetMetaData
import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.beans.BeanWrapperImpl
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ProfileService

// scalastyle:off magic.number
class ImportSingleStudentCommandTest extends AppContextTestBase with Mockito with Logging {

	trait Environment {
		val blobBytes = Array[Byte](1,2,3,4,5)

		val fileDao = mock[FileDao]

		val route = new Route
		val mds = mock[ModuleAndDepartmentService]
		mds.getRouteByCode("c100") returns (Some(route))

		val department = new Department
		department.code = "ph"
		department.name = "Philosophy"
		department.personalTutorSource = "SITS"
		mds.getDepartmentByCode("ph") returns (Some(department))
		mds.getDepartmentByCode("PH") returns (Some(department))
		val rs = mock[ResultSet]
		val md = mock[ResultSetMetaData]
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
		rs.getString("spr_tutor1") returns ("0070790")
		rs.getString("homeDepartmentCode") returns ("PH")

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

		val mac = MembershipInformation(mm, Some(blobBytes))

	}

	// Just a simple test to make sure all the properties that we use BeanWrappers for actually exist, really
	@Test def worksWithNew {
		new Environment {
			val memberDao = mock[MemberDao]
			memberDao.getByUniversityId("0672089") returns(None)

			val command = new ImportSingleStudentCommand(mac, new AnonymousUser(), rs)
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

	@Test def worksWithExisting {
		new Environment {
			val existing = new StudentMember("0672089")

			val memberDao = mock[MemberDao]
			memberDao.getByUniversityId("0672089") returns(Some(existing))

			val command = new ImportSingleStudentCommand(mac, new AnonymousUser(), rs)
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

	@Transactional
	@Test def testCaptureTutor {
		var profileService = Wire.auto[ProfileService]

		new Environment {
			val memberDao = mock[MemberDao]

			// if personalTutorSource is "local", there should be no update
			department.personalTutorSource = "local"

			val command = new ImportSingleStudentCommand(mac, new AnonymousUser(), rs)

			val member = command.applyInternal match {
				case stu: StudentMember => Some(stu)
				case _ => None
			}

			val studentMember = member.get

			studentMember.studyDetails should not be (null)

			val newTutor = profileService.getPersonalTutor(studentMember)

			//newTutor should be (None)


			// if personalTutorSource is "SITS", there *should* an update
			department.personalTutorSource = "SITS"

			val command2 = new ImportSingleStudentCommand(mac, new AnonymousUser(), rs)

			val member2 = command2.applyInternal match {
				case stu: StudentMember => Some(stu)
				case _ => None
			}

			val studentMember2 = member2.get

			studentMember2.studyDetails should not be (null)

			val newTutor2 = profileService.getPersonalTutor(studentMember2)

			val newTutorUniId = newTutor2.get.universityId
			newTutorUniId should be ("0070790")
		}
	}
}

