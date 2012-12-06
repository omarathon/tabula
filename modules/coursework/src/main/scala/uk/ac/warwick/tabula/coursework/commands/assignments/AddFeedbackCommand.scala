package uk.ac.warwick.tabula.coursework.commands.assignments

import java.util.ArrayList
import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import scala.util.matching.Regex
import org.springframework.validation.Errors
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.{MarkingCompleted, Assignment, Feedback, FileAttachment}
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.NoUser
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.util.core.StringUtils.hasText
import uk.ac.warwick.util.core.spring.FileUtils
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import uk.ac.warwick.spring.Wire
import scala.Some

class FeedbackItem {
	@BeanProperty var uniNumber: String = _
	@BeanProperty var file: UploadedFile = new UploadedFile

	@BeanProperty var submissionExists: Boolean = false
	@BeanProperty var duplicateFileNames: Set[String] = Set()

	def listAttachments() = file.attached.map(f => new AttachmentItem(f.name, duplicateFileNames.contains(f.name)))

	def this(uniNumber: String) = {
		this()
		this.uniNumber = uniNumber
	}

	class AttachmentItem(val name: String, val duplicate: Boolean){}
}

// Purely for storing in command to display on the model.
case class ProblemFile(
	@BeanProperty path: String,
	@BeanProperty file: FileAttachment) {
	def this() = this(null, null)
}

// Purely to generate an audit log event
class ExtractFeedbackZip(cmd: AddFeedbackCommand) extends Command[Unit] {
	def applyInternal() {}
	def describe(d: Description) = d.assignment(cmd.assignment).properties(
		"archive" -> cmd.archive.getOriginalFilename)
}

/**
 * Command which adds feedback for an assignment.
 * It either takes a single uniNumber and file, or it takes a
 * zip of files with uni numbers embedded in their path.
 *
 * TODO The controller that does single-file upload isn't exposed in the UI
 * so we could check that this is no longer being accessed by anyone, and then
 * remove all the code in here that handles it, to simplify it a little.
 */
class AddFeedbackCommand(val assignment: Assignment, val submitter: CurrentUser) extends Command[List[Feedback]] with Daoisms with Logging {

	val uniNumberPattern = new Regex("""(\d{7,})""")

	var zipService = Wire.auto[ZipService]
	var userLookup = Wire.auto[UserLookupService]
	var fileDao = Wire.auto[FileDao]
	var assignmentService = Wire.auto[AssignmentService]
	var submissionService = Wire.auto[SubmissionService]

	/* for single upload */
	@BeanProperty var uniNumber: String = _
	@BeanProperty var file: UploadedFile = new UploadedFile
	/* ----- */

	/* for multiple upload */
	// use lazy list with factory as spring doesn't know how to dynamically create items 
	@BeanProperty var items: JList[FeedbackItem] = LazyLists.simpleFactory()
	@BeanProperty var unrecognisedFiles: JList[ProblemFile] = LazyLists.simpleFactory()
	@BeanProperty var invalidFiles: JList[ProblemFile] = LazyLists.simpleFactory()
	@BeanProperty var archive: MultipartFile = _
	@BeanProperty var batch: Boolean = false
	@BeanProperty var fromArchive: Boolean = false
	@BeanProperty var confirmed: Boolean = false
	/* ---- */

	private def filenameOf(path: String) = new java.io.File(path).getName

	def preExtractValidation(errors: Errors) {
		if (batch) {
			if (archive != null && !archive.isEmpty()) {
				logger.info("file name is " + archive.getOriginalFilename())
				if (!"zip".equals(FileUtils.getLowerCaseExtension(archive.getOriginalFilename))) {
					errors.rejectValue("archive", "archive.notazip")
				}
			} else if (items != null && items.isEmpty() && file.isMissing) {
				errors.rejectValue("file.upload", "file.missing")
			}
		}
	}

	def postExtractValidation(errors: Errors) {
		if (!invalidFiles.isEmpty) errors.rejectValue("invalidFiles", "invalidFiles")
		if (items != null && !items.isEmpty) {
			for (i <- 0 until items.length) {
				val item = items.get(i)
				errors.pushNestedPath("items[" + i + "]")
				validateUploadedFile(item, errors)
				errors.popNestedPath()
			}
		} else {
			val tempItem = new FeedbackItem(uniNumber)
			tempItem.file = file
			validateUploadedFile(tempItem, errors)
		}
	}

	private def validateUploadedFile(item: FeedbackItem, errors: Errors) {
		val file = item.file
		val uniNumber = item.uniNumber

		if (file.isMissing) errors.rejectValue("file", "file.missing")
		if (hasText(uniNumber)) {
			if (!UniversityId.isValid(uniNumber)) {
				errors.rejectValue("uniNumber", "uniNumber.invalid")
			} else {
				userLookup.getUserByWarwickUniId(uniNumber) match {
					case FoundUser(u) =>
					case NoUser(u) => errors.rejectValue("uniNumber", "uniNumber.userNotFound", Array(uniNumber), "")
				}

				// warn if feedback for this student is already uploaded
				assignment.feedbacks.find { feedback => feedback.universityId == uniNumber && feedback.hasAttachments } match {
					case Some(feedback) => {
						// set warning flag for existing feedback and check if any existing files will be overwritten
						item.submissionExists = true
						checkForDuplicateFiles(item, feedback)
					}
					case None => {}
				}

			}
		} else {
			errors.rejectValue("uniNumber", "NotEmpty")
		}
	}

	private def checkForDuplicateFiles(item: FeedbackItem, feedback: Feedback){
		val attachedFiles = item.file.attachedFileNames.toSet
		val feedbackFiles = feedback.attachments.map(file => file.getName).toSet
		item.duplicateFileNames = attachedFiles & feedbackFiles
	}

	def onBind = transactional() {
		file.onBind

		def store(itemMap: collection.mutable.Map[String, FeedbackItem], number: String, name: String, file: FileAttachment) =
			itemMap.getOrElseUpdate(number, new FeedbackItem(uniNumber = number))
				.file.attached.add(file)

		def processFiles(bits: Seq[Pair[String, FileAttachment]]) {
			// go through individual files, extracting the uni number and grouping
			// them into feedback items.
			var itemMap = new collection.mutable.HashMap[String, FeedbackItem]()
			unrecognisedFiles.clear()

			for ((filename, file) <- bits) {
				// match uni numbers found in file path
				val allNumbers = uniNumberPattern.findAllIn(filename).matchData.map { _.subgroups(0) }.toList

				// ignore any numbers longer than 7 characters.
				val numbers = allNumbers.filter { _.length == 7 }

				if (numbers.isEmpty) {
					// no numbers at all.
					unrecognisedFiles.add(new ProblemFile(filename, file))
				} else if (numbers.distinct.size > 1) {
					// multiple different numbers, ambiguous, reject this. 
					invalidFiles.add(new ProblemFile(filename, file))
				} else {
					// one 7 digit number, this one might be okay.
					store(itemMap, numbers.head, filenameOf(filename), file)
				}
			}

			items = new ArrayList(itemMap.values().toList)
		}

		// ZIP has been uploaded. unpack it
		if (archive != null && !archive.isEmpty()) {
			val zip = new ZipArchiveInputStream(archive.getInputStream)

			val bits = Zips.iterator(zip) { (iterator) =>
				for (entry <- iterator if !entry.isDirectory) yield {
					val f = new FileAttachment
					// Funny char from Windows? We can't work out what it is so
					// just turn it into an underscore.
					val name = entry.getName.replace("\uFFFD", "_")
					f.name = filenameOf(name)
					f.uploadedData = new ZipEntryInputStream(zip, entry)
					f.uploadedDataLength = entry.getSize
					fileDao.saveTemporary(f)
					(name, f)
				}
			}

			processFiles(bits)

			// remember we got these items from a Zip, so we can tailor the text in the HTML.
			fromArchive = true

			// this do-nothing command is to generate an audit event to record the unzipping
			new ExtractFeedbackZip(this).apply()

		} else {
			if (batch && !file.attached.isEmpty()) {
				val bits = file.attached.map { (attachment) => attachment.name -> attachment }
				processFiles(bits)
			}

			if (items != null) {
				for (item <- items if item.file != null) item.file.onBind
			}
		}

	}

	override def applyInternal(): List[Feedback] = transactional() {

		def saveFeedback(uniNumber: String, file: UploadedFile) = {
			val feedback = assignment.findFeedback(uniNumber).getOrElse(new Feedback)
			feedback.assignment = assignment
			feedback.uploaderId = submitter.apparentId
			feedback.universityId = uniNumber
			feedback.released = false
			for (attachment <- file.attached){
				// if an attachment with the same name as this one exists then delete it
				val duplicateAttachment = feedback.attachments.find(_.name == attachment.name)
				duplicateAttachment.foreach(session.delete(_))
				feedback addAttachment attachment
			}
			session.saveOrUpdate(feedback)
			updateSubmissionState(uniNumber)

			feedback
		}

		def updateSubmissionState(uniNumber: String) {
			val submission = assignmentService.getSubmissionByUniId(assignment, uniNumber)
			submission.foreach(submissionService.updateState(_, MarkingCompleted))
		}

		if (items != null && !items.isEmpty()) {

			val feedbacks = items.map { (item) =>
				val feedback = saveFeedback(item.uniNumber, item.file)
				zipService.invalidateIndividualFeedbackZip(feedback)
				feedback
			}

			zipService.invalidateFeedbackZip(assignment)
			feedbacks.toList

		} else {

			val feedback = saveFeedback(uniNumber, file)

			// delete feedback zip for this assignment, since it'll now be different.
			// TODO should really do this in a more general place, like a save listener for Feedback objects
			zipService.invalidateFeedbackZip(assignment)

			List(feedback)
		}
	}

	//  def filenameOf(entry:ZipEntry):String = {
	//		entry.getName match {
	//			case directoryPattern(number, name) => name
	//			case filePattern(number, name) => name
	//			case anyFilePattern(name) => name 
	//		}
	//	}

	def describe(d: Description) = d
		.assignment(assignment)
		.studentIds(items.map { _.uniNumber })

}

