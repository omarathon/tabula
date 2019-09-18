package uk.ac.warwick.tabula.web.controllers.turnitin.tca


import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestBody, RequestMapping}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.turnitintca.AutowiringTurnitinTcaServiceComponent
import uk.ac.warwick.tabula.services.turnitintca.TcaSubmission
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Profile(Array("turnitinTca"))
@Controller
@RequestMapping(Array("/turnitin/tca/webhooks"))
class TurnitinTcaWebhookController extends BaseController with Logging with AutowiringTurnitinTcaServiceComponent  {

  @RequestMapping(method = Array(POST), value = Array("/submission-complete"))
  def submissionComplete(@RequestBody json: TcaSubmission): Mav = {
    logger.debug(s"turnitin TCA - SUBMISSION_COMPLETE callback\n$json")
    turnitinTcaService.requestSimilarityReport(json)
    Mav.empty()
  }

  @RequestMapping(method = Array(POST), value = Array("/similarity"))
  def similarityComplete(@RequestBody json: String): Mav = {
    logger.debug(s"turnitin TCA - SIMILARITY_COMPLETE callback\n$json")
    Mav.empty()
  }
}


