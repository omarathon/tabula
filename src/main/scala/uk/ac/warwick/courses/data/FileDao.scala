package uk.ac.warwick.courses.data
import uk.ac.warwick.courses.data.model.FileAttachment
import org.hibernate.Hibernate
import org.springframework.stereotype.Repository

@Repository
class FileDao extends Daoisms {
	def saveTemporary(file:FileAttachment) = {
		if (file.data == null && file.uploadedData != null) {
			file.data = session.getLobHelper.createBlob(file.uploadedData, file.uploadedDataLength)
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
}