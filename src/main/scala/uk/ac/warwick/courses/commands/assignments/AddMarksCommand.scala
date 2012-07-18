package uk.ac.warwick.courses.commands.assignments

import scala.util.matching.Regex
import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.util.core.StringUtils.hasText
import uk.ac.warwick.courses.data.model.Feedback
import uk.ac.warwick.courses.services.UserLookupService
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.services.docconversion.MarksExtractor
import uk.ac.warwick.courses.commands.UploadedFile
import uk.ac.warwick.courses.services.docconversion.MarkItem
import uk.ac.warwick.courses.helpers.LazyLists
import uk.ac.warwick.courses.data.model.FileAttachment
import uk.ac.warwick.courses.helpers.NoUser
import org.springframework.validation.Errors
import uk.ac.warwick.courses.helpers.FoundUser
import uk.ac.warwick.courses.UniversityId


@Configurable
class AddMarksCommand( val assignment:Assignment, val submitter:CurrentUser ) extends Command[List[Feedback]] with Daoisms with Logging  {
 
	@Autowired var userLookup:UserLookupService =_
	@Autowired var marksExtractor:MarksExtractor =_  
  
	@BeanProperty var file:UploadedFile = new UploadedFile
    @BeanProperty var marks:JList[MarkItem] = LazyLists.simpleFactory()
    
	private def filenameOf(path:String) = new java.io.File(path).getName
  
	
	def postExtractValidation(errors:Errors) = {
		if (marks != null && !marks.isEmpty()) {
			for (i <- 0 until marks.length) {
				val mark = marks.get(i)
				errors.pushNestedPath("marks["+i+"]")
	 	 	 	mark.isValid = validateMarkItem(mark, errors)
	 	 	 	errors.popNestedPath()
			}
		}
	}
	
	def validateMarkItem(mark:MarkItem, errors:Errors) = {
		var noErrors = true
		// validate id
		if (hasText(mark.universityId)){
			if (!UniversityId.isValid(mark.universityId)) {
	 			errors.rejectValue("universityId", "uniNumber.invalid")
	 			noErrors = false
			} else {
				userLookup.getUserByWarwickUniId(mark.universityId) match {
					case FoundUser(u) => 
					case NoUser(u) => {
						errors.rejectValue("universityId", "uniNumber.userNotFound", Array(mark.universityId), "")
						noErrors = false
					}
				}
	 	 	  	// Reject if marks for this student are already uploaded
	 	 	  	assignment.feedbacks.find {(feedback) => feedback.universityId == mark.universityId && (feedback.hasMark || feedback.hasGrade )} match {
	 	 	  		case Some(feedback) => {
	 	 	  			errors.rejectValue("universityId", "uniNumber.duplicate.feedback")
	 	 	  			noErrors = false
	 	 	  		}
	 	 	  		case None => {}
	 	 	  	}
			}
		} else {
			errors.rejectValue("universityId", "NotEmpty")
		}
		// validate mark (must be int between 0 and 100)
		if (hasText(mark.actualMark)){
			try {
				val asInt = mark.actualMark.toInt
				if(asInt < 0 || asInt > 100) {
					errors.rejectValue("actualMark", "actualMark.range")
					noErrors = false
				}
			}
			catch{ 
				case _ => {
					errors.rejectValue("actualMark", "actualMark.format")
					noErrors = false
				}
			}
		}
		noErrors
	}
	
	@Transactional
	override def apply(): List[Feedback] = {
		def saveFeedback(universityId:String, actualMark:String, actualGrade:String)= {
			val feedback = assignment.findFeedback(universityId).getOrElse(new Feedback)	
		  	feedback.assignment = assignment
		  	feedback.uploaderId = submitter.apparentId
		  	feedback.universityId = universityId
		  	feedback.released = false
		  	feedback.actualMark = Option(actualMark.toInt)
			feedback.actualGrade = actualGrade
		  	session.saveOrUpdate(feedback)
		  	feedback
		}	
		
		// persist valid marks
		val markList = marks filter(_.isValid) map{(mark) => saveFeedback(mark.universityId, mark.actualMark, mark.actualGrade)}
		markList.toList	 	 
	}
  
	@Transactional
	def onBind {
		file.onBind
		if (!file.attached.isEmpty()) { 
			processFiles(file.attached)
		}
		
		def processFiles (files:Seq[FileAttachment]) {
			for(file <- files.filter(_.hasData)){
				marks = marks ++ marksExtractor.readXSSFExcelFile(file.dataStream)
			}
		}
	}
  
	def describe(d: Description) = d.assignment(assignment)

}