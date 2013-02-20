package uk.ac.warwick.tabula.scheduling.services

import java.io.ByteArrayInputStream
import java.sql.Blob
import java.util.Date
import java.sql.ResultSet
import scala.collection.JavaConversions._
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.junit.Test
import org.junit.Test
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.Gender._
import uk.ac.warwick.tabula.data.model.MemberUserType.Staff
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleStudentCommand
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.events.EventHandling

class ProfileImporterTest extends PersistenceTestBase with Mockito {

	EventHandling.enabled = false
  
	trait Environment {
		val blobBytes = Array[Byte](1,2,3,4,5)
		val blob = mock[Blob]
		blob.getBinaryStream() returns(new ByteArrayInputStream(blobBytes))
		blob.length() returns (blobBytes.length)
		
		val rs = mock[ResultSet]
		rs.getString("gender") returns("M")
		rs.getBlob("photo") returns(blob)
		rs.getInt("year_of_study") returns(3)
		rs.getString("spr_code") returns("0672089/2")
		
		val mm = MembershipMember(
			universityId 			= "0672089",
			email					= "M.Mannion@warwick.ac.uk",
			title					= "Mr",
			preferredForenames		= "Mathew",
			preferredSurname		= "Mannion",
			dateOfBirth				= new LocalDate(1984, DateTimeConstants.AUGUST, 19),
			usercode				= "cuscav",
			userType				= Staff
		)
		
		val mac = MembershipInformation(mm, None)
	}
	
	// SITS names come uppercased - check that we reformat various names correctly.
	@Test def forenameFormatting {
		new Environment {
			val names = Seq("Mathew James", "Anna-Lee", "Nick", "Krist\u00EDn")
			val importer = new ProfileImporter
			
			for (name <- names) {
				val mac = MembershipInformation(MembershipMember(
					preferredForenames = name.toUpperCase,
					userType = Staff
				), None)
				
				val member = new ImportSingleStudentCommand(mac, new AnonymousUser, rs)				
				member.firstName should be (name)
			}
		}
	}
  
	// SITS names come uppercased - check that we reformat various names correctly.
	@Test def surnameFormatting {
		new Environment {
			val names = Seq("d'Haenens Johansson", "O'Toole", "Calvo-Bado", "Biggins", "MacCallum", "McCartney", 
							"Mannion", "von Der Glockenspeil", "d'Howes", "di Stefano", "Mc Cauley", "J\u00F3hannesd\u00F3ttir")
	
			val importer = new ProfileImporter
			
			for (name <- names) {
				val mac = MembershipInformation(MembershipMember(
					preferredSurname = name.toUpperCase,
					userType = Staff
				), None)
				
				val member = new ImportSingleStudentCommand(mac, new AnonymousUser, rs)
				member.lastName should be (name)
			}
		}
	}
	
	// Test that if we have name formatting from SSO, we use that as long as the names match
	@Test def takesSuggestions {
		new Environment {
			val importer = new ProfileImporter
			
			val user1 = new User()
			user1.setFirstName("MatHEW James")
			user1.setLastName("Macintosh")
			
			val user2 = new User()
			user2.setFirstName("different")
			user2.setLastName("strokes")
						
			val member1 = new ImportSingleStudentCommand(mac, user1, rs)
			val member2 = new ImportSingleStudentCommand(mac, user2, rs)
			  
			member1.firstName should be ("MatHEW James")
			member1.lastName should be ("Macintosh")
			
			member2.firstName should be ("Mathew James")
			member2.lastName should be ("MacIntosh")
		}
	}
	
	@Test def importStaff {	
		val blobBytes = Array[Byte](1,2,3,4,5)
		val blob = mock[Blob]
		blob.getBinaryStream() returns(new ByteArrayInputStream(blobBytes))
		blob.length() returns (blobBytes.length)
		
		val rs = mock[ResultSet]
		rs.getString("gender") returns("M")
		rs.getBlob("photo") returns(blob)
		rs.getInt("year_of_study") returns(3)
		
		val mac = MembershipInformation(MembershipMember(
			universityId 			= "0672089",
			email					= "M.Mannion@warwick.ac.uk",
			title					= "Mr",
			preferredForenames		= "Mathew",
			preferredSurname		= "Mannion",
			dateOfBirth				= new LocalDate(1984, DateTimeConstants.AUGUST, 19),
			usercode				= "cuscav",
			userType				= Staff
		), None)
		
		val importer = new ProfileImporter()
		val fileDao = mock[FileDao]
		
		val memberDao = mock[MemberDao]
		memberDao.getByUniversityId("0672089") returns(None)
		
		val command = new ImportSingleStudentCommand(mac, new AnonymousUser, rs)
		command.memberDao = memberDao
		command.fileDao = fileDao
		
		val member = command.apply()
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