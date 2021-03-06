package uk.ac.warwick.tabula.data.model.triggers

import javax.persistence.{DiscriminatorValue, Entity, Inheritance, InheritanceType}
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, Submission, ToEntityReference}

object SubmissionBeforeDeadlineTrigger {
  def apply(thisScheduledDate: DateTime, thisTargetEntity: ToEntityReference): SubmissionBeforeDeadlineTrigger = {
    val result = new SubmissionBeforeDeadlineTrigger
    result.scheduledDate = thisScheduledDate
    result.updateTarget(thisTargetEntity)
    result
  }
}

@Entity
@Proxy
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue(value = "SubmissionBeforeCloseDate")
class SubmissionBeforeDeadlineTrigger extends Trigger[Submission, Unit] with HandlesAssignmentTrigger {

  def submission: Submission = target.entity

  override def assignment: Assignment = submission.assignment

  override def apply(): Unit = transactional() {
    handleTurnitinSubmission()
  }
}
