package uk.ac.warwick.courses.data
import uk.ac.warwick.courses.data.model.FileAttachment
import org.hibernate.Hibernate
import org.springframework.stereotype.Repository
import java.io.BufferedInputStream
import org.joda.time.DateTime
import org.joda.time.DateTime.now
import org.joda.time.ReadableInstant
import uk.ac.warwick.courses.data.model.Feedback
import java.io.File
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.InitializingBean
import org.springframework.util.FileCopyUtils
import java.io.FileOutputStream
import java.io.InputStream
import org.hibernate.criterion.{Restrictions => Is}
import collection.JavaConversions._
import uk.ac.warwick.util.core.spring.FileUtils
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Propagation

@Repository
class FileDao extends Daoisms with InitializingBean {
	
	@Value("${filesystem.attachment.dir}") var attachmentDir:File =_
	@Value("${filesystem.create.missing}") var createMissingDirectories:Boolean =_
	
	val idSplitSize = 4
	
	val TemporaryFileBatch = 1000 // query for this many each time
	val TemporaryFileSubBatch = 10 // run a separate transaction for each one
	
	private def partition(id:String): String = id.replace("-","").grouped(idSplitSize).mkString("/")
	
	def targetFile(id:String): File = new File(attachmentDir, partition(id)) 
	
	def saveTemporary(file:FileAttachment) :Unit = {
		session.saveOrUpdate(file)
		if (!file.hasData && file.uploadedData != null) {
			persistFileData(file, file.uploadedData)
		}	
	}
	
	def persistFileData(file:FileAttachment, inputStream:InputStream) {
	    val target = targetFile(file.id)
		val directory = target.getParentFile()
		directory.mkdirs()
		if (!directory.exists) throw new IllegalStateException("Couldn't create directory to store file")
		FileCopyUtils.copy(inputStream, new FileOutputStream(target))
	}
	
	def makePermanent(file:FileAttachment) = {
		//file.temporary = false
		session.update(file)
	}
	
	def getFileById(id:String) = getById[FileAttachment](id)
	
	/** Only for use by FileAttachment to find its own backing file. */
	def getData(id:String):Option[File] = targetFile(id) match {
		case file:File if file.exists => {
			Some(file)
		}
		case _ => None
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
	
	@Transactional
	private def findOldTemporaryFiles = session.newCriteria[FileAttachment]
			.add(Is.eq("temporary", true))
			.add(Is.lt("dateUploaded", now minusDays(2)))
			.setMaxResults(TemporaryFileBatch)
			.list
	
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	private def deleteSomeFiles(files:Seq[FileAttachment]) {
		session.createQuery("delete FileAttachment f where f.id in :ids")
				.setParameterList("ids", files.map(_.id))
				.executeUpdate()
		for (file <- files) targetFile(file.id).delete()
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