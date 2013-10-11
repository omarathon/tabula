package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.FileDao
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.FileAttachment
import org.springframework.beans.factory.annotation.Autowired

trait FileAttachmentComponent {
	def fileAttachmentService: FileAttachmentService
}

trait AutowiringFileAttachmentServiceComponent extends FileAttachmentComponent {
	var fileAttachmentService = Wire[FileAttachmentService]
}

trait FileAttachmentService {
	def deleteAttachments(files: Seq[FileAttachment])
}


abstract class AbstractFileAttachmentService extends FileAttachmentService {
	@Autowired var fileDao: FileDao = _

	def deleteAttachments(files: Seq[FileAttachment]) = fileDao.deleteAttachments(files)

}

@Service("fileAttachmentService")
class FileAttachmentServiceImpl
	extends AbstractFileAttachmentService