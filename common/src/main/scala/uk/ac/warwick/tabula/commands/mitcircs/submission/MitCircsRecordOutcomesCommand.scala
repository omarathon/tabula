package uk.ac.warwick.tabula.commands.mitcircs.submission

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.{BindingResult, Errors}
import MitCircsRecordOutcomesCommand._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesGrading.Rejected
import uk.ac.warwick.tabula.data.model.mitcircs.{MitCircsExamBoardRecommendation, MitigatingCircumstancesAffectedAssessment, MitigatingCircumstancesGrading, MitigatingCircumstancesRejectionReason, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.userlookup.User

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

object MitCircsRecordOutcomesCommand {

  type Result = MitigatingCircumstancesSubmission
  type Command = Appliable[Result] with MitCircsRecordOutcomesState with MitCircsRecordOutcomesRequest with SelfValidating
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Manage

  def apply(submission: MitigatingCircumstancesSubmission, user: User) = new MitCircsRecordOutcomesCommandInternal(submission, user)
    with ComposableCommand[Result]
    with MitCircsRecordOutcomesRequest
    with MitCircsRecordOutcomesValidation
    with MitCircsRecordOutcomesPermissions
    with MitCircsRecordOutcomesDescription
    with MitCircsSubmissionSchedulesNotifications
    with AutowiringMitCircsSubmissionServiceComponent
    with AutowiringModuleAndDepartmentServiceComponent
}

class MitCircsRecordOutcomesCommandInternal(val submission: MitigatingCircumstancesSubmission, val user: User) extends CommandInternal[Result]
  with MitCircsRecordOutcomesState with MitCircsRecordOutcomesValidation with BindListener {

  self: MitCircsRecordOutcomesRequest with MitCircsSubmissionServiceComponent with ModuleAndDepartmentServiceComponent =>

  override def onBind(result: BindingResult): Unit = transactional() {
    affectedAssessments.asScala.foreach(_.onBind(moduleAndDepartmentService))
  }

  def applyInternal(): Result = transactional() {
    require(submission.canRecordOutcomes, "Cannot record outcomes for this submission")

    submission.outcomeGrading = outcomeGrading
    submission.outcomeReasons = outcomeReasons
    submission.boardRecommendations = boardRecommendations.asScala
    if (boardRecommendations.asScala.contains(MitCircsExamBoardRecommendation.Other) && boardRecommendationOther.hasText) {
      submission.boardRecommendationOther = boardRecommendationOther
    } else {
      submission.boardRecommendationOther = null
    }
    submission.boardRecommendationComments = boardRecommendationComments

    if(outcomeGrading == Rejected) {
      submission.rejectionReasons = rejectionReasons.asScala
      if (rejectionReasons.asScala.contains(MitigatingCircumstancesRejectionReason.Other) && rejectionReasonsOther.hasText) {
        submission.rejectionReasonsOther = rejectionReasonsOther
      } else {
        submission.rejectionReasonsOther = null
      }
    } else {
      submission.rejectionReasons = Seq()
      submission.rejectionReasonsOther = null
    }

    // TODO dumping the existing ones is a bit wasteful and might cause issues later if we add other props
    submission.affectedAssessments.clear()
    affectedAssessments.asScala.foreach { item =>
      val affected = new MitigatingCircumstancesAffectedAssessment(submission, item)
      submission.affectedAssessments.add(affected)
    }

    if (confirm) {
      submission.outcomesRecorded()
    }

    submission.lastModifiedBy = user
    submission.lastModified = DateTime.now
    submission.outcomesLastRecordedBy = user
    submission.outcomesLastRecordedOn = DateTime.now
    mitCircsSubmissionService.saveOrUpdate(submission)
    submission
  }
}

trait MitCircsRecordOutcomesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: MitCircsRecordOutcomesState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(RequiredPermission, submission)
  }
}

trait MitCircsRecordOutcomesValidation extends SelfValidating {
  self: MitCircsRecordOutcomesRequest =>

  def validate(errors: Errors) {
    if(outcomeGrading == null) errors.rejectValue("outcomeGrading", "mitigatingCircumstances.outcomes.outcomeGrading.required")
    if(!outcomeReasons.hasText) errors.rejectValue("outcomeReasons", "mitigatingCircumstances.outcomes.outcomeReasons.required")

    if(boardRecommendations.contains(MitCircsExamBoardRecommendation.Other) && !boardRecommendationOther.hasText)
      errors.rejectValue("boardRecommendationOther", "mitigatingCircumstances.outcomes.boardRecommendationsOther.required")

    if(outcomeGrading == Rejected && rejectionReasons.isEmpty) errors.rejectValue("rejectionReasons", "mitigatingCircumstances.outcomes.rejectionReasons.required")
    else if(outcomeGrading == Rejected && rejectionReasons.contains(MitigatingCircumstancesRejectionReason.Other) && !rejectionReasonsOther.hasText)
      errors.rejectValue("rejectionReasonsOther", "mitigatingCircumstances.outcomes.rejectionReasonsOther.required")
  }
}

trait MitCircsRecordOutcomesDescription extends Describable[Result] {
  self: MitCircsRecordOutcomesState =>

  def describe(d: Description) {
    d.mitigatingCircumstancesSubmission(submission)
  }
}

trait MitCircsRecordOutcomesState {
  val submission: MitigatingCircumstancesSubmission
  val user: User
}

trait MitCircsRecordOutcomesRequest {
  self : MitCircsRecordOutcomesState =>

  var affectedAssessments: JList[AffectedAssessmentItem] = submission.affectedAssessments.asScala.map(new AffectedAssessmentItem(_)).asJava
  var outcomeGrading: MitigatingCircumstancesGrading = submission.outcomeGrading
  var outcomeReasons: String = submission.outcomeReasons
  @BeanProperty var boardRecommendations: JList[MitCircsExamBoardRecommendation] = submission.boardRecommendations.asJava
  var boardRecommendationOther: String = submission.boardRecommendationOther
  var boardRecommendationComments: String = submission.boardRecommendationComments
  var rejectionReasons: JList[MitigatingCircumstancesRejectionReason] = submission.rejectionReasons.asJava
  var rejectionReasonsOther: String = submission.rejectionReasonsOther
  var confirm: Boolean = false
}