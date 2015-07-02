package uk.ac.warwick.tabula.services.turnitinlti

import uk.ac.warwick.tabula.data.model.FileAttachment

/**
 * Information about a submission as returned by the Turnitin API.
 */
case class TurnitinLtiSubmissionInfo(
	val objectId: DocumentId,
	val title: String, // paper title
	val universityId: String, // stored under student's first name in Turnitin
	var similarityScore: Int,
	var overlap: Option[Int],
	var webOverlap: Option[Int],
	var publicationOverlap: Option[Int],
	var studentPaperOverlap: Option[Int]) {
	/** If plagiarism checking hasn't been done yet, it will have a score of -1. */
	def hasBeenChecked = similarityScore != -1
	
	/** Whether this appears to match up with the given FileAttachment according to our naming conventions,
	 * which is to extract the ID from the paper title and compare that against the FileAttachment ID. 
	 */
	def matches(attachment: FileAttachment) = title == attachment.id
}