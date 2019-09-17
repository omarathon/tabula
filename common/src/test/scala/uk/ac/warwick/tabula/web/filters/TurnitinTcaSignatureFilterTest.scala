package uk.ac.warwick.tabula.web.filters

import org.apache.http.HttpStatus
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.turnitintca.{TurnitinTcaConfiguration, TurnitinTcaConfigurationComponent}

class TurnitinTcaSignatureFilterTest extends TestBase {

  trait MockTurnitinTcaConfigurationComponent extends TurnitinTcaConfigurationComponent {
    override lazy val tcaConfiguration: TurnitinTcaConfiguration = TurnitinTcaConfiguration("", "", "", "FPWkCqcOxHSEQID13u89mf0/etK3kxQZ1iVvCutAGBc=")
  }

  val filter = new TurnitinTcaSignatureFilter with MockTurnitinTcaConfigurationComponent
  val reqJson = """{"owner":"u1431777","title":"IN-101 Final Esssay","status":"COMPLETE","id":"75380a71-b0be-40df-8c34-981e2bb3a5b0","content_type":"application/vnd.openxmlformats-officedocument.wordprocessingml.document","page_count":1,"word_count":635,"character_count":3124,"created_time":"2019-09-16T15:09:25.206Z","capabilities":["INDEX","SIMILARITY","VIEWER"],"metadata":{"custom":"{\"submission_id\":\"4204e052-a68f-41d1-bcaf-48383087f7d6\", \"file_attachment_id\":\"2ef43e6e-7587-44a1-b079-8ac47c453a32\"}"}}"""

  @Test
  def correctSignature(): Unit = {
    val req = new MockHttpServletRequest
    val resp = new MockHttpServletResponse
    val chain = new MockFilterChain

    req.setRequestURI("/turnitin/tca/webhooks/submission-complete")
    req.setContent(reqJson.getBytes)
    req.addHeader("X-Turnitin-Signature", "167e4183b6a786e07e69f24d2554235b17dc08031620cda5e0b12e3b8b1a07a8")

    filter.doFilter(req, resp, chain)
    resp.getStatus should be (HttpStatus.SC_OK)

  }

  @Test
  def incorrectSignature(): Unit = {
    val req = new MockHttpServletRequest
    val resp = new MockHttpServletResponse
    val chain = new MockFilterChain

    req.setRequestURI("/turnitin/tca/webhooks/submission-complete")
    req.setContent(reqJson.getBytes)
    req.addHeader("X-Turnitin-Signature", "heronsHaveInterceptedThisRequestAndAreTryingToOpenAPortalToHades")

    filter.doFilter(req, resp, chain)
    resp.getStatus should be (HttpStatus.SC_UNAUTHORIZED)
  }
}
