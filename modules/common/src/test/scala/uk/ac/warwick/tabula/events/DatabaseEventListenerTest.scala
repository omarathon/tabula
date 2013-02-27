package uk.ac.warwick.tabula.events

import uk.ac.warwick.tabula.{TestBase, Mockito, Fixtures, ItemNotFoundException}
import uk.ac.warwick.tabula.services.AuditEventService
import uk.ac.warwick.tabula.services.MaintenanceModeEnabledException
import uk.ac.warwick.tabula.services.MaintenanceModeServiceImpl
import org.joda.time.DateTime
import java.io.File
import java.util.UUID

class DatabaseEventListenerTest extends TestBase with Mockito {
	
	val listener = new DatabaseEventListener
	
	val auditEventService = mock[AuditEventService]
	val maintenanceModeService = new MaintenanceModeServiceImpl
	
	listener.auditEventService = auditEventService
	listener.maintenanceModeService = maintenanceModeService
	listener.auditDirectory = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID() + "audit")
	listener.auditDirectory.deleteOnExit()
	listener.createMissingDirs = true
	
	@Test def maintenanceModeDisabled {
		maintenanceModeService.disable
		
		val event = Event(
			id="event", name="AddAssignment", userId="cuscav", realUserId="cuscav", 
			date=DateTime.now, extra=Map()
		)
		
		listener.beforeCommand(event)
		there was one(auditEventService).save(event, "before")
		
		listener.afterCommand(event, Fixtures.assignment("my assignment"))
		there was one(auditEventService).save(event, "after")
		
		listener.onException(event, new ItemNotFoundException)
		there was one(auditEventService).save(event, "error")
	}
	
	@Test def maintenanceMode {
		listener.afterPropertiesSet
		maintenanceModeService.enable
		
		val event = Event(
			id="event", name="AddAssignment", userId="cuscav", realUserId="cuscav", 
			date=DateTime.now, extra=Map()
		)
		
		listener.beforeCommand(event)
		there was no(auditEventService).save(event, "before")
		
		listener.afterCommand(event, Fixtures.assignment("my assignment"))
		there was no(auditEventService).save(event, "after")
		
		listener.onException(event, new ItemNotFoundException)
		there was no(auditEventService).save(event, "error")
		
		// Ho ho ho, magic time - observation deck!
		maintenanceModeService.disable
		
		// That should flush the 3 events to the db
		there was one(auditEventService).save(event, "before")
		there was one(auditEventService).save(event, "after")
		there was one(auditEventService).save(event, "error")
	}

}
