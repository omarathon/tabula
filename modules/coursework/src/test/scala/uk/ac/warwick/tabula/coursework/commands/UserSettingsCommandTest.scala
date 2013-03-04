package uk.ac.warwick.tabula.coursework.commands

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.UserSettingsService
import uk.ac.warwick.tabula.data.model.UserSettings
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.userlookup.AnonymousUser

class UserSettingsCommandTest extends TestBase with Mockito {
	
	val service = mock[UserSettingsService]
	
	@Test def itWorks = withUser("cuscav") {
		val existing = new UserSettings(currentUser.apparentId)
		existing.alertsSubmission = UserSettings.AlertsLateSubmissions
		
		val cmd = new UserSettingsCommand(currentUser, existing)
		cmd.service = service
		
		cmd.alertsSubmission should be (UserSettings.AlertsLateSubmissions)
		
		cmd.alertsSubmission = UserSettings.AlertsAllSubmissions
		cmd.applyInternal
		
		existing.alertsSubmission should be (UserSettings.AlertsAllSubmissions)
		there was one(service).save(currentUser, existing)
	}
	
	@Test def validateNoUser = withCurrentUser(new CurrentUser(new AnonymousUser, new AnonymousUser)) {
		val cmd = new UserSettingsCommand(currentUser, new UserSettings)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCode should be ("user.mustBeLoggedIn")
	}
	
	@Test def validatePasses = withUser("cuscav") {
		val cmd = new UserSettingsCommand(currentUser, new UserSettings)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		
		errors.hasErrors should be (false)
	}

}