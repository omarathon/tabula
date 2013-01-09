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
import uk.ac.warwick.tabula.data.model.Male
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.data.model.Route

class ImportSingleStudentCommandTest extends TestBase with Mockito {
	
	trait Environment {
		val blobBytes = Array[Byte](1,2,3,4,5)
		val blob = mock[Blob]

		val fileDao = mock[FileDao]
		
		val route = Option(new Route)
		val mds = mock[ModuleAndDepartmentService]
		mds.getRouteByCode("C100") returns (route)
		
		blob.getBinaryStream() returns(new ByteArrayInputStream(blobBytes))
		blob.length() returns (blobBytes.length)
		
		val rs = mock[ResultSet]
		rs.getString("university_id") returns("0672089")
		rs.getString("title") returns("Mr")
		rs.getString("preferred_forename") returns("Mathew")
		rs.getString("family_name") returns("Mannion")
		rs.getString("gender") returns("M")
		rs.getString("user_code") returns("cuscav")
		rs.getString("email_address") returns("M.Mannion@warwick.ac.uk")
		rs.getBlob("photo") returns(blob)
		rs.getInt("year_of_study") returns(3)
		rs.getDate("date_of_birth") returns(new Date(new LocalDate(1984, DateTimeConstants.AUGUST, 19).toDate().getTime()))
		rs.getString("spr_code") returns("0672089/2")
		rs.getString("route_code") returns("C100")
		

			

	}
	
	// Just a simple test to make sure all the properties that we use BeanWrappers for actually exist, really
	@Test def worksWithNew {
		new Environment {
			val memberDao = mock[MemberDao]
			memberDao.getByUniversityId("0672089") returns(None)
						
			val command = new ImportSingleStudentCommand(rs)
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
			member.sprCode should be ("0672089/2")
			member.route should be (route)
			
			there was one(fileDao).savePermanent(any[FileAttachment])
			there was no(fileDao).saveTemporary(any[FileAttachment])
			
			there was one(memberDao).saveOrUpdate(any[Member])
		}
	}
	
	@Test def worksWithExisting {
		new Environment {
			val existing = new Member("0672089")
			
			val memberDao = mock[MemberDao]
			memberDao.getByUniversityId("0672089") returns(Some(existing))
			
			val command = new ImportSingleStudentCommand(rs)
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
			
			member.sprCode should be ("0672089/2") // added ZLJ
			member.route should be (route)
			
			there was one(fileDao).savePermanent(any[FileAttachment])
			there was no(fileDao).saveTemporary(any[FileAttachment])
			
			there was one(memberDao).saveOrUpdate(existing)
		}
	}

}