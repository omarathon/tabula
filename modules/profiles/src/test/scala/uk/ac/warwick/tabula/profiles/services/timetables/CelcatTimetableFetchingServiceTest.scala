package uk.ac.warwick.tabula.profiles.services.timetables

import dispatch.classic.Credentials
import org.apache.http.auth.AuthScope
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services.{TermServiceImpl, TermServiceComponent, UserLookupComponent}

class CelcatTimetableFetchingServiceTest extends TestBase {

	val service = new CelcatHttpTimetableFetchingService(new CelcatConfiguration {
		val perDepartmentBaseUris =	Seq(
			("ch", "https://www2.warwick.ac.uk/appdata/chem-timetables"),
			("es", "https://www2.warwick.ac.uk/appdata/eng-timetables")
		)
		lazy val authScope = new AuthScope("www2.warwick.ac.uk", 443)
		lazy val credentials = Credentials("username", "password")
		val cacheEnabled = false
	}) with UserLookupComponent with TermServiceComponent {
		val userLookup = new MockUserLookup
		val termService = new TermServiceImpl
	}

	@Test def parseICal {
		val events = service.parseICal(resourceAsStream("1313406.ics"))
		events.size should be (142)

		val combined = service.combineIdenticalEvents(events)
		combined.size should be (136)
	}

}