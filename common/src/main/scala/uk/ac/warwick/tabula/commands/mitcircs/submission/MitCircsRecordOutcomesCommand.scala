package uk.ac.warwick.tabula.commands.mitcircs.submission

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import MitCircsRecordOutcomesCommand._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.mitcircs.{MitCircsExamBoardRecommendation, MitigatingCircumstancesGrading, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
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
    with AutowiringMitCircsSubmissionServiceComponent
}

class MitCircsRecordOutcomesCommandInternal(val submission: MitigatingCircumstancesSubmission, val user: User) extends CommandInternal[Result]
  with MitCircsRecordOutcomesState with MitCircsRecordOutcomesValidation {

  self: MitCircsRecordOutcomesRequest with MitCircsSubmissionServiceComponent =>

  outcomeGrading = submission.outcomeGrading
  outcomeReasons = submission.outcomeReasons
  boardRecommendations = submission.boardRecommendations.asJava
  boardRecommendationOther = submission.boardRecommendationOther
  boardRecommendationComments = submission.boardRecommendationComments

  def applyInternal(): Result = transactional() {
    submission.outcomeGrading = outcomeGrading
    submission.outcomeReasons = outcomeReasons
    submission.boardRecommendations = boardRecommendations.asScala
    if (boardRecommendations.asScala.contains(MitCircsExamBoardRecommendation.Other) && boardRecommendationOther.hasText) {
      submission.boardRecommendationOther = boardRecommendationOther
    } else {
      submission.boardRecommendationOther = null
    }
    submission.boardRecommendationComments = boardRecommendationComments
    submission.outcomesRecorded()
    submission.lastModifiedBy = user
    submission.lastModified = DateTime.now
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
    if(boardRecommendations.isEmpty) errors.rejectValue("boardRecommendations", "mitigatingCircumstances.outcomes.boardRecommendations.required")
    else if(boardRecommendations.contains(MitCircsExamBoardRecommendation.Other) && !boardRecommendationOther.hasText)
      errors.rejectValue("boardRecommendationOther", "mitigatingCircumstances.outcomes.boardRecommendationsOther.required")
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
  var outcomeGrading: MitigatingCircumstancesGrading = _
  var outcomeReasons: String = _
  @BeanProperty var boardRecommendations: JList[MitCircsExamBoardRecommendation] = JArrayList()
  var boardRecommendationOther: String = _
  var boardRecommendationComments: String = _
}