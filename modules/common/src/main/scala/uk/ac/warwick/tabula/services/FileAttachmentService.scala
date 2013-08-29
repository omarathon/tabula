package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.FileAttachment
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.helpers.Logging
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.spring.Wire

trait FileAttachmentService {
	def delete(attachment: FileAttachment)
}

@Service(value = "fileAttachmentService")
class FileAttachmentServiceImpl extends FileAttachmentService with Logging {
	@Autowired var dao: FileDao = _

	def delete(attachment: FileAttachment) = dao.deleteFile(attachment)
}

trait FileAttachmentServiceComponent {
	def fileAttachmentService: FileAttachmentService
}

trait AutowiringFileAttachmentServiceComponent extends FileAttachmentServiceComponent {
	var fileAttachmentService = Wire[FileAttachmentService]
}
