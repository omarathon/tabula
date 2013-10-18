package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{MockCachingLookupService, TestBase, Mockito}
import uk.ac.warwick.tabula.UserFlavour._

class UserLookupServiceTests extends TestBase with Mockito {

	@Test def swappingUserLookup {
		val lookup1 = mock[UserLookupService]
		val lookup2 = mock[UserLookupService]

		val swappable = new SwappableUserLookupService(lookup1)
		swappable.getUserByUserId("a")
		swappable.delegate = lookup2
		swappable.getUserByUserId("b")

		there was one(lookup1).getUserByUserId("a")
		there was one(lookup2).getUserByUserId("b")
	}

	/*
	 * we're testing the UserByWarwickIdCache trait here,
	 * which is implemented by both the UserLookupService and the MockCachingLookupService
	 */
	@Test def caching {
		val muls = new MockCachingLookupService
		val cache = muls.UserByWarwickIdCache
		var warwickId = ""
		cache.clear()

		// an unverified user should not be cached
		muls.flavour = Unverified
		warwickId = "1234567"
		val unverifiedUser = muls.getUserByWarwickUniId(warwickId)
		unverifiedUser.isVerified should be (false)
		unverifiedUser.isFoundUser should be (false)
		cache.getStatistics.getCacheSize should be (0)

		// a good user should be cached
		muls.flavour = Vanilla
		warwickId = "0123456"
		val existingUser = muls.getUserByWarwickUniId(warwickId)
		existingUser.isVerified should be (true)
		existingUser.isFoundUser should be (true)
		cache.getStatistics.getCacheSize should be (1)
		cache.contains(warwickId) should be (true)

		// an applicant user should also be cached with a real key
		// (for a shorter period, but we can't tell that from outside)
		muls.flavour = Applicant
		warwickId = "1819201"
		val applicantUser = muls.getUserByWarwickUniId(warwickId)
		applicantUser.isVerified should be (true)
		applicantUser.isFoundUser should be (false)
		cache.getStatistics.getCacheSize should be (2)
		cache.contains(warwickId) should be (true)

		// an anonymous user should be cached with empty string as key
		// (for a shorter period, but we can't tell that from outside)
		muls.flavour = Anonymous
		warwickId = "0987654"
		val anonUser = muls.getUserByWarwickUniId(warwickId)
		anonUser.isVerified should be (true)
		anonUser.isFoundUser should be (false)
		cache.getStatistics.getCacheSize should be (3)
		cache.contains(warwickId) should be (true)
	}
}

