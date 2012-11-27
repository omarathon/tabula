package uk.ac.warwick.tabula.data.model

/**
 * An enum of possible submissions states. import using import uk.ac.warwick.tabula.data.model.SubmissionState._
 * so that the internal type declaration is imported as well. Then you can define fields with a type of SubmissionState
 *
 * stored in its own file to avoid cyclic import issues
 */

object SubmissionState extends Enumeration {
	type SubmissionState = Value
	val Received = Value("Received") // initial state - received but has not yet been released for marking
	val ReleasedForMarking = Value("ReleasedForMarking") // ready to be distributed to markers
	val DownloadedByMarker = Value("DownloadedByMarker") // has been downloaded by the marker and is being marked
	val MarkingCompleted = Value("MarkingCompleted") // submission has been marked and feedback has been uploaded
}