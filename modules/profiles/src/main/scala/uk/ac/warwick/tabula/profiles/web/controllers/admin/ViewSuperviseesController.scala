package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.profiles.commands.ViewRelatedStudentsCommand
import uk.ac.warwick.tabula.data.model.{StudentRelationship, RelationshipType}
import scala.Array
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.web.Mav

/**
 * This seems like it would be an obvious candidate for rmerging with ViewPersonalTuteesController.
 *
 * But if we did that, we'd have to change the URL, so that we had a common base to start matching from (unless
 * spring supports paths like "tutees|supervisees". We'd also have to manually match the URL string to find the right
 * RelationshipType (unless spring supports a path expression something like "{relationship:IN(tutees|supervisees)}
 *
 * Given that the two classes are barely anything more than configuration, it doesn't seem worth the bother to try
 * and merge them. If you decide to give it a go, start by making the URL be /relatedby/{tutor|supervisor} and binding
 * the second part into a RelationshipType
 */
@Controller
@RequestMapping(value = Array("/supervisees"))
class ViewSuperviseesController extends ProfilesController {
	@ModelAttribute("cmd") def command = ViewRelatedStudentsCommand(currentMember, RelationshipType.Supervisor)

	@RequestMapping(method = Array(HEAD, GET))
	def view(@ModelAttribute("cmd") cmd: Appliable[Seq[StudentRelationship]]): Mav = {
		Mav("supervisors/supervisee_view", "supervisees" -> cmd.apply)
	}
}