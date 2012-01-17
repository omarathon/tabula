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

@Repository
class FileDao extends Daoisms with InitializingBean {
	
	@Value("${filesystem.attachment.dir}") var attachmentDir:File =_
	@Value("${filesystem.create.missing}") var createMissingDirectories:Boolean =_
	
	val idSplitSize = 4
	
	private def partition(id:String): String = id.replace("-","").grouped(idSplitSize).mkString("/")
	
	private def targetFile(id:String): File = new File(attachmentDir, partition(id)) 
	
	def saveTemporary(file:FileAttachment) :Unit = {
		session.saveOrUpdate(file)
		if (!file.hasData && file.uploadedData != null) {
			val target = targetFile(file.id)
			val directory = target.getParentFile()
			directory.mkdirs()
			if (!directory.exists) throw new IllegalStateException("Couldn't create directory to store file")
			FileCopyUtils.copy(file.uploadedData, new FileOutputStream(target))
		}
		
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
	 * Delete any temporary blobs that are more than a day old.
	 */
	def deleteOldTemporaryFiles = session.createSQLQuery("delete from fileattachment where temporary=1 and dateupload < :old")
		.setTimestamp("old", now minusDays(1) toDate)
		.executeUpdate
	
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