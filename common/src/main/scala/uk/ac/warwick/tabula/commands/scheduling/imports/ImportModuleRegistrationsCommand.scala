package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.ModuleRegistrationDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.{CopyModuleRegistrationProperties, ModuleRegistrationRow}


class ImportModuleRegistrationsCommand(course: StudentCourseDetails, courseRows: Seq[ModuleRegistrationRow], modules: Set[Module])
  extends Command[Seq[ModuleRegistration]] with Logging with Unaudited with PropertyCopying with CopyModuleRegistrationProperties {

  PermissionCheck(Permissions.ImportSystemData)

  var moduleRegistrationDao: ModuleRegistrationDao = Wire[ModuleRegistrationDao]

  override def applyInternal(): Seq[ModuleRegistration] = {
    logger.debug("Importing module registration for student " + course.scjCode)

    val records = courseRows.map { modRegRow =>
      val module = modules.find(_.code == Module.stripCats(modRegRow.sitsModuleCode).get.toLowerCase).get
      val existingRegistration = course.moduleRegistrations.find(modRegRow.matches)

      val isTransient = existingRegistration.isEmpty

      val moduleRegistration = existingRegistration match {
        case Some(moduleRegistration: ModuleRegistration) =>
          moduleRegistration
        case _ =>
          val mr = modRegRow.toModuleRegistration(module)
          course.addModuleRegistration(mr)
          mr
      }

      val hasChanged = copyProperties(modRegRow, moduleRegistration)

      if (isTransient || hasChanged || moduleRegistration.deleted) {
        logger.debug(s"Saving changes for $moduleRegistration because ${if (isTransient) "it's a new object" else if (hasChanged) "it's changed" else "it's been un-deleted"}")

        moduleRegistration.deleted = false
        moduleRegistration.lastUpdatedDate = DateTime.now
        moduleRegistrationDao.saveOrUpdate(moduleRegistration)
      }

      moduleRegistration

    }
    markDeleted(course, courseRows)
    records
  }

  def markDeleted(studentCourse: StudentCourseDetails, courseRows: Seq[ModuleRegistrationRow]): Unit = {
    studentCourse.moduleRegistrations.filterNot(_.deleted).foreach { mr =>
      val mrExists = courseRows.exists(_.matches(mr))

      /** Ensure at least there is some record in SITS.If for some reason we couldn't
        * get any SITS data, don't mark all deleted- better to leave as it is **/
      if (courseRows.nonEmpty && !mrExists) {
        mr.markDeleted()
        logger.info("Marking delete for " + mr)
        mr.lastUpdatedDate = DateTime.now
        moduleRegistrationDao.saveOrUpdate(mr)
      }
    }
  }

  override def describe(d: Description): Unit = d.properties("scjCode" -> course.scjCode)

  override def toString: String = s"ImportModuleRegistrationsCommand($course, $courseRows)"
}
