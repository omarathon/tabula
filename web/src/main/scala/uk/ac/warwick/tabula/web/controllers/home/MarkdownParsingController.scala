package uk.ac.warwick.tabula.web.controllers.home

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, ResponseBody}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.{MarkdownRenderer, MarkdownRendererImpl}


@Controller
class MarkdownParsingController extends BaseController with MarkdownRendererImpl {

	self: MarkdownRenderer =>

	@RequestMapping(value = Array("/markdown/toHtml"), method = Array(POST), produces = Array("text/html"))
	@ResponseBody
	def toHtml(@RequestParam markdownString: String, user: CurrentUser): String = {
		if (user.exists) renderMarkdown(markdownString) else ""
	}
}
