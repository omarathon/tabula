package uk.ac.warwick.tabula.web.views

import org.junit.Before
import org.springframework.mock.web.MockHttpServletRequest
import uk.ac.warwick.tabula.{CurrentUser, TestBase}
import org.springframework.mock.web.MockHttpServletResponse
import uk.ac.warwick.userlookup.User

class JSONViewTest extends TestBase {

  @Test def itWorks() {
    val req = new MockHttpServletRequest
    val res = new MockHttpServletResponse

    val view = new JSONView(Map("yes" -> "no", "bool" -> true, "seq" -> Seq("yes", "no")))
    view.objectMapper = json
    view.features = emptyFeatures
    view.render(null, req, res)

    res.getContentType should be("application/json")
    res.getContentAsString should be("""{"yes":"no","bool":true,"seq":["yes","no"]}""")
  }

  @Test def notRenderStackTraceIfFeaturedOff() {
    val req = new MockHttpServletRequest
    val res = new MockHttpServletResponse

    val view = new JSONView(Map(
      "yes" -> "no",
      "bool" -> true,
      "seq" -> Seq("yes", "no"),
      "errors" -> Array(
        Map(
          "message" -> "wrong",
          "stacktrace" -> "not this"
        )
      ),
    ))
    view.objectMapper = json
    view.features = emptyFeatures
    view.features.renderStackTracesForAllUsers = false

    view.render(null, req, res)

    res.getContentType should be("application/json")
    res.getContentAsString should be("""{"yes":"no","bool":true,"seq":["yes","no"],"errors":{"message":"wrong"}}""")
  }

  @Test def renderStackTraceIfFeaturedOn() {
    val req = new MockHttpServletRequest
    val res = new MockHttpServletResponse

    val view = new JSONView(Map(
      "yes" -> "no",
      "bool" -> true,
      "seq" -> Seq("yes", "no"),
      "errors" -> Array(
        Map(
          "message" -> "wrong",
          "stacktrace" -> "not this"
        )
      ),
    ))
    view.objectMapper = json
    view.features = emptyFeatures
    view.features.renderStackTracesForAllUsers = true

    view.render(null, req, res)

    res.getContentType should be("application/json")
    res.getContentAsString should be("""{"yes":"no","bool":true,"seq":["yes","no"],"errors":[{"message":"wrong","stacktrace":"not this"}]}""")
  }

}