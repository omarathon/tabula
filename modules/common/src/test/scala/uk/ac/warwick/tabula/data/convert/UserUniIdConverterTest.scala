package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.{AnonymousUser, User}


class UserUniIdConverterTest extends TestBase with Mockito {

	val converter = new UserUniIdConverter
	val userLookup = mock[UserLookupService]
	converter.userLookup = userLookup

	@Test def validInput() {
		val user = new User
		user.setUserId("1170836")

		userLookup.getUserByWarwickUniId("1170836") returns user

		converter.convertRight("1170836") should be (user)
	}

	@Test def invalidInput() {
		userLookup.getUserByWarwickUniId("20X6") returns new AnonymousUser

		converter.convertRight("20X6") should be (new AnonymousUser)
	}

	@Test def formatting() {
		converter.convertLeft(new User("cuslaj"){setWarwickId("1170836")}) should be ("1170836")
		converter.convertLeft(null) should be (null)
	}

}
