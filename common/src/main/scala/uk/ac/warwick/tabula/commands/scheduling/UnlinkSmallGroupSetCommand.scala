package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.notifications.groups.UnlinkedSmallGroupSetNotification
import uk.ac.warwick.tabula.data.model.{Department, Notification}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object UnlinkSmallGroupSetCommand {
  def apply() =
    new UnlinkSmallGroupSetCommandInternal
      with AutowiringSmallGroupServiceComponent
      with ComposableCommand[Map[Department, Seq[SmallGroupSet]]]
      with UnlinkSmallGroupSetDescription
      with UnlinkSmallGroupSetPermissions
      with UnlinkSmallGroupSetNotifications
}


class UnlinkSmallGroupSetCommandInternal extends CommandInternal[Map[Department, Seq[SmallGroupSet]]] {

  self: SmallGroupServiceComponent =>

  override def applyInternal(): Map[Department, Seq[SmallGroupSet]] = {
    val academicYear = AcademicYear.now()
    val setMap = transactional() {
      smallGroupService.findSmallGroupSetsLinkedToSITSByDepartment(academicYear)
    }
    setMap.map { case (department, sets) => department -> sets.map { set =>
      transactional() {
        set.memberQuery = null
        set.members.knownType.includedUserIds = (set.members.knownType.staticUserIds diff set.members.knownType.excludedUserIds) ++ set.members.knownType.includedUserIds
        smallGroupService.saveOrUpdate(set)
        set
      }
    }
    }
  }

}

trait UnlinkSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.SmallGroups.UpdateMembership)
  }

}

trait UnlinkSmallGroupSetDescription extends Describable[Map[Department, Seq[SmallGroupSet]]] {

  override lazy val eventName = "UnlinkSmallGroupSet"

  override def describe(d: Description): Unit = {

  }

  override def describeResult(d: Description, result: Map[Department, Seq[SmallGroupSet]]): Unit = {
    d.property("updatedSets" -> result.map { case (dept, sets) => dept.code -> sets.map(_.id) })
  }
}

trait UnlinkSmallGroupSetNotifications extends Notifies[Map[Department, Seq[SmallGroupSet]], Map[Department, Seq[SmallGroupSet]]] {

  def emit(result: Map[Department, Seq[SmallGroupSet]]): Seq[UnlinkedSmallGroupSetNotification] = {
    result.map { case (department, sets) =>
      Notification.init(new UnlinkedSmallGroupSetNotification, null, sets, department)
    }.toSeq
  }
}
