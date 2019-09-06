package uk.ac.warwick.tabula.web.views

import com.fasterxml.jackson.databind.ObjectMapper
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.web.servlet.View
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.JavaImports._

import scala.util.Try

class JSONView(var json: Any) extends View {
  var objectMapper: ObjectMapper = Wire[ObjectMapper]
  var features: Features = Wire[Features]

  override def getContentType() = "application/json"

  override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setContentType(getContentType)
    val out = response.getWriter
    if (features.renderStackTracesForAllUsers) {
      objectMapper.writeValue(out, json)
    } else {
      val fullJson = Try(json.asInstanceOf[Map[String, Any]]).getOrElse(Map.empty)
      val errors = Try(fullJson.get("errors").map(_.asInstanceOf[Array[Map[String, Any]]].flatten.toMap.filterNot {
        case (key, _) => key.equalsIgnoreCase("stackTrace")
      }).getOrElse(Map.empty)).toOption

      if (errors.nonEmpty) {
        objectMapper.writeValue(out, fullJson.filterNot {
          case (key, _) => key.equalsIgnoreCase("errors")
        } ++ Map(
          "errors" -> errors
        ))
      } else objectMapper.writeValue(out, json)
    }
  }
}