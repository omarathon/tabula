package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.CurrentUser
import javax.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.data.model.Member
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.actions.View
import org.springframework.stereotype.Controller
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import org.springframework.util.FileCopyUtils

@Controller
@RequestMapping(value = Array("/view/photo/{member}.jpg"))
class PhotoController extends ProfilesController {

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getPhoto(@PathVariable member: Member, response: HttpServletResponse): Unit = {
		mustBeAbleTo(View(mandatory(member)))

		Option(member.photo) match {
		  	case Some(photo) => 
		  		photo.dataStream match {
		  			case null => throw new ItemNotFoundException
		  			case inStream => {
		  				// TODO We don't use fileserver here at the moment because it's for serving 
	  					// files for download. We could probably extend RenderableFile to provide options 
		  				// to not specify content-disposition, but for the moment let's just serve directly.
		  				
		  				response.addHeader("Content-Type", "image/jpeg")
		  				photo.length.map { length =>
		  					response.addHeader("Content-Length", length.toString)
			  			}
				  		FileCopyUtils.copy(inStream, response.getOutputStream)
		  			}
		  		}
		  	case None => throw new ItemNotFoundException
		}
	}

}