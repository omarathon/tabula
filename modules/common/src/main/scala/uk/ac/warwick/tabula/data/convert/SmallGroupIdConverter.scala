package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}

class SmallGroupIdConverter extends TwoWayConverter[String, SmallGroup] {

	@Autowired var service: SmallGroupService = _

	override def convertRight(id: String) = (Option(id) flatMap { service.getSmallGroupById(_) }).orNull
	override def convertLeft(set: SmallGroup) = (Option(set) map {_.id}).orNull

}