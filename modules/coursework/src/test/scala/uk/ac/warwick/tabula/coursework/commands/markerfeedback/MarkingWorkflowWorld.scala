package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.data.model._
import java.util.HashMap
import uk.ac.warwick.tabula.AppContextTestBase
import org.junit.Before

// reusable environment for marking workflow tests
trait MarkingWorkflowWorld extends AppContextTestBase{
	
	private var _assignment: Assignment = _
	def assignment = _assignment

	@Before def setupTheWorld {
		val assignment = transactional { tx => 
			val assignment = newDeepAssignment(moduleCode = "IN101")
			session.save(assignment.module.department)
			session.save(assignment.module)
			session.save(assignment)
			assignment
		}

		transactional { tx => 
			generateSubmission(assignment, "9876004", "cusxad")
			generateSubmission(assignment, "0270954", "cuscao")
			generateSubmission(assignment, "9170726", "curef")
			generateSubmission(assignment, "0672088", "cusebr")
			generateSubmission(assignment, "0672089", "cuscav")
			addFeedback(assignment)
		}
	
		val markingWorkflow = transactional { tx => 
			val markingWorkflow = new MarkingWorkflow()
			markingWorkflow.name = "My magical mark scheme"
			markingWorkflow.markingMethod = MarkingMethod.SeenSecondMarking
			markingWorkflow.department = assignment.module.department
			markingWorkflow.firstMarkers = makeUserGroup("cuslaj", "cuscav")
			markingWorkflow.secondMarkers = makeUserGroup("cuslat", "cuday")
			assignment.markingWorkflow = markingWorkflow
			
			session.save(markingWorkflow)
			
			markingWorkflow
		}
		
		transactional { tx => 
			assignment.markerMap = new HashMap[String, UserGroup]()
			assignment.markerMap.put("cuslaj", makeUserGroup("cusxad", "cuscao", "curef"))
			assignment.markerMap.put("cuscav", makeUserGroup("cusebr", "cuscav"))
			assignment.markerMap.put("cuslat", makeUserGroup("cusxad", "cuscao", "curef"))
			assignment.markerMap.put("cuday", makeUserGroup("cusebr", "cuscav"))
		}
		
		_assignment = assignment
	}

	def addFeedback(assignment:Assignment){
		val feedback = assignment.submissions.map{s=>
			val newFeedback = new Feedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = "cuslaj"
			newFeedback.universityId = s.universityId
			newFeedback.released = false
			
			session.save(newFeedback)
			
			val fmFeedback = newFeedback.retrieveFirstMarkerFeedback
			fmFeedback.state = MarkingState.ReleasedForMarking
			
			session.update(newFeedback)
			
			newFeedback
		}
		
		assignment.feedbacks.addAll(feedback)
	}


	def generateSubmission(assignment:Assignment, uniId: String, userCode: String) {
		val submission = new Submission()
		submission.assignment = assignment
		submission.universityId = uniId
		submission.userId = userCode
		assignment.submissions.add(submission)
	}

	def makeUserGroup(users: String*): UserGroup = {
		val ug = new UserGroup
		ug.includeUsers = users
		ug
	}
}
