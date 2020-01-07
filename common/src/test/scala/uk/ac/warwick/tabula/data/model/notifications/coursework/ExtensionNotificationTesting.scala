package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, ProfileService, RelationshipService, UserLookupService}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.userlookup.User

trait ExtensionNotificationTesting {
  m: Mockito =>
  lazy val mockUserLookup: UserLookupService = smartMock[UserLookupService]
  lazy val mockProfileService: ProfileService = smartMock[ProfileService]
  lazy val mockRelationshipService: RelationshipService = mock[RelationshipService]

  def wireUserlookup(n: AutowiringUserLookupComponent, student: User): Unit = {
    n.userLookup = mockUserLookup
    mockUserLookup.getUserByUserId(student.getUserId) returns student
    mockUserLookup.getUserByWarwickUniId(any[String]) returns null
  }

}
