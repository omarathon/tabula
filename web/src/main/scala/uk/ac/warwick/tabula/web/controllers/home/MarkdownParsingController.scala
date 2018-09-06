package uk.ac.warwick.tabula.web.controllers.home

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.{JSONView, MarkdownRenderer, MarkdownRendererImpl}


@Controller
class MarkdownParsingController extends BaseController with MarkdownRendererImpl {

	self: MarkdownRenderer =>

	@RequestMapping(value = Array("/markdown/toHtml"), method = Array(POST), produces = Array("application/json"))
	def toHtml(@RequestParam markdownString: String, user: CurrentUser): Mav = {
		if (user.exists) {
			Mav(new JSONView(Map("html" -> renderMarkdown(markdownString))))
		} else {
			Mav(new JSONView(Map.empty))
		}
	}
}
