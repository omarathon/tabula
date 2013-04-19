package uk.ac.warwick.tabula.web.controllers

import uk.ac.warwick.spring.Wire

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class AppVersionController {

	var buildTime = Wire[String]("${build.time}")

	@RequestMapping(Array("/api/version"))
	def showTime = new ResponseEntity(buildTime, plainTextHeaders, HttpStatus.OK)

	def plainTextHeaders = new HttpHeaders {
		setContentType(MediaType.TEXT_PLAIN)
	}

}