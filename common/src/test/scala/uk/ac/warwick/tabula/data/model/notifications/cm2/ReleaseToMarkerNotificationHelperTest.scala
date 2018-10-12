package uk.ac.warwick.tabula.data.model.notifications.cm2

import org.mockito.Matchers
import uk.ac.warwick.tabula.data.model.markingworkflow.{DoubleBlindWorkflow, DoubleWorkflow, MarkingWorkflowStage}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage._
import uk.ac.warwick.tabula.data.model.{Assignment, Department, StudentMember}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService.Allocations
import uk.ac.warwick.userlookup.User

class ReleaseToMarkerNotificationHelperTest extends TestBase with Mockito {


	val cm2MarkingWorkflowService: CM2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]

	val stu1: User = Fixtures.user("1111111", "1111111")
	val stu2: User = Fixtures.user("2222222", "2222222")
	val stu3: User = Fixtures.user("3333333", "3333333")
	val stu4: User = Fixtures.user("4444444", "4444444")


	val marker1: User = Fixtures.user("9999991", "9999991")
	val marker2: User = Fixtures.user("9999992", "9999992")

	val allocations: Map[MarkingWorkflowStage, Allocations] = Map(
		DblFirstMarker -> Map(
			marker1 -> Set(stu1, stu2),
			marker2 -> Set(stu3)
		),
		DblSecondMarker -> Map(
			marker1 -> Set(stu3)
		),

	)

	cm2MarkingWorkflowService.getMarkerAllocations(any[Assignment], Matchers.refEq(DblFirstMarker)) answers (_ => {
		allocations(DblFirstMarker)
	})

	cm2MarkingWorkflowService.getMarkerAllocations(any[Assignment], Matchers.refEq(DblSecondMarker)) answers (_ => {
		allocations(DblSecondMarker)
	})

	cm2MarkingWorkflowService.getMarkerAllocations(any[Assignment], Matchers.refEq(DblFinalMarker)) answers (_ => {
		allocations(DblFirstMarker)
	})

	val dept: Department = Fixtures.department("in")

	val assignment: Assignment = Fixtures.assignment("demo")
	val workflow = DoubleWorkflow("test", dept, allocations(DblFirstMarker).keys.toSeq, allocations(DblSecondMarker).keys.toSeq)
	assignment.cm2MarkingWorkflow = workflow

	@Test
	def getCorrectCountForMarker1(): Unit = {
		cm2MarkingWorkflowService.getAllStudentsForMarker(any[Assignment], any[User]) answers (_ => {
			Seq(stu1, stu2, stu3)
		})

		val helper: ReleaseToMarkerNotificationHelper = new ReleaseToMarkerNotificationHelper(assignment, marker1, cm2MarkingWorkflowService)

		helper.studentsAllocatedToThisMarker should be(Set(stu1, stu2, stu3))

		val result = helper.studentsAtStagesCount

		result should be(
			Seq(
				StudentAtStagesCount(DblFirstMarker.description, 2),
				StudentAtStagesCount(DblSecondMarker.description, 1),
				StudentAtStagesCount(DblFinalMarker.description, 2)
			)
		)
	}


}
