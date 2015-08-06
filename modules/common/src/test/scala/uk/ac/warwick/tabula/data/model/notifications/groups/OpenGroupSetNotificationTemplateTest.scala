package uk.ac.warwick.tabula.data.model.notifications.groups

import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.tabula.{SmallGroupFixture, FreemarkerTestHelpers, TestBase}

class OpenGroupSetNotificationTemplateTest extends TestBase with FreemarkerTestHelpers with FreemarkerRendering {

	private trait NotificationFixture extends SmallGroupFixture {

		val urlModel = new StubFreemarkerDirectiveModel

		implicit val config = newFreemarkerConfiguration
	}

	@Test
	def includesTheNameOfEachGroupSet {
		new NotificationFixture {
			val output =
				renderToString(OpenSmallGroupSetsNotification.templateLocation,
					Map("formatsString" -> "great", "groupsets" -> List(groupSet1, groupSet2), "profileUrl" -> "profileUrl"))
			output should include(groupSet1.name)
			output should include(groupSet2.name)
		}}


}
