package uk.ac.warwick.tabula.services

import java.sql.ResultSet
import scala.collection.JavaConversions._
import org.junit.Test
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.UpstreamMember
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.Department
import java.sql.Date
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.data.model.Male
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTimeConstants

class ProfileImporterTest extends TestBase with Mockito {
  
	// SITS names come uppercased - check that we reformat various names correctly.
	@Test def forenameFormatting {
		val names = Seq("Mathew James", "Anna-Lee", "Nick", "Krist\u00EDn")

		val importer = new ProfileImporter
		
		for (name <- names) {
			val member = new UpstreamMember
			member.firstName = name.toUpperCase()
			
			importer.processNames(member, Map() withDefaultValue(new AnonymousUser))
			
			member.firstName should be (name)
		}
	}
  
	// SITS names come uppercased - check that we reformat various names correctly.
	@Test def surnameFormatting {
		val names = Seq("d'Haenens Johansson", "O'Toole", "Calvo-Bado", "Biggins", "MacCallum", "McCartney", 
						"Mannion", "von Der Glockenspeil", "d'Howes", "di Stefano", "Mc Cauley", "J\u00F3hannesd\u00F3ttir")

		val importer = new ProfileImporter
		
		for (name <- names) {
			val member = new UpstreamMember
			member.lastName = name.toUpperCase()
			
			importer.processNames(member, Map() withDefaultValue(new AnonymousUser))
			
			member.lastName should be (name)
		}
	}
	
	// Test that if we have name formatting from SSO, we use that as long as the names match
	@Test def takesSuggestions {
		val importer = new ProfileImporter
		
		val user1 = new User()
		user1.setFirstName("MatHEW James")
		user1.setLastName("Macintosh")
		
		val user2 = new User()
		user2.setFirstName("different")
		user2.setLastName("strokes")
		
		val users = Map("user1" -> user1, "user2" -> user2)
		
		val member1 = new UpstreamMember
		member1.userId = "user1"
		member1.firstName = "MATHEW JAMES"
		member1.lastName = "MACINTOSH"
		  
		val member2 = new UpstreamMember
		member2.userId = "user2"
		member2.firstName = "MATHEW JAMES"
		member2.lastName = "MACINTOSH"
		  
		importer.processNames(member1, users).firstName should be ("MatHEW James")
		importer.processNames(member1, users).lastName should be ("Macintosh")
		
		importer.processNames(member2, users).firstName should be ("Mathew James")
		importer.processNames(member2, users).lastName should be ("MacIntosh")
	}
	
	@Test def importStaff {
		val importer = new ProfileImporter
		
		val userLookup = mock[UserLookupService]
		userLookup.getUsersByUserIds(any[List[String]]) returns (Map[String, User]())
		
		val moduleAndDepartmentService = mock[ModuleAndDepartmentService]
		
		importer.userLookup = userLookup
		importer.moduleAndDepartmentService = moduleAndDepartmentService
		
		val rs = mock[ResultSet]
		rs.getString("university_id") returns("0672089")
		rs.getString("title") returns("MR")
		rs.getString("preferred_forename") returns("Mathew")
		rs.getString("family_name") returns("Mannion")
		rs.getString("gender") returns("M")
		rs.getString("user_code") returns("cuscav")
		rs.getString("email_address") returns("M.Mannion@warwick.ac.uk")
		rs.getBlob("photo") returns(null)
		rs.getString("academic_year_code") returns("11/12")
		rs.getInt("year_of_study") returns(3)
		rs.getDate("date_of_birth") returns(new Date(new LocalDate(1984, DateTimeConstants.AUGUST, 19).toDate().getTime()))
		
		val member = importer.createMember(rs, Map().withDefaultValue(new AnonymousUser))
		member.title should be ("Mr")
		member.universityId should be ("0672089")
		member.userId should be ("cuscav")
		member.email should be ("M.Mannion@warwick.ac.uk")
		member.gender should be (Male)
		member.firstName should be ("Mathew")
		member.lastName should be ("Mannion")
		member.photo should be (null)
		member.academicYear should be (new AcademicYear(2011))
		member.dateOfBirth should be (new LocalDate(1984, DateTimeConstants.AUGUST, 19))
	}

}