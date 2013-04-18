package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.data.model.UserSettings

class UserSettingsServiceTest extends AppContextTestBase {
	
	lazy val service = Wire[UserSettingsService]
	
	@Test def itWorks = transactional { tx =>
		service.getByUserId("cuscav") should be (None)
		
		val userSettings = new UserSettings
		userSettings.userId = "cuscav"
		userSettings.alertsSubmission = UserSettings.AlertsLateSubmissions
		
		withUser("cuscav") { service.save(currentUser, userSettings) }
		
		service.getByUserId("cuscav") should be ('defined)
		service.getByUserId("cuscav").get.alertsSubmission should be (UserSettings.AlertsLateSubmissions)
		
		// If we save a new empty user settings, we don't overwrite anything existing
		withUser("cuscav") { service.save(currentUser, new UserSettings) }
		
		service.getByUserId("cuscav") should be ('defined)
		service.getByUserId("cuscav").get.alertsSubmission should be (UserSettings.AlertsLateSubmissions)
	}
}
