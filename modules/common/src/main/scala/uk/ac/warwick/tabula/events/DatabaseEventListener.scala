package uk.ac.warwick.tabula.events

import uk.ac.warwick.spring.Wire
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.services.AuditEventService
import uk.ac.warwick.tabula.services.MaintenanceModeEnabledException
import uk.ac.warwick.tabula.services.MaintenanceModeService
import java.io.File
import java.io.FilenameFilter
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.Closeables._
import java.io.ObjectInputStream
import java.io.FileInputStream
import java.util.UUID
import java.io.ObjectOutputStream
import java.io.FileOutputStream
import scala.react.Observing
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.beans.factory.annotation.Autowired

class DatabaseEventListener extends EventListener with Daoisms with InitializingBean with Observing with Logging {

	// TODO FIXME Need to make Wire cleverer and wire placeholders here to get around the need for @Autowired
	// We can't just change to Wire because it's called in afterPropertiesSet, which happens before the other beans exist
	@Autowired var auditEventService: AuditEventService = _
	@Autowired var maintenanceModeService: MaintenanceModeService = _
	var auditDirectory = Wire[File]("${filesystem.auditlog.dir}")
	var createMissingDirs = Wire[JBoolean]("${filesystem.create.missing:false}")

	def save(event: Event, stage: String) {
		if (maintenanceModeService.enabled) {
			val file = new File(auditDirectory, UUID.randomUUID() + "logentry")
			closeThis(new ObjectOutputStream(new FileOutputStream(file))) { stream =>
				stream.writeObject(EventAndStage(event, stage))
			}
		} else {
			auditEventService.save(event, stage)
		}
	}

	def beforeCommand(event: Event) = save(event, "before")
	def afterCommand(event: Event, returnValue: Any) = save(event, "after")
	def onException(event: Event, exception: Throwable) = save(event, "error")

	def startLoggingToFile {
		// nothing to be done, save() will log to file when necessary.
	}

	def stopLoggingToFile {
		// persist files back to database
		logger.info("Writing file based events to database...")
		for (file <- auditDirectory.listFiles(withSuffix("logentry"))) {
			closeThis(new ObjectInputStream(new FileInputStream(file))) { stream =>
				stream.readObject match {
					case event: EventAndStage => auditEventService.save(event.event, event.stage)
				}
			}
			if (!file.delete()) {
				logger.warn("Couldn't immediately delete " + file)
				file.deleteOnExit()
			}
		}
	}

	def afterPropertiesSet {
		if (!auditDirectory.isDirectory) {
			if (createMissingDirs) auditDirectory.mkdirs()
			else throw new IllegalArgumentException("Audit directory " + auditDirectory + " is not a directory")
		}
		// listen for maintenance mode changes
		observe(maintenanceModeService.changingState) { enabled =>
			if (enabled) startLoggingToFile
			else stopLoggingToFile
			true
		}
	}

	def withSuffix(suffix: String): FilenameFilter = new FilenameFilter {
		def accept(file: File, name: String) = name.endsWith(suffix)
	}
}