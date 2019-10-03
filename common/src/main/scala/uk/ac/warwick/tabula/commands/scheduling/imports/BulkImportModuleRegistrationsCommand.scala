package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import BulkImportModuleRegistrationsCommand._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.services.scheduling.ModuleRegistrationImporter.ModuleRegistrationsByAcademicYearQuery
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringModuleRegistrationServiceComponent, ModuleAndDepartmentServiceComponent, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.{AutowiringSitsDataSourceComponent, CopyModuleRegistrationProperties, SitsDataSourceComponent}

import scala.collection.immutable.HashMap
import scala.collection.JavaConverters._
import scala.util.Try

object BulkImportModuleRegistrationsCommand {

  type Result = Unit
  type Command = Appliable[Result] with BulkImportModuleRegistrationsState with BulkImportModuleRegistrationsState with SelfValidating

  def apply(academicYear: AcademicYear) = new BulkImportModuleRegistrationsCommandInternal(academicYear)
    with ComposableCommand[Result]
    with BulkImportModuleRegistrationsPermissions
    with CopyModuleRegistrationProperties
    with PropertyCopying
    with AutowiringSitsDataSourceComponent
    with AutowiringModuleRegistrationServiceComponent
    with AutowiringModuleAndDepartmentServiceComponent
    with Logging with Unaudited
}

class BulkImportModuleRegistrationsCommandInternal(val academicYear: AcademicYear) extends CommandInternal[Result] with BulkImportModuleRegistrationsState
  with TaskBenchmarking {

  self: SitsDataSourceComponent with ModuleRegistrationServiceComponent with ModuleAndDepartmentServiceComponent with CopyModuleRegistrationProperties
    with Logging =>

  def applyInternal(): Result = benchmarkTask(s"Importing module registrations for $academicYear"){
    val params = HashMap(("academicYear", academicYear)).asJava
    val rows = benchmarkTask(s"Fetching registrations from SITS") {
      new ModuleRegistrationsByAcademicYearQuery(sitsDataSource).executeByNamedParam(params).asScala.distinct
    }

    val modulesBySitsCode = benchmarkTask(s"Fetching Tabula modules") {
      rows
        .groupBy(_.sitsModuleCode)
        .keys
        .flatMap { sitsModuleCode =>
          moduleAndDepartmentService.getModuleBySitsCode(sitsModuleCode).map(m => (sitsModuleCode, m))
        }.toMap
    }

    // key the existing registrations by scj code and module to make finding them faster
    val existingRegistrations =  benchmarkTask(s"Fetching existing module registrations") {
      moduleRegistrationService.getByYear(academicYear)
    }
    val existingRegistrationsGrouped = benchmarkTask(s"Keying module registrations by scj and module code") {
      existingRegistrations.groupBy(mr => (mr._scjCode, mr.module.code))
    }

    // registrations that we found in SITS that already existed in Tabula (don't delete these)
    val foundRegistrations = benchmarkTask(s"Updating registrations") { rows.flatMap(row => {
      val existing = existingRegistrationsGrouped.getOrElse((row.scjCode, row.moduleCode.orNull), Nil).find(row.matches)
      val registration = existing.orElse(modulesBySitsCode.get(row.sitsModuleCode).map(row.toModuleRegistration))

      registration match {
        case Some(r) =>
          val hasChanged = copyProperties(row, r)

          if (existing.isEmpty || hasChanged || r.deleted) {
            logger.debug(s"Saving changes for $registration")

            r.deleted = false
            r.lastUpdatedDate = DateTime.now
            try {
              moduleRegistrationService.saveOrUpdate(r)
            } catch {
              case t: Throwable => logger.error(s"Unable to save module registration $r", t)
            }
          }
        case None => logger.warn(s"No module exists in Tabula for $row - Not importing")
      }

      existing
    })}

    benchmarkTask(s"Deleting registrations that weren't found in SITS") {
      val missingRegistrations = existingRegistrations.toSet -- foundRegistrations
      missingRegistrations.foreach(mr => {
        logger.debug(s"Marking $mr as deleted")
        mr.markDeleted()
        mr.lastUpdatedDate = DateTime.now
        try {
          moduleRegistrationService.saveOrUpdate(mr)
        } catch {
          case t: Throwable => logger.error(s"Unable to delete module registration $mr", t)
        }
      })
    }

  }
}

trait BulkImportModuleRegistrationsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: BulkImportModuleRegistrationsState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.ImportSystemData)
  }
}


trait BulkImportModuleRegistrationsState {
 val academicYear: AcademicYear
}