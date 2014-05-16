package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{Submission, Assignment}
import uk.ac.warwick.tabula.services.{AssignmentMembershipService, UserLookupService, IncludeType, MembershipItem, AssignmentMembershipInfo}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.forms.Extension

class SubmissionDueNotificationTest extends TestBase with Mockito {

	val users = Seq(
		Fixtures.user(universityId="0123456", userId="cusaaa"),
		Fixtures.user(universityId="0133454", userId="cusaab")
	)

	val assignment = new Assignment

	@Test
	def generalRecipients() {
		val notification = new SubmissionDueGeneralNotification {
			override def assignment = SubmissionDueNotificationTest.this.assignment
		}

		val membershipService = smartMock[AssignmentMembershipService]
		membershipService.determineMembershipUsers(assignment) returns(users)
		notification.membershipService = membershipService

		notification.recipients should be (users)

		// Don't notify them if they've submitted
		withClue("Shouldn't notify user who has submitted") {
			val submission = new Submission
			submission.universityId = "0133454"
			assignment.addSubmission(submission)
			notification.recipients should be(Seq(users(0)))
		}

		withClue("Shouldn't notify user with extension") { // A different class handles individual extensions
			val extension = new Extension
			extension.universityId = "0123456"
			extension.approve()
			assignment.extensions.add(extension)
			notification.recipients should be(Seq())
		}
	}

	@Test
	def extensionRecipients() {
		val anExtension = new Extension
		anExtension.universityId = "0133454"

		val notification = new SubmissionDueWithExtensionNotification {
			override def extension = anExtension
		}
		notification.userLookup = mock[UserLookupService]
		notification.userLookup.getUserByWarwickUniId("0133454") returns (users(1))
		anExtension.assignment = assignment

		withClue("Should only be sent to the one user who has an extension") {
			notification.recipients should be(Seq(users(1)))
		}

		withClue("Shouldn't be sent if extension user has submitted") {
			val submission = new Submission
			submission.universityId = "0133454"
			assignment.addSubmission(submission)
			notification.recipients should be(Seq())
		}
	}

	private def membershipItem(uniId: String, usercode: String) = {
		val user = new User(usercode)
		user.setWarwickId(uniId)
		MembershipItem(user, Some(uniId), Some(usercode), IncludeType, false)
	}

}
