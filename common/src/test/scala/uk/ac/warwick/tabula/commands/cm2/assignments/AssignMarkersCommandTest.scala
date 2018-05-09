package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.DateTime
import org.springframework.validation.{BeanPropertyBindingResult, MapBindingResult}
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.commands.{DescriptionImpl, ValidatorHelpers}
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback, MarkerFeedback}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{ModerationMarker, ModerationModerator}
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService._
import uk.ac.warwick.tabula.services.UserLookupComponent
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
	}

	@Test
	def testDescription(): Unit = new Fixture {
		val d = new  DescriptionImpl
		val description = new AssignMarkersDescription with AssignMarkersState with UserLookupComponent {
			val userLookup = userlookupService
			val assignment: Assignment = a1
			override val allocationMap: Map[MarkingWorkflowStage, Allocations] = alloc
		}
		description.describe(d)
		d.allProperties("assignment") should be (a1.id)
		d.allProperties("allocations") should be ("Marker:\ncuslaj -> u1431777,u1431778,u1431779\ncuslak -> u1431780,u1431781,u1431782\nModerator:\ncuslal -> u1431777,u1431778,u1431779,u1431780,u1431781,u1431782")
	}

	@Test
	def testValidateChangedAllocationsValid(): Unit = new Fixture {
		private val validation = new AssignMarkersValidation with ValidateConcurrentStages with AssignMarkersState {
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
		validation.validateChangedAllocations(bindingResult)

		bindingResult.hasErrors shouldBe false
	}

	@Test
	def testValidateChangedAllocationsInvalid(): Unit = new Fixture {
		private val validation = new AssignMarkersValidation with ValidateConcurrentStages with AssignMarkersState {
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
		validation.validateChangedAllocations(bindingResult)

		private val maybeError = bindingResult.getAllErrors.asScala.find(_.getCode == "markingWorkflow.markers.finalised")
		maybeError should not be empty

		private val error = maybeError.get
		error.getArguments shouldBe Array("marker", "Marker One", "Student One")
	}

}
