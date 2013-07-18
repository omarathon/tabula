package uk.ac.warwick.tabula.web.controllers

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class AppVersionController {

	@Value("${build.time}") var buildTime: String = _
  @Value("${git.commit}") var gitSha:String= _
	@Value("${git.describe}") var gitDesc:String = _

	@RequestMapping(Array("/api/version"))
	def showTime = new ResponseEntity("%s - %s - %s".format(buildTime, gitSha, gitDesc), plainTextHeaders, HttpStatus.OK)

	def plainTextHeaders = new HttpHeaders {
		setContentType(MediaType.TEXT_PLAIN)
	}

}