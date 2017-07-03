package uk.ac.warwick.tabula.dev.web.commands

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.{MarkingWorkflowType, SingleMarkerWorkflow}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent, UserGroupDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.commands.cm2.markingworkflows.CreatesMarkingWorkflow

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._

class CreatePremarkedCM2AssignmentFixtureCommand extends CommandInternal[Assignment] with Logging {

	this: TransactionalComponent =>

	val assignmentSrv: AssessmentService = Wire[AssessmentService]
	val userLookup: UserLookupService = Wire[UserLookupService]
	val moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
	val markingWorkflowService: MarkingWorkflowService = Wire[MarkingWorkflowService]
	val cm2MarkingWorkflowService: CM2MarkingWorkflowService = Wire[CM2MarkingWorkflowService]
	val submissionService: SubmissionService = Wire[SubmissionService]
	val feedbackService: FeedbackService = Wire[FeedbackService]
	val userGroupDao: UserGroupDao = Wire[UserGroupDao]

	var moduleCode: String = _

	protected def applyInternal(): Assignment = {
		val module = moduleAndDepartmentService.getModuleByCode(moduleCode).getOrElse(
			throw new IllegalArgumentException(s"wrong module code $moduleCode")
		)

		val assignment = new Assignment
		assignment.module = module
		assignment.name = "Premarked assignment CM2"
		assignment.setDefaultBooleanProperties()
		assignment.addDefaultFields()
		assignment.openDate = DateTime.now.minusDays(30)
		assignment.closeDate = DateTime.now
		assignment.openEnded = false
		assignment.collectMarks = true
		assignment.collectSubmissions = true
		assignment.restrictSubmissions = true
		assignment.allowLateSubmissions = true
		assignment.allowResubmission = false
		assignment.displayPlagiarismNotice = true
		assignment.allowExtensions = true
		assignment.summative = false
		assignment.cm2Assignment = true

		transactional() {

		// persist the assignment to give it an ID
//		createAndSaveSingleUseWorkflow(assignment)
		assignmentSrv.save(assignment)


		val markersAUsers: Seq[User] = userLookup.getUsersByUserIds(CreatePremarkedCM2AssignmentFixtureCommand.firstMarkers).values.toSeq
		val markersBUsers: Seq[User] = JArrayList().asScala
	//	val data = MarkingWorkflowData(module.adminDepartment, "module.adminDepartment", markersAUsers, markersBUsers, MarkingWorkflowType.SingleMarking)
	//	val workflow = createWorkflow(data)
//		workflow.isReusable = true
	//	cm2MarkingWorkflowService.save(workflow)


			val singleMarkerWorkflow = SingleMarkerWorkflow("Single marker workflow", module.adminDepartment, markersAUsers)
		singleMarkerWorkflow.academicYear = AcademicYear(2016)
		singleMarkerWorkflow.isReusable = true
		cm2MarkingWorkflowService.save(singleMarkerWorkflow)
		assignment.cm2MarkingWorkflow = singleMarkerWorkflow

	//		val workflow = new FirstMarkerOnlyWorkflow(module.adminDepartment)
	//		workflow.name = "First marker only workflow"
	//		workflow.firstMarkers.knownType.includedUserIds = CreatePremarkedAssignmentFixtureCommand.firstMarkers
	//		markingWorkflowService.save(workflow)
	//		assignment.markingWorkflow = workflow

	/**		val group = UserGroup.ofUniversityIds
			assignment.members = group
			assignment.members.knownType.addUserId("3000001")
			assignment.members.knownType.addUserId("3000003")
			userGroupDao.saveOrUpdate(group)


			val markerBGroup = UserGroup.ofUniversityIds
			markerBGroup.addUserId("3000001")
			markerBGroup.addUserId("3000003")
			userGroupDao.saveOrUpdate(markerBGroup)
			val map1 = FirstMarkersMap(assignment, "tabula-functest-marker1", marker1Group)
			assignment.firstMarkers = Seq(map1).asJava    **/

			val submissions = CreatePremarkedCM2AssignmentFixtureCommand.students.map(student => {
				val s = new Submission
				s.assignment = assignment
				s.usercode = student.userId
				s._universityId = student.universityId
				s.submittedDate = DateTime.now.minusDays(30)
				s
			})
			submissions.foreach(submissionService.saveSubmission)
			assignment.submissions = submissions.asJava

			val feedbacks = CreatePremarkedCM2AssignmentFixtureCommand.students.map(student => {
				val f = new AssignmentFeedback
				f._universityId = student.universityId
				f.usercode = student.userId
				f.assignment = assignment
				f.uploaderId = "tabula-functest-admin1"
				f.actualMark = Some(41)
				val currentStage = singleMarkerWorkflow.initialStages.head
				f.outstandingStages = currentStage.nextStages.asJava
				feedbackService.saveOrUpdate(f)

				//val newMarkerFeedback = new MarkerFeedback(f)
				//newMarkerFeedback.stage = singleMarkerWorkflow.initialStages.head
				//newMarkerFeedback

				val mf = new MarkerFeedback(f)
				mf.marker = markersAUsers.head
				mf.stage = currentStage
				//f.firstMarkerFeedback = mf
				mf.mark = Some(41)

				//f.outstandingStages = f.assignment.cm2MarkingWorkflow.initialStages.asJava
				feedbackService.saveOrUpdate(f)
				//mf.state = MarkingCompleted
			//	val mf1 = new Outstandin(f)
				feedbackService.save(mf)
				f
			})

			assignment.feedbacks = feedbacks.asJava
			assignmentSrv.save(assignment)
		}

		assignment
	}
}


object CreatePremarkedCM2AssignmentFixtureCommand {

	case class Student(universityId: String, userId: String)

	val students = Seq(Student("3000001", "tabula-functest-student1"), Student("3000003", "tabula-functest-student3"))
	val firstMarkers = Seq("tabula-functest-marker1")

	def apply(): CreatePremarkedCM2AssignmentFixtureCommand with ComposableCommand[Assignment] with AutowiringTransactionalComponent with PubliclyVisiblePermissions with Unaudited ={
		new CreatePremarkedCM2AssignmentFixtureCommand
			with ComposableCommand[Assignment]
			with AutowiringTransactionalComponent
			with PubliclyVisiblePermissions
			with Unaudited
	}
}
