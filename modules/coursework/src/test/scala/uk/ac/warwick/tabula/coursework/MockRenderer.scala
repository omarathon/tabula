package uk.ac.warwick.tabula.coursework

import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.Mockito

trait MockRenderer extends TextRenderer with Mockito {
	val mockRenderer = mock[TextRenderer]
	def renderTemplate(id:String,model:Any ):String = {
		mockRenderer.renderTemplate(id, model)
	}
}
