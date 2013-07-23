package uk.ac.warwick.tabula.data.model.groups

import javax.persistence.{ManyToOne, Entity}
import org.hibernate.annotations.AccessType
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@AccessType("field")
@Entity
class SmallGroupEventOccurrence extends GeneratedId with PermissionsTarget with Serializable {
	type WeekNumber = Int

	@ManyToOne
	var smallGroupEvent: SmallGroupEvent = _

	var week: WeekNumber = _

	def permissionsParents = Stream(smallGroupEvent)

}
