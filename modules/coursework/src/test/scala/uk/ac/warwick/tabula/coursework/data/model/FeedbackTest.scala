package uk.ac.warwick.tabula.coursework.data.model

import uk.ac.warwick.tabula.coursework.TestBase


import org.junit.Test
import scala.util.Random

class FeedbackTest extends TestBase {
	
	@Test def fields {
	  
	  val random = new Random
	  val actualGrades = List("1","21","22","3","A","A+","AB","B","C","CO","CP","D","E, F","L","M","N","NC","P","PL","QF","R","RF","RW","S","T","W","WW")
	  
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
	
		
	/** Zero-pad integer to a 7 digit string */
	def idFormat(i:Int) = "%07d" format i
}