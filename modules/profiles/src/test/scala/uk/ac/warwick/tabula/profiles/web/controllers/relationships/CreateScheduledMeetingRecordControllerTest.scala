package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class CreateScheduledMeetingRecordControllerTest extends TestBase with Mockito {

	val controller = new ConvertScheduledMeetingRecordController
	val student = Fixtures.student()
	val studentCourseDetails = student.mostSignificantCourseDetails.get

	// TAB-2430
	@Test
	def getCreateCommand() {
		val cmd = controller.getCreateCommand(null , studentCourseDetails)
		cmd should be(None)
	}
}
