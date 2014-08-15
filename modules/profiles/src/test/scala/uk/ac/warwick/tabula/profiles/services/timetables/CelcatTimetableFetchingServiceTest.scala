package uk.ac.warwick.tabula.profiles.services.timetables

import dispatch.classic.Credentials
import org.apache.http.auth.AuthScope
import uk.ac.warwick.tabula.profiles.services.timetables.CelcatTimetableFetchingServiceTest.UserLookupContext
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services.TermServiceImpl
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.web.Uri

class CelcatTimetableFetchingServiceTest extends TestBase with FunctionalContextTesting {

	@Test def itWorks { inContext[UserLookupContext] {
		val service = new CelcatHttpTimetableFetchingServiceComponent with CelcatConfigurationComponent {
			val celcatConfiguration = new CelcatConfiguration {
				val perDepartmentBaseUris =	Seq(
					("ch", "https://www2.warwick.ac.uk/appdata/chem-timetables"),
					("es", "https://www2.warwick.ac.uk/appdata/eng-timetables")
				)
				val authScope = new AuthScope("www2.warwick.ac.uk", 443)
				val credentials = Credentials("in-tabula-timetablefetcher", "oWWK5xr31y8JYgje4cTN0G047rXq4B")
				val cacheEnabled = false
			}
		}.timetableFetchingService

		service.getTimetableForStudent("1313406")
	}}

}

object CelcatTimetableFetchingServiceTest {
	class UserLookupContext extends FunctionalContext {
		bean(){
			val userLookup = new MockUserLookup
			val user = new User("cuscav")
			user.setWarwickId("1313406")
			user.setDepartmentCode("ES")
			user.setFoundUser(true)

			userLookup.registerUserObjects(user)
			userLookup
		}
		bean(){new TermServiceImpl}
	}
}