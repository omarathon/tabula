package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.roles.Submitter
import uk.ac.warwick.tabula.roles.FeedbackRecipient
import uk.ac.warwick.tabula.roles.SettingsOwner

class OwnDataRoleProviderTest extends TestBase {
	
	val provider = new OwnDataRoleProvider
	
	val submission = Fixtures.submission("0123456", "cuscav")	
	val feedback = Fixtures.feedback("0123456")
	val userSettings = Fixtures.userSettings("cuscav")
	
	@Test def forSubmission = withUser("cuscav", "0123456") {
		provider.getRolesFor(currentUser, submission) should be (Seq(Submitter(submission)))
		provider.getRolesFor(currentUser, Fixtures.submission("xxxxxx", "000000")) should be (Seq())
	}
	
	@Test def forFeedback = withUser("cuscav", "0123456") {
		// only if released!
		provider.getRolesFor(currentUser, feedback) should be (Seq())
		
		feedback.released = true
		
		provider.getRolesFor(currentUser, feedback) should be (Seq(FeedbackRecipient(feedback)))
		
		provider.getRolesFor(currentUser, Fixtures.feedback()) should be (Seq())
	}
	
	@Test def forSettings = withUser("cuscav", "0123456") {
		provider.getRolesFor(currentUser, userSettings) should be (Seq(SettingsOwner(userSettings)))
		provider.getRolesFor(currentUser, Fixtures.userSettings("xxxxxx")) should be (Seq())
	}
	
	@Test def forSettingsNoId = withUser("") {
		provider.getRolesFor(currentUser, Fixtures.userSettings("")) should be (Seq())
	}
	
	@Test def handlesDefault = withUser("cuscav", "0123456") {
		provider.getRolesFor(currentUser, Fixtures.department("in", "IN202")) should be (Seq())
	}

}