package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{Submission, Assignment}
import uk.ac.warwick.tabula.services.{UserLookupService, IncludeType, MembershipItem, AssignmentMembershipInfo}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.forms.Extension

class SubmissionDueNotificationTest extends TestBase with Mockito {

	val memberInfo = new AssignmentMembershipInfo(Seq(
		membershipItem(uniId="0123456", usercode="cusaaa"),
		membershipItem(uniId="0133454", usercode="cusaab")
	))

	val assignment = new Assignment {
		override def membershipInfo = memberInfo
	}

	val users = assignment.membershipInfo.items.map { _.user }

	@Test
	def generalRecipients() {
		val notification = new SubmissionDueGeneralNotification {
			override def assignment = SubmissionDueNotificationTest.this.assignment
		}

		val users = assignment.membershipInfo.items.map { _.user }

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
