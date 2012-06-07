package uk.ac.warwick.courses.services.turnitin

import org.apache.commons.codec.digest.DigestUtils

/**
 * There should be a functional test for the Turnitin
 * support but in the meantime I'm just running this
 * little app manually to find out how it works. 
 */
object TryTurnitin extends App {
	
	//12a4e7b0bfc5f55b4b1ef252b1b05919
	println(DigestUtils.md5Hex("100" +
			"0" +
			"0" +
			"1" +
			"1" +
			"200310311" +
			"john.doe@myschool.edu" +
			"JohnDoe" +
			"john123" +
			"2" +
			"hothouse123"))
	
	val api = new Turnitin
	api.diagnostic = true
	api.submitPaper
	
}