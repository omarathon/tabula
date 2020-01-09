package uk.ac.warwick.tabula.data.model.notifications.cm2

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService.Student
import uk.ac.warwick.userlookup.User

case class StudentAtStagesCount(stageName: String, count: Int)

class ReleaseToMarkerNotificationHelper(assignment: Assignment, recipient: User, cm2MarkingWorkflowService: CM2MarkingWorkflowService) {

  // all students assigned to recipient (marker) for this assignment
  lazy val studentsAllocatedToThisMarker: Set[Student] = cm2MarkingWorkflowService.getAllStudentsForMarker(assignment, recipient).toSet


  // students at each stage that's assigned to the recipient for this assignment
  lazy val studentsAtStages: Seq[(MarkingWorkflowStage, Set[Student])] = {
    assignment.cm2MarkingWorkflow.workflowType.allStages.map { stage =>
      stage -> {
        val allocationsForAllMarkers = cm2MarkingWorkflowService.getMarkerAllocations(assignment, stage)
        val allocationsForThisMarker = allocationsForAllMarkers.get(recipient)
        allocationsForThisMarker.map(_.intersect(studentsAllocatedToThisMarker)).getOrElse(Set.empty)
      }
    }
  }

  lazy val studentsAtStagesCount: Seq[StudentAtStagesCount] = {
    studentsAtStages.map {
      case (stage, students) => StudentAtStagesCount(stage.description, students.size)
    }
  }

  lazy val submissions: Seq[Submission] = assignment.cm2MarkerSubmissions(recipient).distinct

  lazy val submissionsCount: Int = submissions.count(_.submitted)

  lazy val approvedExtensions: Map[String, Extension] =
    assignment.approvedExtensions
      .filter { case (_, e) => studentsAllocatedToThisMarker.exists(e.isForUser) }

  lazy val extensionsCount: Int = approvedExtensions.size

  lazy val submissionsWithExtensionCount: Int =
    submissions
      .filter { s => approvedExtensions.contains(s.usercode) }
      .count(_.submitted)
}
