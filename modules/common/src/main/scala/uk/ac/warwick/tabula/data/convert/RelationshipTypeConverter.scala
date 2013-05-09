package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.MeetingFormat
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.RelationshipType

class RelationshipTypeConverter extends TwoWayConverter[String, RelationshipType] {

	override def convertRight(code: String) = RelationshipType.fromCode(code)
	override def convertLeft(relType: RelationshipType) = (Option(relType) map { _.dbValue }).orNull
	
}