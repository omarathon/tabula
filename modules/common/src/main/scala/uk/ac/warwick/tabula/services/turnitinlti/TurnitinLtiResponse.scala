package uk.ac.warwick.tabula.services.turnitinlti

import uk.ac.warwick.tabula.helpers.Logging
import scala.util.parsing.json.JSON
import scala.xml.Elem

/**
 * Response from the Turnitin LTI API.
 */
case class TurnitinLtiResponse(
	val success: Boolean,
	val statusMessage: Option[String] = None,
	val diagnostic: Option[String] = None,
	val redirectUrl: Option[String] = None,
	val json: Option[String] = None,
	val html: Option[String] = None,
	val xml: Option[Elem] = None) extends Logging {

	def turnitinSubmissionId(): String = {
		(xml.get \\ "lis_result_sourcedid").text
	}

	def submissionInfo() = {

		// TODO do this properly
			JSON.parseFull(json.get) match {
			case Some(theJson: Map[String, Any] @unchecked) =>
				theJson.get("outcome_originalityreport") match {
					case Some(reports: Map[String, Any] @unchecked) =>
						reports.get("breakdown") match {
							case Some(breakdowns: Map[String, Double] @unchecked) =>
								breakdowns.foreach { case (key, value) =>
									logger.info("KEY: " + key)
									logger.info("VALUE: " + value)
								}

								val submittedWorksScore = breakdowns.get("submitted_works_score")
								val internetScore = breakdowns.get("internet_score")

//								new TurnitinLtiSubmissionInfo()
//								submissionInfo.similarityScore = submittedWorksScore
//								submissionInfo.webOverlap = internetScore


								reports.get("numeric") match {
									case Some(numerics: Map[String, Double] @unchecked) =>
										numerics.foreach { case (key, value) =>
											logger.info("NUMERICS KEY: " + key)
											logger.info("NUMERICS VALUE: " + value)
										}
										val numericsScore = numerics.get("score")
										val numericsMax = numerics.get("max")
									case _ => Nil
								}
								reports.get("text") match {
									case Some(text: String) => {
										logger.info("text : " + text)
										val textScore = text
									}
									case _ => Nil
								}
							case _ => Nil
						}
					case _ => Nil
				}
			case _ => Nil
		}
	}

}

object TurnitinLtiResponse extends Logging {

	def redirect(location: String) = {
		new TurnitinLtiResponse(true, redirectUrl = Some(location))
	}

	def fromJson(json: String) = {
		logger.info("Json response: " + json)
		new TurnitinLtiResponse(true, json = Some(json))
	}

	def fromHtml(success: Boolean, html: String) = {
		logger.info("html response: " + html)
		new TurnitinLtiResponse(success, html = Some(html))
	}

	def fromXml(xml: Elem) = {
		logger.info(xml.text)
		logger.info("status: " + (xml \\ "status").text)
		logger.info("status message: " + (xml \\ "message").text)
		logger.info("submission id: " + (xml \\ "lis_result_sourcedid").text)
		new TurnitinLtiResponse((xml \\ "status").text.equals("fullsuccess"), statusMessage = Some((xml \\ "message").text), xml = Some(xml))
	}

}