package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

import scala.jdk.CollectionConverters._

object UpdateLinkedSmallGroupSetsCommand {
  def apply() =
    new UpdateLinkedSmallGroupSetsCommandInternal(
      FindStudentsForUserGroupCommandFactoryImpl,
      UpdateStudentsForUserGroupCommandFactoryImpl
    ) with ComposableCommandWithoutTransaction[Seq[SmallGroupSet]]
      with AutowiringFeaturesComponent
      with AutowiringProfileServiceComponent
      with AutowiringSmallGroupServiceComponent
      with UpdateLinkedSmallGroupSetsDescription
      with UpdateLinkedSmallGroupSetsPermissions
}

class UpdateLinkedSmallGroupSetsCommandInternal(
  findStudentsCommandFactory: FindStudentsForUserGroupCommandFactory,
  updateCommandFactory: UpdateStudentsForUserGroupCommandFactory
) extends CommandInternal[Seq[SmallGroupSet]] with Logging with TaskBenchmarking {

  self: FeaturesComponent with SmallGroupServiceComponent =>

  override def applyInternal(): Seq[SmallGroupSet] = {
    val setsToUpdate = transactional(readOnly = true) {
      smallGroupService.listSetsForMembershipUpdate
    }

    logger.info(s"${setsToUpdate.size} sets need membership updating")

    setsToUpdate.foreach { set =>
      val staticStudentIds = transactional(readOnly = true) {
        val cmd = findStudentsCommandFactory.apply(set.department, set.module, set)
        cmd.populate()
        cmd.doFind = true
        cmd.apply().staticStudentIds
      }
      transactional() {
        val updateCommand = updateCommandFactory.apply(set.department, set.module, set)
        updateCommand.linkToSits = true
        updateCommand.filterQueryString = set.memberQuery
        updateCommand.staticStudentIds.clear()
        updateCommand.staticStudentIds.addAll(staticStudentIds)
        updateCommand.includedStudentIds.clear()
        updateCommand.includedStudentIds.addAll(set.members.knownType.includedUserIds.asJava)
        updateCommand.excludedStudentIds.clear()
        updateCommand.excludedStudentIds.addAll(set.members.knownType.excludedUserIds.asJava)
        updateCommand.apply()
      }
    }

    setsToUpdate
  }

}

trait UpdateLinkedSmallGroupSetsPermissions extends RequiresPermissionsChecking {

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.SmallGroups.UpdateMembership)
  }

}

trait UpdateLinkedSmallGroupSetsDescription extends Describable[Seq[SmallGroupSet]] {

  override lazy val eventName = "UpdateLinkedSmallGroupSets"

  override def describe(d: Description): Unit = {

  }
}
