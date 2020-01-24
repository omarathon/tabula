package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.submission.MitCircsShareSubmissionCommand
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesAcuteOutcome, MitigatingCircumstancesGrading, MitigatingCircumstancesRejectionReason, MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmissionState}
import uk.ac.warwick.tabula.data.model.notifications.coursework.{ExtensionRequestCreatedNotification, ExtensionRequestModifiedNotification}
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{EditedMeetingRecordApprovalNotification, MeetingRecordRejectedNotification, NewMeetingRecordApprovalNotification, ScheduledMeetingRecordConfirmNotification}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.jdk.CollectionConverters._

object HandleDeceasedStudentCommand {
  def apply(student: StudentMember) =
    new HandleDeceasedStudentCommandInternal(student)
      with AutowiringExtensionServiceComponent
      with AutowiringRelationshipServiceComponent
      with AutowiringMeetingRecordServiceComponent
      with AutowiringSmallGroupServiceComponent
      with AutowiringMitCircsSubmissionServiceComponent
      with NotificationServiceComponent
      with ScheduledNotificationServiceComponent
      with ComposableCommand[Unit]
      with HandleDeceasedStudentPermissions
      with HandleDeceasedStudentDescription
}

trait HandleDeceasedStudentState {
  def student: StudentMember
}

class HandleDeceasedStudentCommandInternal(val student: StudentMember) extends CommandInternal[Unit]
  with HandleDeceasedStudentState {
  self: ExtensionServiceComponent
    with RelationshipServiceComponent
    with MeetingRecordServiceComponent
    with NotificationServiceComponent
    with ScheduledNotificationServiceComponent
    with SmallGroupServiceComponent
    with MitCircsSubmissionServiceComponent =>

  override def applyInternal(): Unit = {
    if (student.deceased) {
      cleanUpNotifications(
        handlePendingExtensions() ++
        handleRelationships() ++
        handleSmallGroups() ++
        handleMitigatingCircumstances()
      )
    }
  }

  private def handlePendingExtensions(): Seq[Extension] = {
    val extensions = extensionService.getAllExtensionRequests(MemberOrUser(student).asUser).filter(_.awaitingReview)
    extensions.foreach(_.reject("Automatically removing request from deceased student"))
    extensions.foreach(extensionService.saveOrUpdate)
    extensions
  }

  private def handleRelationships(): Seq[ToEntityReference] = {
    val current = relationshipService.getAllCurrentRelationships(student)
    val future = relationshipService.getAllFutureRelationships(student)
    current.filter(rel => !Option(rel.endDate).exists(_.isBeforeNow)).foreach { rel =>
      rel.endDate = DateTime.now
      relationshipService.saveOrUpdate(rel)
    }
    relationshipService.removeFutureStudentRelationships(future)
    current ++ future ++ handleMeetingRecords(current)
  }

  private def handleMeetingRecords(relationships: Seq[StudentRelationship]): Seq[AbstractMeetingRecord] = {
    relationships.flatMap(rel =>
      rel.agentMember.map(MemberOrUser(_).asUser).map(agentUser =>
        meetingRecordService.listAll(rel).filter(abstractMeeting => HibernateHelpers.initialiseAndUnproxy(abstractMeeting) match {
          case meeting: MeetingRecord => meeting.pendingActionBy(new CurrentUser(agentUser, agentUser))
          case scheduled: ScheduledMeetingRecord => scheduled.creator == rel.agentMember.get
        })
      ).getOrElse(Seq())
    )
  }

  private def handleSmallGroups(): Seq[SmallGroup] = {
    val groups = smallGroupService.findSmallGroupsByStudent(student.asSsoUser)
    groups.foreach { group =>
      smallGroupService.removeUserFromGroup(student.asSsoUser, group)
    }

    // Manually included in SmallGroupSets
    groups.map(_.groupSet).distinct.foreach { groupSet =>
      groupSet.members.items.find { case (user, includeType) =>
        user.getWarwickId == student.universityId && includeType == UserGroupItemType.Included
      }.foreach { case (user, _) => groupSet.members.remove(user) }
    }

    groups
  }

  private def handleMitigatingCircumstances(): Seq[MitigatingCircumstancesSubmission] = {
    val allSubmissions = mitCircsSubmissionService.submissionsForStudent(student)

    // Remove any existing share permissions
    allSubmissions.foreach { submission =>
      val removeSharingCommand = MitCircsShareSubmissionCommand.remove(submission, new User("system"))
      removeSharingCommand.usercodes.addAll(removeSharingCommand.grantedRole.toSeq.flatMap(_.users.knownType.allIncludedIds).asJava)
      removeSharingCommand.apply()
    }

    val submissionsToWithdraw = allSubmissions.filter(_.isDraft)
    submissionsToWithdraw.foreach { submission =>
      submission.withdraw()
      mitCircsSubmissionService.saveOrUpdate(submission)
    }

    val submissionsToRecordOutcomes = allSubmissions.filter { submission =>
      Set[MitigatingCircumstancesSubmissionState](
        MitigatingCircumstancesSubmissionState.Submitted, MitigatingCircumstancesSubmissionState.ReadyForPanel
      ).contains(submission.state)
    }
    submissionsToRecordOutcomes.foreach { submission =>
      submission.panel = null
      submission.outcomeGrading = MitigatingCircumstancesGrading.Rejected
      submission.outcomeReasons = "Automatically removing request from deceased student"
      submission.acuteOutcome = MitigatingCircumstancesAcuteOutcome.HandledElsewhere
      submission.rejectionReasons = Seq(MitigatingCircumstancesRejectionReason.Other)
      submission.rejectionReasonsOther = "Automatically removing request from deceased student"
      submission.affectedAssessments.asScala.foreach { item =>
        item.acuteOutcome = null
      }
      submission.outcomesRecorded()
      mitCircsSubmissionService.saveOrUpdate(submission)
    }

    allSubmissions
  }

  private def completeNotification(completedBy: User)(notification: ActionRequiredNotification): Unit = {
    notification match {
      case _: AllCompletedActionRequiredNotification => notification.actionCompleted(completedBy)
      case recipient: RecipientCompletedActionRequiredNotification => recipient.actionCompletedByOther(completedBy)
      case _ => throw new IllegalArgumentException
    }
  }

  private def cleanUpNotifications(entities: Seq[ToEntityReference]): Unit = {
    val user = new AnonymousUser()

    val notifications: Seq[ActionRequiredNotification] = entities.flatMap {

      case extension: Extension =>
        notificationService.findActionRequiredNotificationsByEntityAndType[ExtensionRequestCreatedNotification](extension) ++
          notificationService.findActionRequiredNotificationsByEntityAndType[ExtensionRequestModifiedNotification](extension)

      case meeting: MeetingRecord =>
        meeting.approvals.asScala.flatMap(approval => notificationService.findActionRequiredNotificationsByEntityAndType[MeetingRecordRejectedNotification](approval)) ++
          notificationService.findActionRequiredNotificationsByEntityAndType[NewMeetingRecordApprovalNotification](meeting) ++
          notificationService.findActionRequiredNotificationsByEntityAndType[EditedMeetingRecordApprovalNotification](meeting)

      case scheduledMeeting: ScheduledMeetingRecord =>
        notificationService.findActionRequiredNotificationsByEntityAndType[ScheduledMeetingRecordConfirmNotification](scheduledMeeting)

      case _ =>
        Seq()
    }

    if (notifications.nonEmpty) {
      notifications.foreach(completeNotification(user))
      notificationService.update(
        notifications.map(_.asInstanceOf[Notification[_, _]]),
        user
      )
    }

    entities.foreach(scheduledNotificationService.removeInvalidNotifications)
  }

}

trait HandleDeceasedStudentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.ImportSystemData)
  }

}

trait HandleDeceasedStudentDescription extends Describable[Unit] {
  self: HandleDeceasedStudentState =>

  override lazy val eventName = "HandleDeceasedStudent"

  override def describe(d: Description): Unit =
    d.studentIds(student.universityId)
}
