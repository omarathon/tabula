package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.EarlyRequestInfo
import uk.ac.warwick.tabula.commands.sysadmin.{DepartmentMandatoryPermissionsCommand, DepartmentMandatoryPermissionsInfo}
import uk.ac.warwick.tabula.data.model.{CourseType, Department}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.SlackService
import uk.ac.warwick.tabula.services.SlackService._
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean
import uk.ac.warwick.tabula.web.Routes

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class DepartmentMandatoryPermissionsWarningJob extends AutowiredJobBean {

  override def executeInternal(context: JobExecutionContext): Unit =
    if (features.schedulingDepartmentMandatoryPermissionsWarning)
      exceptionResolver.reportExceptions {
        EarlyRequestInfo.wrap() {
          def flattenChildren(info: DepartmentMandatoryPermissionsInfo, parent: Option[DepartmentMandatoryPermissionsInfo] = None): Seq[(DepartmentMandatoryPermissionsInfo, Option[DepartmentMandatoryPermissionsInfo])] =
            (info.copy(children = Nil), parent) +: info.children.flatMap(flattenChildren(_, Some(info)))

          def hasMissingUAM(d: (DepartmentMandatoryPermissionsInfo, Option[DepartmentMandatoryPermissionsInfo])): Boolean =
            d match { case (info, parent) =>
              !info.hasActiveUAM && !parent.exists(_.hasActiveUAM)
            }

          def hasMitCircsNotEnabled(d: (DepartmentMandatoryPermissionsInfo, Option[DepartmentMandatoryPermissionsInfo])): Boolean =
            d match { case (info, parent) =>
              (
                // Has at least one non-PGR active student
                (info.activeStudents - CourseType.PGR).values.exists(_ > 0) &&

                // Not enabled
                !info.department.enableMitCircs && !parent.exists(_.department.enableMitCircs)
              )
            }

          def hasMissingMCO(d: (DepartmentMandatoryPermissionsInfo, Option[DepartmentMandatoryPermissionsInfo])): Boolean =
            d match { case (info, parent) =>
              (
                // Has at least one non-PGR active student
                (info.activeStudents - CourseType.PGR).values.exists(_ > 0) &&

                // No active MCO
                !info.hasActiveMCO && !parent.exists(_.hasActiveMCO)
              )
            }

          val topLevelUrl: String = Wire.property("${toplevel.url}")

          val mitCircsDepartmentCodesToIgnore =
            Wire.property("${departmentMandatoryPermissionsWarning.mitCircsDepartmentCodesToIgnore}")
              .split(',')
              .filter(_.hasText)
              .map(_.trim().toLowerCase())

          val allInfo = DepartmentMandatoryPermissionsCommand().apply()

          val departmentsWithMissingUAM = allInfo.flatMap(flattenChildren(_)).filter(hasMissingUAM)

          val mitCircsInfo =
            allInfo.filterNot(info => mitCircsDepartmentCodesToIgnore.contains(info.department.code))
              .flatMap(flattenChildren(_))

          val departmentsWithMitCircsNotEnabled = mitCircsInfo.filter(hasMitCircsNotEnabled)
          val departmentsWithMissingMCO = mitCircsInfo.filter { info => !hasMitCircsNotEnabled(info) && hasMissingMCO(info) }

          if (departmentsWithMissingUAM.nonEmpty || departmentsWithMitCircsNotEnabled.nonEmpty || departmentsWithMissingMCO.nonEmpty) {
            val webhookUrl = Wire.property("${departmentMandatoryPermissionsWarning.slackWebhookUrl}")
            val slackService = Wire[SlackService]

            val fallbackText =
              Seq(
                if (departmentsWithMissingUAM.nonEmpty)
                  Some(s"There ${if (departmentsWithMissingUAM.size != 1) "are" else "is"} ${departmentsWithMissingUAM.size} department${if (departmentsWithMissingUAM.size != 1) "s" else ""} without a valid User Access Manager: ${departmentsWithMissingUAM.map(_._1.department.code.toUpperCase()).mkString(", ")}")
                else None,
                if (departmentsWithMitCircsNotEnabled.nonEmpty)
                  Some(s"There ${if (departmentsWithMitCircsNotEnabled.size != 1) "are" else "is"} ${departmentsWithMitCircsNotEnabled.size} department${if (departmentsWithMitCircsNotEnabled.size != 1) "s" else ""} with Mitigating Circumstances disabled: ${departmentsWithMitCircsNotEnabled.map(_._1.department.code.toUpperCase()).mkString(", ")}")
                else None,
                if (departmentsWithMissingMCO.nonEmpty)
                  Some(s"There ${if (departmentsWithMissingMCO.size != 1) "are" else "is"} ${departmentsWithMissingMCO.size} department${if (departmentsWithMissingMCO.size != 1) "s" else ""} without a valid Mitigating Circumstances Officer: ${departmentsWithMissingMCO.map(_._1.department.code.toUpperCase()).mkString(", ")}")
                else None,
              ).flatten.mkString("\n")

            def departmentListItemMarkdown(info: DepartmentMandatoryPermissionsInfo, route: Department => String): String =
              s"• <$topLevelUrl${route(info.department)}|${info.department.name}> (*${info.department.code.toUpperCase}*) - ${CourseType.all.filter(c => info.activeStudents(c) > 0).map(c => s"${info.activeStudents(c)} ${c.description}").mkString(", ")}"

            val sections =
              Seq(
                if (departmentsWithMissingUAM.nonEmpty)
                  Some(SectionBlock(
                    text = Text.markdown(
                      s""":crown: There ${if (departmentsWithMissingUAM.size != 1) "are" else "is"} ${departmentsWithMissingUAM.size} department${if (departmentsWithMissingUAM.size != 1) "s" else ""} *without a valid User Access Manager*:
                         |
                         |${departmentsWithMissingUAM.map { case (info, _) => departmentListItemMarkdown(info, Routes.sysadmin.Departments.admins) }.mkString("\n")}
                         |
                         |:honk-warning: Tabula system admins will need to contact the Head of Department for someone to assume the UAM role as soon as practically possible.""".stripMargin
                    )
                  ))
                else None,
                if (departmentsWithMitCircsNotEnabled.nonEmpty)
                  Some(SectionBlock(
                    text = Text.markdown(
                      s""":heavy_heart_exclamation_mark_ornament: There ${if (departmentsWithMitCircsNotEnabled.size != 1) "are" else "is"} ${departmentsWithMitCircsNotEnabled.size} department${if (departmentsWithMissingUAM.size != 1) "s" else ""} *with Mitigating Circumstances disabled*:
                         |
                         |${departmentsWithMitCircsNotEnabled.map { case (info, _) => departmentListItemMarkdown(info, Routes.admin.department.settings.mitigatingCircumstances) }.mkString("\n")}
                         |
                         |:honk-warning: These students currently cannot submit claims for mitigation and the department should rectify this as soon as possible.""".stripMargin
                    )
                  ))
                else None,
                if (departmentsWithMissingMCO.nonEmpty)
                  Some(SectionBlock(
                    text = Text.markdown(
                      s""":guardsman: There ${if (departmentsWithMissingMCO.size != 1) "are" else "is"} ${departmentsWithMissingMCO.size} department${if (departmentsWithMissingMCO.size != 1) "s" else ""} *without a valid Mitigating Circumstances Officer*:
                         |
                         |${departmentsWithMissingMCO.map { case (info, _) => departmentListItemMarkdown(info, Routes.admin.department.permissions) }.mkString("\n")}
                         |
                         |:honk-warning: These students currently cannot submit claims for mitigation and the department should rectify this as soon as possible.""".stripMargin
                    )
                  ))
                else None,
                Some(SectionBlock(
                  text = Text.markdown(s"<$topLevelUrl${Routes.sysadmin.Departments.mandatoryPermissions}|View the full report>")
                ))
              ).flatten

            // Add dividers between each section
            val it = sections.iterator
            var sectionsWithDividers = mutable.ListBuffer[LayoutBlock]()
            sectionsWithDividers += it.next()
            while (it.hasNext) {
              sectionsWithDividers += DividerBlock()
              sectionsWithDividers += it.next()
            }

            val message = MessagePayload.blocks(
              fallbackText = fallbackText,
              blocks = sectionsWithDividers.toSeq
            )

            Await.result(slackService.postMessage(webhookUrl, message), Duration.Inf)
          }
        }
      }

}
