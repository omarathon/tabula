package uk.ac.warwick.tabula.attendance.commands.report

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.NoCurrentUser

class ReportStudentsConfirmCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends TermServiceComponent with AvailablePeriods with ReportStudentsConfirmState
			with FindTermForPeriod with ReportStudentsConfirmCommandValidation with ReportStudentsState {
		val termService = mock[TermService]
		val monitoringPointService = mock[MonitoringPointService]
		val profileService = mock[ProfileService]
	}

	trait Fixture {
		val cmd = new ReportStudentsConfirmCommand(new Department(), NoCurrentUser()) with CommandTestSupport
		cmd.availablePeriods = Seq()
		cmd.unreportedStudentsWithMissed = Seq()
	}

	@Test
	def validateNoStudentsToReport () {
		new Fixture {
			var errors = new BindException(cmd, "command")
			cmd.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("unreportedStudentsWithMissed").size() should be (1)
		}
	}

}
