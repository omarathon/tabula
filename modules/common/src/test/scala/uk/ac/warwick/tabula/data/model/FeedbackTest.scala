package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import scala.util.Random
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.junit.Test

class FeedbackTest extends TestBase {
	
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
	
		
	/** Zero-pad integer to a 7 digit string */
	def idFormat(i:Int) = "%07d" format i
}