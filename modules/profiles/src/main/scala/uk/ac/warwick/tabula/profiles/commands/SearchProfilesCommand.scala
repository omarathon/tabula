package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.commands.Command
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.commands.Description

class SearchProfilesCommand extends Command[List[Member]] {
	
	@BeanProperty var query: String = _
	
	override def applyInternal() = {
		//TODO
		List()
	}
	
	override def describe(d: Description) = d.property("query" -> query)

}