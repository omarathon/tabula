package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.CurrentUser

class SearchProfilesCommandTest extends TestBase with Mockito {

	@Test def validQuery {

		val member = mock[Member]
		val user = mock[CurrentUser]

		val cmd = new SearchProfilesCommand(member, user)

		// query needs to be at least 3 characters
		cmd.setQuery("xx")
		cmd.validQuery should be(false)

		// and the minimum term length is two
		cmd.setQuery("xx x")
		cmd.validQuery should be(false)

		cmd.setQuery("select foo from bar")
		cmd.validQuery should be(true)
	}
}
