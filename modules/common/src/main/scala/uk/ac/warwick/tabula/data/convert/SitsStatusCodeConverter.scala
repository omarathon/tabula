package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.SitsStatusDao

class SitsStatusCodeConverter extends TwoWayConverter[String, SitsStatus] {

	@Autowired var dao: SitsStatusDao = _

	override def convertRight(code: String) = (Option(code) flatMap { dao.getByCode(_) }).orNull
	override def convertLeft(moa: SitsStatus) = (Option(moa) map {_.code}).orNull
	
}