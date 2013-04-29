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

// scalastyle:off magic.number
class ImportSingleStudentCommandTest extends TestBase with Mockito {
	
	trait Environment {
		val blobBytes = Array[Byte](1,2,3,4,5)

		val fileDao = mock[FileDao]
		
		val route = new Route
		val mds = mock[ModuleAndDepartmentService]
		mds.getRouteByCode("c100") returns (Some(route))
		
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
		
		val mm = MembershipMember(
			universityId 			= "0672089",
			departmentCode			= null,
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
			userType				= Student
		)
		
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

}