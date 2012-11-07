package uk.ac.warwick.courses.events

import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.services.AuditEventService
import uk.ac.warwick.courses.services.MaintenanceModeEnabledException
import uk.ac.warwick.courses.services.MaintenanceModeService
import org.springframework.beans.factory.annotation.Value
import java.io.File
import java.io.FilenameFilter
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.helpers.Closeables._
import java.io.ObjectInputStream
import java.io.FileInputStream
import java.util.UUID
import java.io.ObjectOutputStream
import java.io.FileOutputStream
import scala.react.Observing

class DatabaseEventListener extends EventListener with Daoisms with InitializingBean with Observing with Logging {

	@Autowired var auditEventService: AuditEventService = _
	@Autowired var maintenanceModeService: MaintenanceModeService = _
	@Value("${filesystem.auditlog.dir}") var auditDirectory: File = _
	@Value("${filesystem.create.missing}") var createMissingDirs: Boolean = false

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