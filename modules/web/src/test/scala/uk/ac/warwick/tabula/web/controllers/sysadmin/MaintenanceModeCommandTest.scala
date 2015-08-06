package uk.ac.warwick.tabula.web.controllers.sysadmin

import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.tabula.services.MaintenanceModeService
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.MaintenanceModeMessage

class MaintenanceModeCommandTest extends TestBase with Mockito {

	val queue = mock[Queue]
	val service = mock[MaintenanceModeService]

	@Test def populateEmpty {
		service.until returns (None)
		service.message returns (None)

		val cmd = new MaintenanceModeCommand(service)
		cmd.until should not be (null)
		cmd.message should be (null)
	}

	@Test def populateNotEmpty {
		val dt = DateTime.now

		service.until returns (Some(dt))
		service.message returns (Some("yes"))

		val cmd = new MaintenanceModeCommand(service)
		cmd.until should be (dt)
		cmd.message should be ("yes")
	}

	@Test def enable {
		service.until returns (None)
		service.message returns (None)

		val cmd = new MaintenanceModeCommand(service)
		cmd.queue = queue

		val dt = DateTime.now

		cmd.enable = true
		cmd.message = "Sound the alarm"
		cmd.until = dt

		cmd.applyInternal

		verify(service, times(1)).message_=(Some("Sound the alarm"))
		verify(service, times(1)).until_=(Some(dt))
		verify(service, times(1)).enable
		verify(queue, times(1)).send(isA[MaintenanceModeMessage])
	}

	@Test def disable {
		service.until returns (None)
		service.message returns (None)

		val cmd = new MaintenanceModeCommand(service)
		cmd.queue = queue

		val dt = DateTime.now

		cmd.enable = false
		cmd.message = "Sound the alarm"
		cmd.until = dt

		cmd.applyInternal

		// even though it was set in the command, we re-set to None
		verify(service, times(1)).message_=(None)
		verify(service, times(1)).until_=(None)
		verify(service, times(1)).disable
		verify(queue, times(1)).send(isA[MaintenanceModeMessage])
	}

}