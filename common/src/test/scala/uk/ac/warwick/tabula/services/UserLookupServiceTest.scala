package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{Mockito, TestBase}

class UserLookupServiceTest extends TestBase with Mockito {
  @Test def swappingUserLookup() {
    val lookup1 = mock[UserLookupService]
    val lookup2 = mock[UserLookupService]

    val swappable = new SwappableUserLookupService(lookup1)
    swappable.getUserByUserId("a")
    swappable.delegateUserLookupService = lookup2
    swappable.getUserByUserId("b")

    verify(lookup1, times(1)).getUserByUserId("a")
    verify(lookup2, times(1)).getUserByUserId("b")
  }
}

