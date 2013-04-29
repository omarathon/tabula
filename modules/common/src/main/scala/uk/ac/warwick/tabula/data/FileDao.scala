package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.model.FileAttachment
import org.hibernate.Hibernate
import org.springframework.stereotype.Repository
import java.io.BufferedInputStream
import org.joda.time.DateTime
import org.joda.time.DateTime.now
import org.joda.time.ReadableInstant
import uk.ac.warwick.tabula.data.model.Feedback
import java.io.File
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.InitializingBean
import org.springframework.util.FileCopyUtils
import java.io.FileOutputStream
import java.io.InputStream
import org.hibernate.criterion.{ Restrictions => Is }
import org.hibernate.criterion.Order._
import collection.JavaConversions._
import collection.JavaConverters._
import uk.ac.warwick.util.core.spring.FileUtils
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.transaction.annotation.Propagation._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.criterion.Projections
import org.hibernate.`type`.StringType
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.util.files.hash.impl.SHAFileHasher
import uk.ac.warwick.util.files.hash.FileHasher
import uk.ac.warwick.spring.Wire

@Repository
class FileDao extends Daoisms with InitializingBean with Logging {

	@Value("${filesystem.attachment.dir}") var attachmentDir: File = _
	@Value("${filesystem.create.missing}") var createMissingDirectories: Boolean = _
	
	var fileHasher = Wire[FileHasher]

	val idSplitSize = 2
	val idSplitSizeCompat = 4 // for existing paths split by 4 chars

	val TemporaryFileBatch = 1000 // query for this many each time
	val TemporaryFileSubBatch = 50 // run a separate transaction for each one

	private def partition(id: String, splitSize: Int): String = id.replace("-", "").grouped(splitSize).mkString("/")
	private def partition(id: String): String = partition(id, idSplitSize)
	private def partitionCompat(id: String): String = partition(id, idSplitSizeCompat)

	/**
	 * Retrieves a File object where you can store data under this ID. It doesn't check
	 * whether the File already exists. If you want to retrieve an existing file you must
	 * use #getData which checks whether it exists and also knows to check the old-style path if needed.
	 */
	def targetFile(id: String): File = new File(attachmentDir, partition(id))
	def targetFileCompat(id: String): File = new File(attachmentDir, partitionCompat(id))

	private def saveAttachment(file: FileAttachment) {
		if ((!file.id.hasText || !file.hasData) && file.uploadedData != null) {
			file.hash = fileHasher.hash(file.uploadedData())
		}
		
		session.saveOrUpdate(file)
		
		if (!file.hasData && file.uploadedData != null) {
			persistFileData(file, file.uploadedData())
		}
	}

	def saveTemporary(file: FileAttachment) {
		file.temporary = true
		saveAttachment(file)
	}

	def savePermanent(file: FileAttachment) {
		file.temporary = false
		saveAttachment(file)
	}
	
	def saveOrUpdate(file: FileAttachment) = session.saveOrUpdate(file)

	def persistFileData(file: FileAttachment, inputStream: InputStream) {
		val target = targetFile(file.id)
		val directory = target.getParentFile()
		directory.mkdirs()
		if (!directory.exists) throw new IllegalStateException("Couldn't create directory to store file")
		FileCopyUtils.copy(inputStream, new FileOutputStream(target))
	}

	def getFileById(id: String) = getById[FileAttachment](id)

	def getFileByStrippedId(id: String) = transactional(readOnly = true) {
		session.newCriteria[FileAttachment]
				.add(Is.sqlRestriction("replace({alias}.id, '-', '') = ?", id, StringType.INSTANCE))
				.setMaxResults(1)
				.uniqueResult
	}

	/** Only for use by FileAttachment to find its own backing file. */
	def getData(id: String): Option[File] = targetFile(id) match {
		case file: File if file.exists => Some(file)
		// If no file found, check if it's stored under old 4-character path style
		case _ => targetFileCompat(id) match {
			case file: File if file.exists => Some(file)
			case _ => None
		}
	}

	def getFilesCreatedSince(createdSince: DateTime, maxResults: Int): Seq[FileAttachment] = transactional(readOnly = true) {
		session.newCriteria[FileAttachment]
				.add(Is.ge("dateUploaded", createdSince))
				.setMaxResults(maxResults)
				.addOrder(asc("dateUploaded"))
				.addOrder(asc("id"))
				.list
	}

	def getFilesCreatedOn(createdOn: DateTime, maxResults: Int, startingId: String): Seq[FileAttachment] = transactional(readOnly = true) {
		val criteria =
			session.newCriteria[FileAttachment]
				.add(Is.eq("dateUploaded", createdOn))

		if (startingId.hasText)
			criteria.add(Is.gt("id", startingId))

		criteria
			.setMaxResults(maxResults)
			.addOrder(asc("id"))
			.list
	}

	def getAllFileIds(createdBefore: Option[DateTime] = None): Set[String] = transactional(readOnly = true) {
		val criteria =
			session.newCriteria[FileAttachment]
				.setProjection(Projections.id())

		createdBefore.map { date =>
			criteria.add(Is.lt("dateUploaded", date))
		}
		criteria.listOf[String].toSet
	}

	/**
	 * Delete any temporary blobs that are more than 2 days old.
	 */
	def deleteOldTemporaryFiles = {
		val oldFiles = findOldTemporaryFiles
		/*
		 * This is a fun time for getting out of sync.
		 * Trying to run a few at a time in a separate transaction so that if something
		 * goes rubbish, there isn't too much out of sync.
		 */
		for (files <- oldFiles.grouped(TemporaryFileSubBatch)) deleteSomeFiles(files)

		oldFiles.size
	}

	private def findOldTemporaryFiles = transactional() {
		session.newCriteria[FileAttachment]
			.add(Is.eq("temporary", true))
			.add(Is.lt("dateUploaded", now minusDays (2)))
			.setMaxResults(TemporaryFileBatch)
			.list
	}

	private def deleteSomeFiles(files: Seq[FileAttachment]) {
		transactional(propagation = REQUIRES_NEW) {
			// To be safe, split off temporary files which are attached to non-temporary things
			// (which shouldn't happen, but we definitely don't want to delete things because of a bug elsewhere)
			// WARNING isAttached isn't exhaustive so this won't protect you all the time.
			val (dontDelete, okayToDelete) = files partition (_.isAttached)

			if (dontDelete.size > 0) {
				// Somewhere else in the app is failing to set temporary=false
				logger.error(
					"%d fileAttachments are temporary but are attached to another entity! " +
					"I won't delete them, but this is a bug that needs fixing!!" format dontDelete.size
				)
			}

			session.newQuery[FileAttachment]("delete FileAttachment f where f.id in :ids")
				.setParameterList("ids", okayToDelete.map(_.id))
				.run()
			for (attachment <- files; file <- getData(attachment.id)) {
				file.delete()
			}
		}
	}

	def afterPropertiesSet {
		if (!attachmentDir.isDirectory()) {
			if (createMissingDirectories) {
				attachmentDir.mkdirs();
			} else {
				throw new IllegalStateException("Attachment store '" + attachmentDir + "' must be an existing directory");
			}
		}
	}
}
