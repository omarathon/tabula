package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.FileDao
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{FileAttachmentToken, FileAttachment}
import org.springframework.beans.factory.annotation.Autowired

trait FileAttachmentServiceComponent {
	def fileAttachmentService: FileAttachmentService
}

trait AutowiringFileAttachmentServiceComponent extends FileAttachmentServiceComponent {
	var fileAttachmentService = Wire[FileAttachmentService]
}

trait FileAttachmentService {
	def deleteAttachments(files: Seq[FileAttachment])
	def saveOrUpdate(attachmentToken: FileAttachmentToken): Unit
	def getValidToken(attachment: FileAttachment): Option[FileAttachmentToken]
}


abstract class AbstractFileAttachmentService extends FileAttachmentService {
	@Autowired var fileDao: FileDao = _

	def deleteAttachments(files: Seq[FileAttachment]) = fileDao.deleteAttachments(files)
	def saveOrUpdate(token: FileAttachmentToken): Unit = fileDao.saveOrUpdate(token)
	def getValidToken(attachment: FileAttachment): Option[FileAttachmentToken] = fileDao.getValidToken(attachment)
}

@Service("fileAttachmentService")
class FileAttachmentServiceImpl
	extends AbstractFileAttachmentService