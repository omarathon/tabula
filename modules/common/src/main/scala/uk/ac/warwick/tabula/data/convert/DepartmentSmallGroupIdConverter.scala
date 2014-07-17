package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroup
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.system.TwoWayConverter

class DepartmentSmallGroupIdConverter extends TwoWayConverter[String, DepartmentSmallGroup] {

	@Autowired var service: SmallGroupService = _

	override def convertRight(id: String) = (Option(id) flatMap { service.getDepartmentSmallGroupById(_) }).orNull
	override def convertLeft(group: DepartmentSmallGroup) = (Option(group) map {_.id}).orNull

}
