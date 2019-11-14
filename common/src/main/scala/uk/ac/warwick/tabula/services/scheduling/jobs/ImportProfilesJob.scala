package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext, Scheduler}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.EarlyRequestInfo
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportProfilesCommand
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.SchedulingHelpers._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.elasticsearch.AuditEventQueryService
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

import scala.concurrent.Await
import scala.concurrent.duration._

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ImportProfilesJob extends AutowiredJobBean {

  private val moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
  private val scheduler = Wire[Scheduler]
  private val auditEventQueryService = Wire[AuditEventQueryService]

  override def executeInternal(context: JobExecutionContext): Unit = {
    if (features.schedulingProfilesImport)
      transactional() {
        exceptionResolver.reportExceptions {
          EarlyRequestInfo.wrap() {
            val auditEvents = Await.result(auditEventQueryService.query("eventType:ImportProfiles", 0, 1000), 1.minute)

            moduleAndDepartmentService.allRootDepartments
              .sortBy { dept =>
                auditEvents
                  .filter(event =>
                    event.data == "{\"department\":\"%s\"}".format(dept.code)
                    // legacy imports
                    || event.data == "{\"deptCode\":\"%s\"}".format(dept.code)
                  )
                  .find(_.isSuccessful)
                  .map(_.eventDate.getMillis)
              }
              .foreach { dept =>
                scheduler.scheduleNow[ImportProfilesSingleDepartmentJob]("departmentCode" -> dept.code)
              }
          }
        }
      }
  }

}

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ImportProfilesSingleDepartmentJob extends AutowiredJobBean with Logging {

  override def executeInternal(context: JobExecutionContext): Unit =
    if (features.schedulingProfilesImport)
      exceptionResolver.reportExceptions {
        EarlyRequestInfo.wrap() {
          val deptCode = Option(context.getMergedJobDataMap.getString("departmentCode")).flatMap(_.maybeText)
          if (deptCode.isEmpty) {
            logger.error("Tried to import profiles for a department, but no department code was found.")
          } else {
            val cmd = new ImportProfilesCommand()
            cmd.deptCode = deptCode.get
            cmd.apply()
          }
        }
      }

}
