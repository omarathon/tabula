package uk.ac.warwick.tabula.data.model

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.MarkingMethod.FirstMarkerOnly
import uk.ac.warwick.tabula.services.SubmissionService

@Entity
@Proxy
@DiscriminatorValue(value = "FirstMarkerOnly")
class FirstMarkerOnlyWorkflow extends MarkingWorkflow with NoSecondMarker with AssessmentMarkerMap {

  def this(dept: Department) = {
    this()
    this.department = dept
  }

  @transient var submissionService: SubmissionService = Wire[SubmissionService]

  def markingMethod = FirstMarkerOnly

  override def getStudentsSecondMarker(assessment: Assessment, universityId: String) = None

  override def validForExams = true
}