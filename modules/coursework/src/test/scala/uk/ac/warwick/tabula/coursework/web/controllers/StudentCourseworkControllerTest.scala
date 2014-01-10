package uk.ac.warwick.tabula.coursework.web.controllers

import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.coursework.web.controllers.StudentCourseworkCommand._

class StudentCourseworkControllerTest extends TestBase with Mockito {

	private trait Fixture {
		val assignment = newDeepAssignment("LA101")
		assignment.closeDate = new DateTime()

		val submission = Fixtures.submission("0000001")
		submission.submittedDate = new DateTime()
	}

	@Test
	def historicalAssignmentsNoDupes {
		new Fixture {
			val  assignmentInfo = Map("assignment" -> assignment,
				"submission" -> Option(submission))

			// A late formative assignment that has also been submitted will appear in both of the following
			val assignmentsWithSubmissionInfo = Seq(assignmentInfo)
			val lateFormativeAssignmentsInfo = Seq(assignmentInfo)

			val historicalAssignmentsInfo = getHistoricAssignmentsInfo(Nil, assignmentsWithSubmissionInfo, lateFormativeAssignmentsInfo)
			historicalAssignmentsInfo.size should be(1)
		}
	}

}
