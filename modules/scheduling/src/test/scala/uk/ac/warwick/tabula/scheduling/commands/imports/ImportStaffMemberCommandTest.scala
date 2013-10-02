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
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.data.model.MemberUserType.Staff
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.scheduling.services.MembershipMember
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.data.model.StaffProperties
import java.sql.ResultSetMetaData

// scalastyle:off magic.number
class ImportStaffMemberCommandTest extends TestBase with Mockito {

	trait Environment {
		val blobBytes = Array[Byte](1,2,3,4,5)

		val mm = MembershipMember(
			universityId 			= "0672089",
			email					= "M.Mannion@warwick.ac.uk",
			title					= "Mr",
			preferredForenames		= "Mathew",
			preferredSurname		= "Mannion",
			dateOfBirth				= new LocalDate(1984, DateTimeConstants.AUGUST, 19),
			usercode				= "cuscav",
			gender					= Male,
			userType				= Staff
		)

		val mac = MembershipInformation(mm, () => Some(blobBytes))
	}

	// Just a simple test to make sure all the properties that we use BeanWrappers for actually exist, really
	@Test def worksWithNew {
		new Environment {
			val fileDao = mock[FileDao]

			val memberDao = mock[MemberDao]
			memberDao.getByUniversityId("0672089") returns(None)

			val command = new ImportStaffMemberCommand(mac, new AnonymousUser())
			command.memberDao = memberDao
			command.fileDao = fileDao

			val member = command.applyInternal
			member.isInstanceOf[StaffProperties] should be (true)

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
			val existing = new StaffMember("0672089")

			val fileDao = mock[FileDao]

			val memberDao = mock[MemberDao]
			memberDao.getByUniversityId("0672089") returns(Some(existing))

			val command = new ImportStaffMemberCommand(mac, new AnonymousUser())
			command.memberDao = memberDao
			command.fileDao = fileDao

			val member = command.applyInternal
			member.isInstanceOf[StaffProperties] should be (true)

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

}