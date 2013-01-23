package uk.ac.warwick.tabula.dev.web.controllers

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.dev.web.commands.FixturesCommand

@Controller
@RequestMapping(Array("/fixtures/setup"))
class FixturesController {
	
	@RequestMapping
	def apply(cmd: FixturesCommand):Unit = cmd.apply() 

}