package uk.ac.warwick.tabula.web.views

import freemarker.template.TemplateScalarModel
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.tabula.data.model.Assignment

class AssignmentCloseTimes extends TemplateScalarModel {
  private val formatter = DateTimeFormat.forPattern("ha")
  def getAsString: String = s"${formatter.print(Assignment.CloseTimeStart).toLowerCase} and ${formatter.print(Assignment.CloseTimeEnd).toLowerCase}"
}