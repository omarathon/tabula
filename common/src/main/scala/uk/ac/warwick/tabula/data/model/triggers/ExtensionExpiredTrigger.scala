package uk.ac.warwick.tabula.data.model.triggers

import javax.persistence.{DiscriminatorValue, Entity, Inheritance, InheritanceType}
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{Assignment, ToEntityReference}

object ExtensionExpiredTrigger {
  def apply(thisScheduledDate: DateTime, thisTargetEntity: ToEntityReference): ExtensionExpiredTrigger = {
    val result = new ExtensionExpiredTrigger
    result.scheduledDate = thisScheduledDate
    result.updateTarget(thisTargetEntity)
    result
  }
}

@Entity
@Proxy
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue(value = "ExtensionExpired")
class ExtensionExpiredTrigger extends Trigger[Extension, Unit] with HandlesAssignmentTrigger {

  def extension: Extension = target.entity

  override def assignment: Assignment = extension.assignment

  override def apply(): Unit = transactional() {
    // Only if this is the most recent approved extension for the student
    if (assignment.approvedExtensions.get(extension.usercode).contains(extension)) {
      handleAssignment(Seq(extension.usercode))
    }
  }

}
