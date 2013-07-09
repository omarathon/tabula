package uk.ac.warwick.tabula.data.model

import scala.util.Random
import uk.ac.warwick.tabula.PersistenceTestBase

// scalastyle:off magic.number

class FeedbackTest extends PersistenceTestBase {
	
	@Test def fields {
	  
	  val random = new Random
	  val actualGrades: List[Option[String]] = List(Some("1"),Some("21"),Some("22"),Some("3"),Some("A"),Some("A+"),Some("AB"),Some("B"),Some("C"),Some("CO"),Some("CP"),Some("D"),Some("E, F"),Some("L"),Some("M"),Some("N"),Some("NC"),Some("P"),Some("PL"),Some("QF"),Some("R"),Some("RF"),Some("RW"),Some("S"),Some("T"),Some("W"),Some("WW"))
	  
	  val assignment = new Assignment
	  assignment.collectMarks = true
	  
	  for (i <- 1 to 10){ // 0000001 .. 0000010 
	    var feedback = new Feedback(universityId = idFormat(i))
	    // assign marks to even numbered students
	    if(i % 2 == 0){
	      val newMark = random.nextInt(101)
	      feedback.actualMark = Some(newMark)
	      val newGrade = random.shuffle(actualGrades).head
	      feedback.actualGrade = newGrade
	      feedback.actualMark.get should be (newMark)
	      feedback.actualGrade should be (newGrade)
	    }
	    assignment.feedbacks add feedback
	  }
	  assignment.feedbacks.size should be (10)
	}
	
	@Test def deleteFileAttachmentOnDelete = transactional {ts=>
		// TAB-667
		val orphanAttachment = flushing(session) {
			val attachment = new FileAttachment
			
			session.save(attachment)
			attachment
		}
		
		val (feedback, feedbackAttachment) = flushing(session) {
			val feedback = new Feedback(universityId = idFormat(1))
			
			val assignment = new Assignment
			session.save(assignment)
			
			feedback.assignment = assignment
			
			val attachment = new FileAttachment
			feedback.addAttachment(attachment)
			
			session.save(feedback)
			(feedback, attachment)
		}
		
		// Ensure everything's been persisted
		orphanAttachment.id should not be (null)
		feedback.id should not be (null)
		feedbackAttachment.id should not be (null)
		
		// Can fetch everything from db
		flushing(session) {
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[Feedback], feedback.id) should be (feedback)
			session.get(classOf[FileAttachment], feedbackAttachment.id) should be (feedbackAttachment)
		}

		flushing(session) { session.delete(feedback) }
		
		// Ensure we can't fetch the feedback or attachment, but all the other objects are returned
		flushing(session) {
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[Feedback], feedback.id) should be (null)
			session.get(classOf[FileAttachment], feedbackAttachment.id) should be (null)
		}
	}
	
		
	/** Zero-pad integer to a 7 digit string */
	def idFormat(i:Int) = "%07d" format i
}