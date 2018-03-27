package uk.ac.warwick.tabula.data.model.notifications

import org.joda.time.DateTime
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.CurrentAcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.cm2.Cm2StudentFeedbackAdjustmentNotification
import uk.ac.warwick.tabula.data.model.notifications.coursework.FeedbackPublishedNotification
import uk.ac.warwick.tabula.notifications.{MyWarwickNotificationListener, MyWarwickServiceComponent}
import uk.ac.warwick.tabula.web.views.{TextRenderer, TextRendererComponent}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.mywarwick.MyWarwickService
import uk.ac.warwick.util.mywarwick.model.request.Activity


class MyWarwickNotificationListenerTest extends TestBase with Mockito {

	var listener = new MyWarwickNotificationListener with TextRendererComponent
		with FeaturesComponent
		with MyWarwickServiceComponent
		with TopLevelUrlComponent {

		def textRenderer: TextRenderer = smartMock[TextRenderer]

		def features: Features = emptyFeatures
		var myWarwickService: MyWarwickService = smartMock[MyWarwickService]

		def toplevelUrl: String = ""
	}

	trait Fixture extends CurrentAcademicYear {
		val user = new User("cusxad")
		user.setFoundUser(true)

		val currentUser = new CurrentUser(user, user);
		val feedback = new AssignmentFeedback

		val module: Module = Fixtures.module(code = "ls101")
		val assignment: Assignment = Fixtures.assignment("test")
		assignment.id = "12345"
		assignment.academicYear = academicYear
		assignment.closeDate = DateTime.now.plusDays(30)
		assignment.members = UserGroup.ofUsercodes
		assignment.module = module
		module.assignments.add(assignment)
		feedback.assignment = assignment
		assignment.feedbacks.add(feedback)
	}

	@Test def alertNotifcation() {
		new Fixture {
			val fpn = new FeedbackPublishedNotification
			val rn = new RecipientNotificationInfo(fpn, user)
			fpn.recipientNotificationInfos.add(rn)
			val n = Notification.init(fpn, currentUser.apparentUser, Seq(feedback), feedback.assignment)
			listener.listen(n)
			verify(listener.myWarwickService, times(1)).sendAsNotification(any[Activity])
		}
	}


	@Test def activitiyNotifcation() {
		new Fixture {
			val sfan = new Cm2StudentFeedbackAdjustmentNotification
			val rn = new RecipientNotificationInfo(sfan, user)
			sfan.recipientNotificationInfos.add(rn)
			val n = Notification.init(sfan, currentUser.apparentUser, Seq(feedback), feedback.assignment)
			listener.listen(n)
			verify(listener.myWarwickService, times(1)).sendAsActivity(any[Activity])
		}
	}


}