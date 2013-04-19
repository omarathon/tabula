package uk.ac.warwick.tabula.scheduling.web.controllers.sync

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.spring.Wire
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.scheduling.services.MessageAuthenticationCodeGenerator
import uk.ac.warwick.tabula.data.FileDao
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.helpers.Logging
import org.apache.http.HttpStatus
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.ItemNotFoundException

@Controller
@RequestMapping(value = Array("/sync/getFile"))
class DownloadFileController extends BaseController with Logging {
	
	var fileServer = Wire[FileServer]
	var macGenerator = Wire[MessageAuthenticationCodeGenerator]
	var fileDao = Wire[FileDao]
	
	@RequestMapping
	def serve(@RequestParam("id") id: String, @RequestParam("mac") mac: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		if (!macGenerator.isValidSalt) {
			response.setStatus(HttpStatus.SC_BAD_REQUEST)
			logger.error("Invalid shared secret")
		} else if (macGenerator.generateMessageAuthenticationCode(id) != mac) {
			response.setStatus(HttpStatus.SC_BAD_REQUEST)
			logger.error("**** Attempt to retrieve a file with an invalid Message Authentication Code ****")
		} else {
			val file = fileDao.getFileById(id)
			file match {
				case Some(a) if a.hasData => fileServer.serve(new RenderableAttachment(a))
				case _ => throw new ItemNotFoundException()
			}
		}
	}

}

object DownloadFileController {
	val IdParam = "id"
		
	val MacParam = "mac"
}