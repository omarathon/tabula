package uk.ac.warwick.tabula.coursework.notifications

import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, RelationshipService, ProfileService, UserLookupService}
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.notifications.ExtensionNotification
import uk.ac.warwick.userlookup.User


trait ExtensionNotificationTesting { m: Mockito =>
	lazy val mockUserLookup = smartMock[UserLookupService]
	lazy val mockProfileService = smartMock[ProfileService]
	lazy val mockRelationshipService = mock[RelationshipService]

	def wireUserlookup(n: AutowiringUserLookupComponent, student: User) {
		n.userLookup = mockUserLookup
		mockUserLookup.getUserByUserId(student.getUserId) returns (student)
	}
	
}
