package uk.ac.warwick.tabula.events

import java.io._
import java.util.UUID

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.{Autowired, Value}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AuditEventService, MaintenanceModeService}

import scala.util.Using

class DatabaseEventListener extends EventListener with InitializingBean with Logging {

  @Autowired var auditEventService: AuditEventService = _
  @Autowired var maintenanceModeService: MaintenanceModeService = _
  @Value("${filesystem.auditlog.dir}") var auditDirectory: File = _
  @Value("${filesystem.create.missing}") var createMissingDirs: Boolean = false

  def save(event: Event, stage: String): Unit = {
    if (maintenanceModeService.enabled) {
      val file = new File(auditDirectory, UUID.randomUUID() + "logentry")
      Using.resource(new ObjectOutputStream(new FileOutputStream(file))) { stream =>
        stream.writeObject(EventAndStage(event, stage))
      }
    } else {
      auditEventService.save(event, stage)
    }
  }

  def beforeCommand(event: Event): Unit = save(event, "before")

  def afterCommand(event: Event, returnValue: Any, beforeEvent: Event): Unit = save(event, "after")

  def onException(event: Event, exception: Throwable): Unit = save(event, "error")

  def startLoggingToFile(): Unit = {
    // nothing to be done, save() will log to file when necessary.
  }

  def stopLoggingToFile(): Unit = {
    // persist files back to database
    logger.info("Writing file based events to database...")
    for (file <- auditDirectory.listFiles(withSuffix("logentry"))) {
      Using.resource(new ObjectInputStream(new FileInputStream(file))) { stream =>
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

  def afterPropertiesSet(): Unit = {
    if (!auditDirectory.isDirectory) {
      if (createMissingDirs) auditDirectory.mkdirs()
      else throw new IllegalArgumentException("Audit directory " + auditDirectory + " is not a directory")
    }
    // listen for maintenance mode changes
    maintenanceModeService.changingState.observe { enabled =>
      if (enabled) startLoggingToFile()
      else stopLoggingToFile()
    }
  }

  def withSuffix(suffix: String): FilenameFilter = new FilenameFilter {
    def accept(file: File, name: String): Boolean = name.endsWith(suffix)
  }
}
