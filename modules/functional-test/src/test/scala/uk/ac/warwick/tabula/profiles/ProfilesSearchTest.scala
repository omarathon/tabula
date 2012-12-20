package uk.ac.warwick.tabula.profiles

import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.internal.seleniumemulation.WaitForPageToLoad

class ProfilesSearchTest extends BrowserTest {
	
	"A member of staff" must "be able to view student profiles" ignore {
		signIn as(P.Admin1) to (Path("/profiles"))
		click on "query"
		pressKeys("john") // There's always a John

		// eventually AJAX will return a response.
		eventuallyAjax {
			find(cssSelector("#searchProfilesCommand ul")) should not be (None) 			
		}
		
		// TODO for some reason this engine doesn't like our typeahead
		// setup. in the updater method, profilePickerMappings[mapKey]
		// returns undefined.
		//click on (cssSelector("#searchProfilesCommand ul li"))
		
		// clicking on results is a bit broken in htmlunit, as above,
		// so just submit the form which should go to the first result.
		submit()
		
		// click first result
		click on (cssSelector(".profile-search-results a"))
		
		find("personal-details") should not be (None)
		
		// Just check here that we're not showing sysadminy buttons to a regular staff.
		pageSource should not include ("Sysadmin")
	}
	
}