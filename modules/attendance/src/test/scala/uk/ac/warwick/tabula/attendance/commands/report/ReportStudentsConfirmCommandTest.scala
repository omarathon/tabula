package uk.ac.warwick.tabula.attendance.commands.report

import uk.ac.warwick.tabula.{Mockito, TestBase}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{StudentMember, Department}
import uk.ac.warwick.tabula.NoCurrentUser
import uk.ac.warwick.tabula.services.{ProfileService, MonitoringPointService, TermService}

class ReportStudentsConfirmCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends ReportStudentsConfirmState	with ReportStudentsConfirmCommandValidation  {
		val termService = mock[TermService]
		val monitoringPointService = mock[MonitoringPointService]
		val profileService = mock[ProfileService]
	}

	trait Fixture {
		val cmd = new ReportStudentsConfirmCommand(new Department(), NoCurrentUser()) with CommandTestSupport
		val availablePeriod = "Autumn"
		val unavailablePeriod = "Springtime"
		val unspecifiedPeriod = "Summertime"
		cmd.availablePeriods = Seq((availablePeriod, true), (unavailablePeriod, false))

	}

	@Test
	def valid () {
		new Fixture {
			var errors = new BindException(cmd, "command")
			cmd.unreportedStudentsWithMissed = Seq((mock[StudentMember], 1))
			cmd.period = availablePeriod
			cmd.confirm = true
			cmd.validate(errors)
			errors.hasFieldErrors should be (false)
		}
	}

	@Test
	def unconfirmed () {
		new Fixture {
			var errors = new BindException(cmd, "command")
			cmd.unreportedStudentsWithMissed = Seq((mock[StudentMember], 1))
			cmd.period = availablePeriod
			cmd.validate(errors)
			errors.hasFieldErrors should be (true)
			errors.getErrorCount should be (1)
			errors.getFieldErrors("confirm").size() should be (1)
		}
	}

	@Test
	def unavailablePeriod () {
		new Fixture {
			var errors = new BindException(cmd, "command")
			cmd.period = unavailablePeriod
			cmd.confirm = true
			cmd.unreportedStudentsWithMissed = Seq((mock[StudentMember], 1))
			cmd.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getErrorCount should be (1)
			errors.getFieldErrors("period").size() should be (1)
		}
	}

	@Test
	def periodNotInAvailablePeriods() {
		new Fixture {
			var errors = new BindException(cmd, "command")
			cmd.period = unspecifiedPeriod
			cmd.confirm = true
			cmd.unreportedStudentsWithMissed = Seq((mock[StudentMember], 1))
			cmd.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getErrorCount should be (1)
			errors.getFieldErrors("period").size() should be (1)
		}
	}

	@Test
	def validateNoStudentsToReport () {
		new Fixture {
			var errors = new BindException(cmd, "command")
			cmd.unreportedStudentsWithMissed = Seq()
			cmd.confirm = true
			cmd.period = availablePeriod
			cmd.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getErrorCount should be (1)
			errors.getFieldErrors("unreportedStudentsWithMissed").size() should be (1)
		}
	}

}
