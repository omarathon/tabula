package uk.ac.warwick.courses.data.model

import uk.ac.warwick.courses.PersistenceTestBase
import org.junit.runner.RunWith
import org.junit.Test
import uk.ac.warwick.courses.Fixtures
import uk.ac.warwick.courses.services.AssignmentServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AssignmentService

class SubmissionPersistenceTest extends PersistenceTestBase {
	
	/**
	 * Test the relationship between Submission and OriginalityReport.
	 */
    @Test def submissionsAndReports {
    	transactional { t => 
    		
        	val submission = Fixtures.submission()
        	val assignment = newDeepAssignment()
        	submission.assignment = assignment
        	session.save(assignment.module.department)
        	session.save(assignment.module)
        	session.save(assignment)
        	session.save(submission)
        	
        	val report1 = newReport
        	
        	submission addOriginalityReport report1
        	session.update(submission)
        	session.save(submission.originalityReport)
        	
        	session.flush
        	
        	val retrievedSubmission = session.get(classOf[Submission], submission.id).asInstanceOf[Submission]
        	retrievedSubmission.originalityReport.overlap should be (Some(1))
        	retrievedSubmission.originalityReport.webOverlap should be (None)
        	
        	// check that previous reports are removed, otherwise the @OneToOne will explode
        	retrievedSubmission.originalityReport = null
        	session.delete(report1)
        	session.flush // hmm, need to flush to delete.
        	
        	val report2 = newReport
        	retrievedSubmission addOriginalityReport report2
        	session.update(retrievedSubmission)
        	session.save(retrievedSubmission.originalityReport)
            
            session.flush
            session.clear
            
            // expecting: no exception from having >1 matching OriginalityReport
//            val session2 = session.get(classOf[Submission], submission.id).asInstanceOf[Submission]
//        	session2.originalityReport should be ('completed)
    	}
    }
    
    def newReport = {
    	val r = new OriginalityReport
        r.overlap = Some(1)
        r
    }
	
}