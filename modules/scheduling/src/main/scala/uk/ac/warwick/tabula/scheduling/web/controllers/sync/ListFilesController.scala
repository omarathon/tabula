package uk.ac.warwick.tabula.scheduling.web.controllers.sync

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.web.views.JSONView
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.StringUtils

@Controller
@RequestMapping(value = Array("/sync/listFiles.json"))
class ListFilesController extends BaseController {
	import ListFilesController._		
	
	var fileDao = Wire[FileDao]
	
	@RequestMapping
	def list(@RequestParam("start") startDateMillis: Long, @RequestParam(value="startFromId", required=false) startingId: String) = {
		if (startDateMillis < 0) throw new IllegalArgumentException("start must be specified")
		val startDate = new DateTime(startDateMillis)
		
		val files = 
			if (startingId.hasText) fileDao.getFilesCreatedOn(startDate, MaxResponses, startingId)
			else fileDao.getFilesCreatedSince(startDate, MaxResponses)
			
		val lastFileUploadedDate = 
			if (!files.isEmpty) files.last.dateUploaded.getMillis
			else null
			
		val json = Map(
			"createdSince" -> startDate.getMillis,
			"lastFileReceived" -> lastFileUploadedDate,
			"maxResponses" -> MaxResponses,
			"files" -> (files map { 
				file => Map(
					"id" -> file.id, 
					"createdDate" -> file.dateUploaded.getMillis
				) 
			})
		)
		
		Mav(new JSONView(json))
	}

}

object ListFilesController {
	val MaxResponses = 100
	
	val StartParam = "start"
	
	val StartFromIdParam = "startFromId"
}