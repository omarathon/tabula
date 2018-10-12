package uk.ac.warwick.tabula.data.model.notifications.cm2

import org.junit.{After, Before}
import org.mockito.Matchers
import uk.ac.warwick.tabula.data.model.markingworkflow.{DoubleBlindWorkflow, DoubleWorkflow, MarkingWorkflowStage}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage._
import uk.ac.warwick.tabula.data.model.{Assignment, Department, StudentMember}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService.Allocations
import uk.ac.warwick.userlookup.User

class ReleaseToMarkerNotificationHelperTest extends TestBase with Mockito {
	var cm2MarkingWorkflowService: CM2MarkingWorkflowService = _
	var dept: Department = _

	var assignment: Assignment = _

	@Before
	def prepare(): Unit = {
		cm2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]
		dept = Fixtures.department("in")
		assignment = Fixtures.assignment("demo")
	}

	val stu1: User = Fixtures.user("1111111", "1111111")
	val stu2: User = Fixtures.user("2222222", "2222222")
	val stu3: User = Fixtures.user("3333333", "3333333")
	val stu4: User = Fixtures.user("4444444", "4444444")


	val marker1: User = Fixtures.user("9999991", "9999991")
	val marker2: User = Fixtures.user("9999992", "9999992")

	@Test
	def getCorrectCountForMarkers(): Unit = {

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


		val workflow = DoubleWorkflow("test", dept, allocations(DblFirstMarker).keys.toSeq, allocations(DblSecondMarker).keys.toSeq)
		assignment.cm2MarkingWorkflow = workflow


		cm2MarkingWorkflowService.getAllStudentsForMarker(any[Assignment], Matchers.refEq(marker1)) answers (_ => {
			Seq(stu1, stu2, stu3)
		})

		cm2MarkingWorkflowService.getAllStudentsForMarker(any[Assignment], Matchers.refEq(marker2)) answers (_ => {
			Seq(stu3)
		})

		var marker1Helper: ReleaseToMarkerNotificationHelper = new ReleaseToMarkerNotificationHelper(assignment, marker1, cm2MarkingWorkflowService)

		marker1Helper.studentsAllocatedToThisMarker should be(Set(stu1, stu2, stu3))

		val marker1Result = marker1Helper.studentsAtStagesCount

		marker1Result should be(
			Seq(
				StudentAtStagesCount(DblFirstMarker.description, 2),
				StudentAtStagesCount(DblSecondMarker.description, 1),
				StudentAtStagesCount(DblFinalMarker.description, 2)
			)
		)

		val marker2Helper = new ReleaseToMarkerNotificationHelper(assignment, marker2, cm2MarkingWorkflowService)

		marker2Helper.studentsAllocatedToThisMarker should be(Set(stu3))

		val marker2Result = marker2Helper.studentsAtStagesCount

		marker2Result should be(
			Seq(
				StudentAtStagesCount(DblFirstMarker.description, 1),
				StudentAtStagesCount(DblSecondMarker.description, 0),
				StudentAtStagesCount(DblFinalMarker.description, 1)
			)
		)
	}


}
