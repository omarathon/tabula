package uk.ac.warwick.tabula.data.model.triggers

import javax.persistence.{DiscriminatorValue, Entity, Inheritance, InheritanceType}

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Submission, ToEntityReference}

object SubmissionAfterCloseDateTrigger {
	def apply(thisScheduledDate: DateTime, thisTargetEntity: ToEntityReference) = {
		val result = new SubmissionAfterCloseDateTrigger
		result.scheduledDate = thisScheduledDate
		result.updateTarget(thisTargetEntity)
		result
	}
}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue(value="SubmissionAfterCloseDate")
class SubmissionAfterCloseDateTrigger	extends Trigger[Submission, Unit] with HandlesAssignmentTrigger {

	def submission = target.entity
	override def assignment = submission.assignment

	override def apply() = transactional() {
		if (assignment.isClosed && (submission.isLate || submission.isAuthorisedLate)) {
			handleAssignment(Seq(submission.universityId))
		}

	}
}
