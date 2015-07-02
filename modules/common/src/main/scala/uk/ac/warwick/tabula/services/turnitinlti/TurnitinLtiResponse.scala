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


	/**
	 * Submission details is expected in the following format:
	 *
	{
   "outcome_originalfile":{
      "roles":[
         "Learner",
         "Instructor"
      ],
      "text":null,
      "launch_url":"https://sandbox.turnitin.com/api/lti/1p0/download/orig/200503253?lang=en_us",
      "label":"Download File in Original Format"
   },
   "outcome_grademark":{
      "roles":[
         "Instructor"
      ],
      "numeric":{
         "score":null,
         "max":null
      },
      "text":"--",
      "launch_url":"https://sandbox.turnitin.com/api/lti/1p0/dv/grademark/200503253?lang=en_us",
      "label":"Open GradeMark"
   },
   "outcome_pdffile":{
      "roles":[
         "Learner",
         "Instructor"
      ],
      "text":null,
      "launch_url":"https://sandbox.turnitin.com/api/lti/1p0/download/pdf/200503253?lang=en_us",
      "label":"Download File in PDF Format"
   },
   "outcome_originalityreport":{
      "text":"100%",
      "roles":[
         "Instructor"
      ],
      "numeric":{
         "score":100,
         "max":100
      },
      "label":"Open Originality Report",
      "launch_url":"https://sandbox.turnitin.com/api/lti/1p0/dv/report/200503253?lang=en_us",
      "breakdown":{
         "publications_score":35,
         "internet_score":100,
         "submitted_works_score":100
      }
   },
   "outcome_resubmit":{
      "text":null,
      "roles":[
         "Learner"
      ],
      "label":"Resubmit File",
      "launch_url":"https://sandbox.turnitin.com/api/lti/1p0/upload/resubmit/200503253?lang=en_us"
   }
}
	 *
	 */
	def submissionInfo() = {

		// TODO do this properly
			JSON.parseFull(json.get) match {
			case Some(theJson: Map[String, Any] @unchecked) =>
				theJson.get("outcome_originalityreport") match {
					case Some(reports: Map[String, Any] @unchecked) =>
						reports.get("breakdown") match {
							case Some(breakdowns: Map[String, Double] @unchecked) =>
								breakdowns.foreach { case (key, value) =>
									debug("KEY: " + key)
									debug("VALUE: " + value)
								}
								reports.get("numeric") match {
									case Some(numerics: Map[String, Double] @unchecked) =>
										numerics.foreach { case (key, value) =>
											debug("NUMERICS KEY: " + key)
											debug("NUMERICS VALUE: " + value)
										}
									case _ => Nil
								}
								reports.get("text") match {
									case Some(text: String) => {
										debug("text : " + text)
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
		new TurnitinLtiResponse((xml \\ "status").text.equals("fullsuccess"), statusMessage = Some((xml \\ "message").text), xml = Some(xml))
	}

}