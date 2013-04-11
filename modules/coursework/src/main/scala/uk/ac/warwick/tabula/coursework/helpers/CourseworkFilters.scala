package uk.ac.warwick.tabula.coursework.helpers

import org.joda.time.DateTime
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils

import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.CaseObjectEqualityFixes
import uk.ac.warwick.tabula.coursework.commands.assignments.Student
import uk.ac.warwick.tabula.data.convert.JodaDateTimeConverter
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.MarkingMethod
import uk.ac.warwick.tabula.data.model.MarkingState
import uk.ac.warwick.tabula.system.TwoWayConverter

/**
 * Filters a set of "Student" case objects (which are a representation of the current 
 * state of a single student's submission workflow on an assignment, containing the
 * submission, extension and feedback where available). Provides a predicate for
 * filtering Student objects, and an applies() method to see whether it is even relevant
 * for an assigment (for example, if an assignment doesn't take submissions, there's no
 * point offering a filter for Unsubmitted students).
 */
sealed abstract class CourseworkFilter extends CaseObjectEqualityFixes[CourseworkFilter] {
	def getName = CourseworkFilters.shortName(getClass.asInstanceOf[Class[_ <: CourseworkFilter]])
	def getDescription: String
	def predicate(parameters: Map[String, String])(student: Student): Boolean
	def applies(assignment: Assignment): Boolean
	def validate(parameters: Map[String, String], fieldName: String = "filterParameters")(errors: Errors): Unit
	def parameters: Seq[(String, String, String)]
}

abstract class ParameterlessCourseworkFilter extends CourseworkFilter {
	def predicate(student: Student): Boolean
	final def predicate(parameters: Map[String, String])(student: Student): Boolean = predicate(student)
	final def validate(parameters: Map[String, String], fieldName: String)(errors: Errors) {}
	final override def parameters = Seq()
}

object CourseworkFilters {
	private val ObjectClassPrefix = CourseworkFilters.getClass.getName
	lazy val AllFilters = Seq(
		AllStudents, SubmittedBetweenDates, OnTime, WithExtension, WithinExtension, WithWordCount, Unsubmitted,
		NotReleasedForMarking, NotMarked, MarkedByFirst, MarkedBySecond,
		CheckedForPlagiarism, NotCheckedForPlagiarism, MarkedPlagiarised, WithSimilarityPercentage,
		NoFeedback, FeedbackNotReleased, FeedbackNotDownloaded
	)
	
	/**
	 * Create a Filter from a name (e.g. "AllStudents").
	 * Most likely useful in view templates.
	 *
	 * Note that, like the templates they're used in, the correctness isn't
	 * checked at runtime.
	 */
	def of(name: String): CourseworkFilter = {
		try {
			// Go through the magical hierarchy
			val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$') + "$")
			clz.getDeclaredField("MODULE$").get(null).asInstanceOf[CourseworkFilter]
		} catch {
			case e: ClassNotFoundException => throw new IllegalArgumentException("Filter " + name + " not recognised")
			case e: ClassCastException => throw new IllegalArgumentException("Filter " + name + " is not an endpoint of the hierarchy")
		}
	}
	
	def shortName(clazz: Class[_ <: CourseworkFilter])
		= clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length - 1).replace('$', '.')
	
	case object AllStudents extends ParameterlessCourseworkFilter {
		def getDescription = "students"
		def predicate(item: Student) = {
			true
		}
		def applies(assignment: Assignment) = true
	}
	
	case object SubmittedBetweenDates extends CourseworkFilter {
		final val converter = new JodaDateTimeConverter
		
		def getDescription = "students who submitted between..."
			
		def parameters = Seq(
			("startDate", "Start date", "datetime"),
			("endDate", "End date", "datetime")
		)	
		def predicate(parameters: Map[String, String])(item: Student) = {
			val start = converter.convertRight(parameters("startDate"))
			val end = converter.convertRight(parameters("endDate"))
			
			def betweenDates(dt: DateTime) =
				dt != null && 
				(dt == start || dt.isAfter(start)) &&
				(dt == end || dt.isBefore(end))
			
			(item.coursework.enhancedSubmission map { item => betweenDates(item.submission.submittedDate) }) getOrElse(false)
		}
		def validate(parameters: Map[String, String], fieldName: String = "filterParameters")(errors: Errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "%s[startDate]".format(fieldName), "NotEmpty")
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "%s[endDate]".format(fieldName), "NotEmpty")
			
			if (!errors.hasErrors) {
				val start = converter.convertRight(parameters("startDate"))
				if (start == null) errors.rejectValue("%s[startDate]".format(fieldName), "typeMismatch.org.joda.time.DateTime")
				
				val end = converter.convertRight(parameters("endDate"))
				if (end == null) errors.rejectValue("%s[endDate]".format(fieldName), "typeMismatch.org.joda.time.DateTime")
				
				if (start != null && end != null && !end.isAfter(start))
					errors.rejectValue("%s[endDate]".format(fieldName), "filters.SubmittedBetweenDates.end.beforeStart")
			}
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions
	}
	
	case object OnTime extends ParameterlessCourseworkFilter {
		def getDescription = "students who submitted on time"
		def predicate(item: Student) = {
			(item.coursework.enhancedSubmission map { item => !item.submission.isLate && !item.submission.isAuthorisedLate }) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions
	}
	
	case object WithExtension extends ParameterlessCourseworkFilter {
		def getDescription = "students with extensions"
		def predicate(item: Student) = {
			item.coursework.enhancedExtension.isDefined
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.allowExtensions
	}
	
	case object WithinExtension extends ParameterlessCourseworkFilter {
		def getDescription = "students who submitted within extension"
		def predicate(item: Student) = {
			(item.coursework.enhancedSubmission map { item => item.submission.isAuthorisedLate }) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.allowExtensions
	}
	
	case object WithWordCount extends CourseworkFilter {
		private def toInt(text: String) = 
			if (text.hasText) try {	Some(text.toInt) } catch { case e: NumberFormatException => None }
			else None
		
		def getDescription = "students who submitted with word count between..."
			
		def parameters = Seq(
			("minWords", "Min word count", "number"),
			("maxWords", "Max word count", "number")
		)	
		def predicate(parameters: Map[String, String])(item: Student) = {
			val min = toInt(parameters("minWords")).get
			val max = toInt(parameters("maxWords")).get
			
			(item.coursework.enhancedSubmission flatMap { item => 
				val submission = item.submission
				val assignment = submission.assignment
				
				assignment.wordCountField flatMap { field =>
					submission.valuesByFieldName.get(field.name) flatMap { toInt(_) }
				} map { words => 
					(words >= min && words <= max)
				}
			}) getOrElse(false)
		}
		def validate(parameters: Map[String, String], fieldName: String = "filterParameters")(errors: Errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "%s[minWords]".format(fieldName), "NotEmpty")
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "%s[maxWords]".format(fieldName), "NotEmpty")
			
			if (!errors.hasErrors) {
				val min = toInt(parameters("minWords"))
				if (min.isEmpty) errors.rejectValue("%s[minWords]".format(fieldName), "typeMismatch")
				
				val max = toInt(parameters("maxWords"))
				if (max.isEmpty) errors.rejectValue("%s[maxWords]".format(fieldName), "typeMismatch")
				
				if (min.isDefined && max.isDefined) {
					if (max.get < min.get)
						errors.rejectValue("%s[maxWords]".format(fieldName), "filters.WithWordCount.max.lessThanMin")
						
					if (min.get < 0)
						errors.rejectValue("%s[minWords]".format(fieldName), "filters.WithWordCount.min.lessThanZero")
						
					if (max.get < 0)
						errors.rejectValue("%s[maxWords]".format(fieldName), "filters.WithWordCount.max.lessThanZero")
				}
			}
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.wordCountField.isDefined
	}
	
	case object Unsubmitted extends ParameterlessCourseworkFilter {
		def getDescription = "students who have not submitted an assignment"
		def predicate(item: Student) = {
			item.coursework.enhancedSubmission.isEmpty
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions
	}
	
	case object NotReleasedForMarking extends ParameterlessCourseworkFilter {
		def getDescription = "submissions that have not been released for marking"
		def predicate(item: Student) = {
			(item.coursework.enhancedSubmission map { item => !item.submission.isReleasedForMarking }) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.markingWorkflow != null
	}
	
	case object NotMarked extends ParameterlessCourseworkFilter {
		def getDescription = "submissions not marked"
		def predicate(item: Student) = {
			(item.coursework.enhancedSubmission map { item => 
				val releasedForMarking = item.submission.isReleasedForMarking
				val hasFirstMarker = item.submission.assignment.getStudentsFirstMarker(item.submission).isDefined
				releasedForMarking && hasFirstMarker
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.markingWorkflow != null
	}
	
	case object MarkedByFirst extends ParameterlessCourseworkFilter {
		def getDescription = "submissions marked by first marker"
		def predicate(item: Student) = {
			(item.coursework.enhancedSubmission map { item => 
				val releasedToSecondMarker = item.submission.isReleasedToSecondMarker
				val markingCompleted = item.submission.state == MarkingState.MarkingCompleted
				releasedToSecondMarker || markingCompleted
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.markingWorkflow != null
	}
	
	case object MarkedBySecond extends ParameterlessCourseworkFilter {
		def getDescription = "submissions marked by second marker"
			
		def predicate(item: Student) = {
			(item.coursework.enhancedSubmission map { item => 
				item.submission.state == MarkingState.MarkingCompleted
			}) getOrElse(false)
		}
		
		// Only applies to seen second marking
		def applies(assignment: Assignment) = 
			assignment.collectSubmissions && 
			assignment.markingWorkflow != null && 
			assignment.markingWorkflow.markingMethod == MarkingMethod.SeenSecondMarking
	}
	
	case object CheckedForPlagiarism extends ParameterlessCourseworkFilter {
		def getDescription = "submissions checked for plagiarism"
		def predicate(item: Student) = {
			(item.coursework.enhancedSubmission map { item => 
				item.submission.hasOriginalityReport.booleanValue()
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.module.department.plagiarismDetectionEnabled
	}
	
	case object NotCheckedForPlagiarism extends ParameterlessCourseworkFilter {
		def getDescription = "submissions not checked for plagiarism"
		def predicate(item: Student) = {
			(item.coursework.enhancedSubmission map { item => 
				!item.submission.hasOriginalityReport.booleanValue()
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.module.department.plagiarismDetectionEnabled
	}
	
	case object MarkedPlagiarised extends ParameterlessCourseworkFilter {
		def getDescription = "submissions marked as plagiarised"
		def predicate(item: Student) = { 
			(item.coursework.enhancedSubmission map { item => 
				item.submission.suspectPlagiarised.booleanValue
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions
	}
	
	case object WithSimilarityPercentage extends CourseworkFilter {
		private def toInt(text: String) = 
			if (text.hasText) try {	Some(text.toInt) } catch { case e: NumberFormatException => None }
			else None
		
		def getDescription = "submissions with a plagiarism similarity percentage between..."
			
		def parameters = Seq(
			("minSimilarity", "Min similarity %", "percentage"),
			("maxSimilarity", "Max similarity %", "percentage")
		)	
		def predicate(parameters: Map[String, String])(item: Student) = {
			val min = toInt(parameters("minSimilarity")).get
			val max = toInt(parameters("maxSimilarity")).get
			
			(item.coursework.enhancedSubmission map { item => 
				item.submission.allAttachments flatMap { 
					a => Option(a.originalityReport) flatMap { _.similarity } 
				} map { similarity => 
					(similarity >= min && similarity <= max)
				} exists { b => b }
			}) getOrElse(false)
		}
		def validate(parameters: Map[String, String], fieldName: String = "filterParameters")(errors: Errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "%s[minSimilarity]".format(fieldName), "NotEmpty")
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "%s[maxSimilarity]".format(fieldName), "NotEmpty")
			
			if (!errors.hasErrors) {
				val min = toInt(parameters("minSimilarity"))
				if (min.isEmpty) errors.rejectValue("%s[minSimilarity]".format(fieldName), "typeMismatch")
				
				val max = toInt(parameters("maxSimilarity"))
				if (max.isEmpty) errors.rejectValue("%s[maxSimilarity]".format(fieldName), "typeMismatch")
				
				if (min.isDefined && max.isDefined) {
					if (max.get < min.get)
						errors.rejectValue("%s[maxSimilarity]".format(fieldName), "filters.WithSimilarityPercentage.max.lessThanMin")
						
					if (min.get < 0 || min.get > 100)
						errors.rejectValue("%s[minSimilarity]".format(fieldName), "filters.WithSimilarityPercentage.min.notInRange")
						
					if (max.get < 0 || max.get > 100)
						errors.rejectValue("%s[maxSimilarity]".format(fieldName), "filters.WithSimilarityPercentage.max.notInRange")
				}
			}
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.module.department.plagiarismDetectionEnabled
	}
	
	case object NoFeedback extends ParameterlessCourseworkFilter {
		def getDescription = "submissions with no feedback"
		def predicate(item: Student) = {
			!item.coursework.enhancedFeedback.isDefined
		}
		def applies(assignment: Assignment) = true
	}
	
	case object FeedbackNotReleased extends ParameterlessCourseworkFilter {
		def getDescription = "feedbacks not published"
		def predicate(item: Student) = { 
			(item.coursework.enhancedFeedback map { item => 
				!item.feedback.released
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = true
	}
	
	case object FeedbackNotDownloaded extends ParameterlessCourseworkFilter {
		def getDescription = "feedbacks not downloaded by students"
		def predicate(item: Student) = { 
			(item.coursework.enhancedFeedback map { item => 
				!item.downloaded
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = true
	}
}