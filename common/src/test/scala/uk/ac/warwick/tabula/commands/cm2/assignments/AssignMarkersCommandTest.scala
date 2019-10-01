package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.DateTime
import org.springframework.validation.BeanPropertyBindingResult
import uk.ac.warwick.tabula.commands.{DescriptionImpl, ValidatorHelpers}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{ModerationMarker, ModerationModerator, SelectedModerationMarker, SelectedModerationModerator}
import uk.ac.warwick.tabula.data.model.markingworkflow.{MarkingWorkflowStage, ModerationSampler, SelectedModeratedWorkflow}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

import scala.collection.JavaConverters._

class AssignMarkersCommandTest extends TestBase with Mockito with ValidatorHelpers {

  trait Fixture {
    val marker1 = Fixtures.user("1170836", "cuslaj")
    val marker2 = Fixtures.user("1170837", "cuslak")
    val moderator = Fixtures.user("1170838", "cuslal")
    val student1 = Fixtures.user("1431777", "u1431777")
    val student2 = Fixtures.user("1431778", "u1431778")
    val student3 = Fixtures.user("1431779", "u1431779")
    val student4 = Fixtures.user("1431780", "u1431780")
    val student5 = Fixtures.user("1431781", "u1431781")
    val student6 = Fixtures.user("1431782", "u1431782")
    val userlookupService = Fixtures.userLookupService(marker1, marker2, moderator, student1, student2, student3, student4, student5, student6)
    val a1 = Fixtures.assignment("a1")
    a1.id = "a1"

    val alloc: Map[MarkingWorkflowStage, Allocations] = Map(
      ModerationMarker -> Map(marker1 -> Set(student1, student2, student3), marker2 -> Set(student4, student5, student6)),
      ModerationModerator -> Map(moderator -> Set(student1, student2, student3, student4, student5, student6))
    )

    marker1.setFullName("Marker One")
    student1.setFullName("Student One")
    student2.setFullName("Student Two")
  }

  @Test
  def testDescription(): Unit = new Fixture {
    val d = new DescriptionImpl
    val description = new AssignMarkersDescription with AssignMarkersState with UserLookupComponent {
      val userLookup = userlookupService
      val assignment: Assignment = a1
      override val allocationMap: Map[MarkingWorkflowStage, Allocations] = alloc
    }
    description.describe(d)
    d.allProperties("assignment") should be(a1.id)
    d.allProperties("allocations") should be("Marker:\ncuslaj -> u1431777,u1431778,u1431779\ncuslak -> u1431780,u1431781,u1431782\nModerator:\ncuslal -> u1431777,u1431778,u1431779,u1431780,u1431781,u1431782")
  }

  @Test
  def testValidateChangedMarkerAllocationsValid(): Unit = new Fixture {
    private val validation = new AssignMarkersValidation with ValidateConcurrentStages with AssignMarkersState with UserLookupComponent with CM2MarkingWorkflowServiceComponent with AssessmentMembershipServiceComponent {

      val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
      val userLookup = userlookupService
      val cm2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]
      // Set up an assignment where marker1 has written some non-final feedback for student1
      val assignment: Assignment = {
        val feedback = new AssignmentFeedback
        feedback.usercode = student1.getUserId
        val mf = new MarkerFeedback
        mf.userLookup = userlookupService
        mf.stage = ModerationMarker
        mf.marker = marker1
        mf.mark = Some(80)
        mf.comments = "Good job"
        mf.uploadedDate = DateTime.now
        mf.updatedOn = DateTime.now
        feedback.outstandingStages.add(ModerationMarker)
        mf.feedback = feedback
        feedback.markerFeedback.add(mf)
        a1.feedbacks.add(feedback)
        a1.feedbackService = smartMock[FeedbackService]
        a1.feedbackService.loadFeedbackForAssignment(a1) returns a1.feedbacks.asScala

        mf should not be 'finalised

        a1
      }

      // Now allocate student1 to marker2 - this is fine
      val allocationMap: Map[MarkingWorkflowStage, Allocations] =
        Map(
          ModerationMarker -> Map(
            marker1 -> Set.empty,
            marker2 -> Set(student1, student2)
          ),
          ModerationModerator -> Map(
            moderator -> Set(student1, student2)
          )
        )
    }

    private val bindingResult = new BeanPropertyBindingResult(validation, "assignMarkers")
    validation.validateChangedMarkerAllocations(validation.allocationMap, bindingResult)

    bindingResult.hasErrors shouldBe false
  }

  @Test
  def testValidateChangedMarkerAllocationsInvalid(): Unit = new Fixture {
    private val validation = new AssignMarkersValidation with ValidateConcurrentStages with AssignMarkersState with UserLookupComponent with CM2MarkingWorkflowServiceComponent with AssessmentMembershipServiceComponent {
      val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
      val userLookup = userlookupService
      val cm2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]
      // Set up an assignment where marker1 has finalised some feedback for student1
      val assignment: Assignment = {
        val feedback = new AssignmentFeedback
        feedback.usercode = student1.getUserId
        val mf = new MarkerFeedback
        mf.userLookup = userlookupService
        mf.stage = ModerationMarker
        mf.marker = marker1
        mf.mark = Some(80)
        mf.comments = "Good job"
        mf.uploadedDate = DateTime.now
        mf.updatedOn = DateTime.now
        feedback.outstandingStages.add(ModerationModerator)
        mf.feedback = feedback
        feedback.markerFeedback.add(mf)
        a1.feedbacks.add(feedback)
        a1.feedbackService = smartMock[FeedbackService]
        a1.feedbackService.loadFeedbackForAssignment(a1) returns a1.feedbacks.asScala

        mf shouldBe 'finalised

        a1
      }

      // Now try and reallocate student1 to marker2 - this shouldn't be valid
      val allocationMap: Map[MarkingWorkflowStage, Allocations] =
        Map(
          ModerationMarker -> Map(
            marker1 -> Set.empty,
            marker2 -> Set(student1, student2)
          ),
          ModerationModerator -> Map(
            moderator -> Set(student1, student2)
          )
        )
    }

    private val bindingResult = new BeanPropertyBindingResult(validation, "assignMarkers")
    validation.validateChangedMarkerAllocations(validation.allocationMap, bindingResult)

    private val maybeError = bindingResult.getAllErrors.asScala.find(_.getCode == "markingWorkflow.markers.finalised")
    maybeError should not be empty

    private val error = maybeError.get
    error.getArguments shouldBe Array("marker", "Marker One", "Student One")
  }


  @Test
  def testValidateUnallocatedMarkerAllocationsValid(): Unit = new Fixture {
    private val validation = new AssignMarkersValidation with ValidateConcurrentStages with AssignMarkersState with UserLookupComponent with CM2MarkingWorkflowServiceComponent with AssessmentMembershipServiceComponent {
      val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
      val userLookup = userlookupService
      val cm2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]

      // Set up an assignment where marker1 has written some non-final feedback for student1
      val assignment: Assignment = {
        a1.assessmentMembershipService = assessmentMembershipService
        val feedback = new AssignmentFeedback
        feedback.usercode = student1.getUserId
        val mf = new MarkerFeedback
        mf.userLookup = userlookupService
        mf.stage = ModerationMarker
        mf.marker = marker1
        mf.mark = Some(80)
        mf.comments = "Good job"
        mf.uploadedDate = DateTime.now
        mf.updatedOn = DateTime.now
        feedback.outstandingStages.add(ModerationMarker)
        mf.feedback = feedback
        feedback.markerFeedback.add(mf)
        a1.feedbacks.add(feedback)
        a1.feedbackService = smartMock[FeedbackService]
        a1.feedbackService.loadFeedbackForAssignment(a1) returns a1.feedbacks.asScala

        mf should not be 'finalised
        val department = new Department
        val module = new Module("IN101", department)
        a1.module = module
        a1
      }

      assessmentMembershipService.determineMembershipUsersWithOrder(assignment) returns Seq((student1, None), (student2, None))
      assessmentMembershipService.determineMembershipUsers(assignment) returns Seq(student1, student2)

      cm2MarkingWorkflowService.getMarkerAllocations(assignment, SelectedModerationMarker) returns Map(marker1 -> Set(student1), marker2 -> Set(student2))
      cm2MarkingWorkflowService.getMarkerAllocations(assignment, SelectedModerationModerator) returns Map(moderator -> Set(student1, student1))


      val workflow = SelectedModeratedWorkflow("test", assignment.module.adminDepartment, ModerationSampler.Marker, Seq(marker1, marker2), Seq(moderator))
      val mockUserLookup: UserLookupService = Fixtures.userLookupService(Seq(moderator, marker1, marker2) ++ Seq(student1, student2): _*)
      workflow.stageMarkers.asScala.foreach(_.markers.asInstanceOf[UserGroup].userLookup = mockUserLookup)
      assignment.cm2MarkingWorkflow = workflow


      // Now try and unallocate student1 from marker1 - this should be valid
      val allocationMap: Map[MarkingWorkflowStage, Allocations] =
        Map(
          ModerationMarker -> Map(
            marker1 -> Set.empty,
            marker2 -> Set(student2)
          ),
          ModerationModerator -> Map(
            moderator -> Set(student1, student2)
          )
        )
    }

    private val bindingResult = new BeanPropertyBindingResult(validation, "assignMarkers")
    validation.validateUnallocatedMarkerAllocations(validation.allocationMap, bindingResult)
    bindingResult.hasErrors shouldBe false
  }


  @Test
  def testValidateUnallocatedMarkerAllocationsInvalid(): Unit = new Fixture {
    private val validation = new AssignMarkersValidation with ValidateConcurrentStages with AssignMarkersState with UserLookupComponent with CM2MarkingWorkflowServiceComponent with AssessmentMembershipServiceComponent {
      val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
      val userLookup = userlookupService
      val cm2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]

      // Set up an assignment where marker1 has finalised some feedback for student2
      val assignment: Assignment = {
        a1.assessmentMembershipService = assessmentMembershipService

        val feedback = new AssignmentFeedback
        feedback.usercode = student2.getUserId
        val mf = new MarkerFeedback
        mf.userLookup = userlookupService
        mf.stage = ModerationMarker
        mf.marker = marker1
        mf.mark = Some(80)
        mf.comments = "Good job"
        mf.uploadedDate = DateTime.now
        mf.updatedOn = DateTime.now
        feedback.outstandingStages.add(ModerationModerator)
        mf.feedback = feedback
        feedback.markerFeedback.add(mf)
        a1.feedbacks.add(feedback)
        a1.feedbackService = smartMock[FeedbackService]
        a1.feedbackService.loadFeedbackForAssignment(a1) returns a1.feedbacks.asScala

        mf shouldBe 'finalised

        val department = new Department
        val module = new Module("IN101", department)
        a1.module = module
        a1
      }
      assessmentMembershipService.determineMembershipUsersWithOrder(assignment) returns Seq((student1, None), (student2, None))
      assessmentMembershipService.determineMembershipUsers(assignment) returns Seq(student1, student2)

      cm2MarkingWorkflowService.getMarkerAllocations(assignment, SelectedModerationMarker) returns Map(marker1 -> Set(student2), marker2 -> Set(student1))
      cm2MarkingWorkflowService.getMarkerAllocations(assignment, SelectedModerationModerator) returns Map(moderator -> Set(student1, student1))


      val workflow = SelectedModeratedWorkflow("test", assignment.module.adminDepartment, ModerationSampler.Marker, Seq(marker1, marker2), Seq(moderator))
      val mockUserLookup: UserLookupService = Fixtures.userLookupService(Seq(moderator, marker1, marker2) ++ Seq(student1, student2): _*)
      workflow.stageMarkers.asScala.foreach(_.markers.asInstanceOf[UserGroup].userLookup = mockUserLookup)
      assignment.cm2MarkingWorkflow = workflow

      // Now try and unallocate student2 from marker1 - this shouldn't be valid
      val allocationMap: Map[MarkingWorkflowStage, Allocations] =
        Map(
          ModerationMarker -> Map(
            marker1 -> Set.empty,
            marker2 -> Set(student1)
          ),
          ModerationModerator -> Map(
            moderator -> Set(student1, student2)
          )
        )
    }

    private val bindingResult = new BeanPropertyBindingResult(validation, "assignMarkers")
    validation.validateUnallocatedMarkerAllocations(validation.allocationMap, bindingResult)

    private val maybeError = bindingResult.getAllErrors.asScala.find(_.getCode == "markingWorkflow.markers.finalised")
    maybeError should not be empty

    private val error = maybeError.get
    error.getArguments shouldBe Array("marker", "Marker One", "Student Two")
  }

}
