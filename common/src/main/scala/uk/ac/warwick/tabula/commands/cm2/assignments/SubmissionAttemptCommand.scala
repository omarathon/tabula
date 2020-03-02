package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}

object SubmissionAttemptCommand {
  type Command = Appliable[Unit] with SubmissionAttemptDescription

  def apply(assignment: Assignment, user: CurrentUser): Command =
    new SubmissionAttemptCommandInternal(assignment, MemberOrUser(user.profile, user.apparentUser))
      with ComposableCommand[Unit]
      with SubmissionAttemptDescription
      with SubmitAssignmentAsSelfPermissions
      with SubmitAssignmentState
}

class SubmissionAttemptCommandInternal(val assignment: Assignment, val user: MemberOrUser)
  extends CommandInternal[Unit] {

  override def applyInternal(): Unit = {}
}

trait SubmissionAttemptDescription extends Describable[Unit] {
  self: SubmitAssignmentState =>

  override lazy val eventName = "SubmissionAttempt"

  override def describe(d: Description): Unit = {
    d.assignment(assignment)
  }
}
