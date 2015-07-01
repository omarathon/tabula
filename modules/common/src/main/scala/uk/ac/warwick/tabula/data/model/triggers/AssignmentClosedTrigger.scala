package uk.ac.warwick.tabula.data.model.triggers

import javax.persistence.{DiscriminatorValue, Entity, Inheritance, InheritanceType}

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.coursework.assignments.ReleaseForMarkingCommand
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, ToEntityReference}
import uk.ac.warwick.userlookup.AnonymousUser

import scala.collection.JavaConverters._

object AssignmentClosedTrigger {
	def apply(thisScheduledDate: DateTime, thisTargetEntity: ToEntityReference) = {
		val result = new AssignmentClosedTrigger
		result.scheduledDate = thisScheduledDate
		result.updateTarget(thisTargetEntity)
		result
	}
}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue(value="AssignmentClosed")
class AssignmentClosedTrigger	extends Trigger[Assignment, Seq[Feedback]] {

	override def apply(): Seq[Feedback] = transactional() {
		val assignment = target.entity

		if (assignment.isClosed) {
			if (assignment.automaticallyReleaseToMarkers && assignment.hasWorkflow) {
				val studentsWithSubmission = assignment.submissions.asScala.map(_.universityId)
				val releaseToMarkersCommand = ReleaseForMarkingCommand(assignment.module, assignment, new AnonymousUser)
				releaseToMarkersCommand.students = JArrayList(studentsWithSubmission)
				releaseToMarkersCommand.confirm = true
				releaseToMarkersCommand.onBind(null)
				releaseToMarkersCommand.apply()
			} else {
				Seq()
			}
		} else {
			Seq()
		}

	}
}
