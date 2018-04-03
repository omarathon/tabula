package uk.ac.warwick.tabula.commands.scheduling.imports


import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Unaudited}
import uk.ac.warwick.tabula.data.{StudentCourseDetailNoteDao, StudentCourseDetailsDao}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentCourseDetailsNote}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.StudentCourseDetailsNoteRow
import scala.util.matching.Regex


object ImportCourseDetailsNoteCommand {
	val ScjMatch: Regex = "SC.:([^~]*).*".r
	val SprMatch: Regex = "SPR:([^~]*).*".r
}

class ImportCourseDetailsNoteCommand(row: StudentCourseDetailsNoteRow) extends Command[Option[StudentCourseDetailsNote]]
	with Logging with Unaudited with PropertyCopying {

	import ImportCourseDetailsNoteCommand._

	PermissionCheck(Permissions.ImportSystemData)

	var studentCourseDetailsDao: StudentCourseDetailsDao = Wire[StudentCourseDetailsDao]
	var studentCourseDetailNoteDao: StudentCourseDetailNoteDao = Wire[StudentCourseDetailNoteDao]

	val code: String = row.code
	var scjCode: String = _
	val note: String = row.note

	private val properties = Set("code", "scjCode", "note")

	override def applyInternal(): Option[StudentCourseDetailsNote] = transactional() {

		val studentCourseDetails: Option[StudentCourseDetails] = row.code match {
			case ScjMatch(scj) =>
				logger.info(s"Found note for SCJ - $scj")
				studentCourseDetailsDao.getByScjCodeStaleOrFresh(scj)
			case SprMatch(spr) =>
				logger.info(s"Found note for SPR - $spr")
				studentCourseDetailsDao.getBySprCodeStaleOrFresh(spr).headOption
			case _ => None
		}

		studentCourseDetails match {
			case None =>
				logger.warn("Can't record SCD note - could not find a StudentCourseDetails for " + row.code)
				None
			case Some(scd: StudentCourseDetails) =>
				scjCode = scd.scjCode
				storeNote()
		}
	}


	def storeNote(): Some[StudentCourseDetailsNote] = {
		val noteExisting: Option[StudentCourseDetailsNote] = studentCourseDetailNoteDao.getNoteByCode(row.code)
		val isTransient = noteExisting.isEmpty

		val studentCourseDetailsNote = noteExisting.getOrElse(new StudentCourseDetailsNote(code, scjCode, note))

		val commandBean = new BeanWrapperImpl(this)
		val noteBean = new BeanWrapperImpl(studentCourseDetailsNote)
		val hasChanged = copyBasicProperties(properties, commandBean, noteBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + studentCourseDetailsNote.code)
			studentCourseDetailNoteDao.saveOrUpdate(studentCourseDetailsNote)
		}

		Some(studentCourseDetailsNote)
	}



}
