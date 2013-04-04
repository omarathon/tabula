package uk.ac.warwick.tabula.coursework.helpers

import org.hibernate.annotations.Filters
import uk.ac.warwick.tabula.coursework.commands.assignments.Student
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.MarkingState
import uk.ac.warwick.tabula.data.model.MarkingMethod

sealed abstract class CourseworkFilter {
	def getName = CourseworkFilters.shortName(getClass.asInstanceOf[Class[_ <: CourseworkFilter]])
	def getDescription: String 
	def predicate: (Student => Boolean)
	def applies(assignment: Assignment): Boolean
	
	/* We need to override equals() here because under heavy load, the class loader will 
	 * (stupidly) return a different instance of the case object, which fails the equality
	 * check because the default AnyRef implementation of equals is just this eq that.
	 * 
	 * We also have to override hashCodes because their default is computed at compile time,
	 * based only on the (unqualified) name of the current case object, so,
	 * before override, Module.Create.hashCode() == PersonalTutor.Create.hashCode()
	 */
	override def equals(other: Any) = other match {
		case that: CourseworkFilter => getName == that.getName
		case _ => false
	}
	override def hashCode() = getName.hashCode()
	override def toString() = getName
}

object CourseworkFilters {
	private val ObjectClassPrefix = CourseworkFilters.getClass.getName
	val AllFilters = Seq(
		AllStudents, OnTime, WithExtension, WithinExtension, Unsubmitted,
		NotReleasedForMarking, NotMarked, MarkedByFirst, MarkedBySecond,
		CheckedForPlagiarism, NotCheckedForPlagiarism, MarkedPlagiarised,
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
	
	case object AllStudents extends CourseworkFilter {
		def getDescription = "students"
		def predicate = { item: Student =>
			true
		}
		def applies(assignment: Assignment) = true
	}
	
	case object OnTime extends CourseworkFilter {
		def getDescription = "students who submitted on time"
		def predicate = { item: Student =>
			(item.coursework.enhancedSubmission map { item => !item.submission.isLate && !item.submission.isAuthorisedLate }) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions
	}
	
	case object WithExtension extends CourseworkFilter {
		def getDescription = "students with extensions"
		def predicate = { item: Student =>
			item.coursework.enhancedExtension.isDefined
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.allowExtensions
	}
	
	case object WithinExtension extends CourseworkFilter {
		def getDescription = "students who submitted within extension"
		def predicate = { item: Student =>
			(item.coursework.enhancedSubmission map { item => item.submission.isAuthorisedLate }) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.allowExtensions
	}
	
	case object Unsubmitted extends CourseworkFilter {
		def getDescription = "students who have not submitted an assignment"
		def predicate = { item: Student =>
			item.coursework.enhancedSubmission.isEmpty
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions
	}
	
	case object NotReleasedForMarking extends CourseworkFilter {
		def getDescription = "submissions that have not been released for marking"
		def predicate = { item: Student =>
			(item.coursework.enhancedSubmission map { item => item.submission.isReleasedForMarking }) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.markingWorkflow != null
	}
	
	case object NotMarked extends CourseworkFilter {
		def getDescription = "submissions not marked"
		def predicate = { item: Student =>
			(item.coursework.enhancedSubmission map { item => 
				val releasedForMarking = item.submission.isReleasedForMarking
				val hasFirstMarker = item.submission.assignment.getStudentsFirstMarker(item.submission).isDefined
				releasedForMarking && hasFirstMarker
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.markingWorkflow != null
	}
	
	case object MarkedByFirst extends CourseworkFilter {
		def getDescription = "submissions marked by first marker"
		def predicate = { item: Student =>
			(item.coursework.enhancedSubmission map { item => 
				val releasedToSecondMarker = item.submission.isReleasedToSecondMarker
				val markingCompleted = item.submission.state == MarkingState.MarkingCompleted
				releasedToSecondMarker || markingCompleted
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.markingWorkflow != null
	}
	
	case object MarkedBySecond extends CourseworkFilter {
		def getDescription = "submissions marked by second marker"
			
		def predicate = { item: Student =>
			(item.coursework.enhancedSubmission map { item => 
				item.submission.state == MarkingState.MarkingCompleted
			}) getOrElse(false)
		}
		
		// Only applies to seen second marking
		def applies(assignment: Assignment) = 
			assignment.collectSubmissions && 
			assignment.markingWorkflow != null && 
			assignment.markingWorkflow.getMarkingMethod == MarkingMethod.SeenSecondMarking
	}
	
	case object CheckedForPlagiarism extends CourseworkFilter {
		def getDescription = "submissions checked for plagiarism"
		def predicate = { item: Student =>
			(item.coursework.enhancedSubmission map { item => 
				item.submission.allAttachments.find(_.originalityReport != null).isDefined
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.module.department.plagiarismDetectionEnabled
	}
	
	case object NotCheckedForPlagiarism extends CourseworkFilter {
		def getDescription = "submissions not checked for plagiarism"
		def predicate = { item: Student => !CheckedForPlagiarism.predicate(item) }
		def applies(assignment: Assignment) = assignment.collectSubmissions && assignment.module.department.plagiarismDetectionEnabled
	}
	
	case object MarkedPlagiarised extends CourseworkFilter {
		def getDescription = "submissions marked as plagiarised"
		def predicate = { item: Student => 
			(item.coursework.enhancedSubmission map { item => 
				item.submission.suspectPlagiarised.booleanValue
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = assignment.collectSubmissions
	}
	
	case object NoFeedback extends CourseworkFilter {
		def getDescription = "submissions with no feedback"
		def predicate = { item: Student =>
			!item.coursework.enhancedFeedback.isDefined
		}
		def applies(assignment: Assignment) = true
	}
	
	case object FeedbackNotReleased extends CourseworkFilter {
		def getDescription = "feedback not published"
		def predicate = { item: Student => 
			(item.coursework.enhancedFeedback map { item => 
				!item.feedback.released
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = true
	}
	
	case object FeedbackNotDownloaded extends CourseworkFilter {
		def getDescription = "feedback not downloaded by student"
		def predicate = { item: Student => 
			(item.coursework.enhancedFeedback map { item => 
				!item.downloaded
			}) getOrElse(false)
		}
		def applies(assignment: Assignment) = true
	}
}