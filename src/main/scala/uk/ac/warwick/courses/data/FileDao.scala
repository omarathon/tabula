package uk.ac.warwick.courses.data
import uk.ac.warwick.courses.data.model.FileAttachment
import org.hibernate.Hibernate
import org.springframework.stereotype.Repository
import java.io.BufferedInputStream
import org.joda.time.DateTime
import org.joda.time.DateTime.now
import org.joda.time.ReadableInstant

@Repository
class FileDao extends Daoisms {
	def saveTemporary(file:FileAttachment) = {
		if (file.data == null && file.uploadedData != null) {
			file.data = session.getLobHelper.createBlob(
					new BufferedInputStream(file.uploadedData, 1024*64), // 64k buffer 
					file.uploadedDataLength)
		}
		session.saveOrUpdate(file)
		// HFC-38 Trying to induce Hibernate to not explode with BLOBS
		session.flush
		session.refresh(file)
	}
	
	def makePermanent(file:FileAttachment) = {
		//file.temporary = false
		session.update(file)
	}
	
	def getFileById(id:String) = getById[FileAttachment](id)
	
	/**
	 * Delete any temporary blobs that are more than a day old.
	 */
	def deleteOldTemporaryFiles = session.createSQLQuery("delete from fileattachment where temporary=1 and dateupload < :old")
		.setTimestamp("old", now minusDays(1) toDate)
		.executeUpdate
		
}