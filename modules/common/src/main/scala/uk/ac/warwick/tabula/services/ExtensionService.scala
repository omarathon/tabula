package uk.ac.warwick.tabula.services

import scala.collection.JavaConversions._

import org.hibernate.annotations.AccessType
import org.hibernate.criterion.{Projections, Restrictions, Order}
import org.springframework.stereotype.Service

import javax.persistence.Entity
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._

trait ExtensionService {
	def getExtensionById(id: String): Option[Extension]
}

@Service(value = "extensionService")
class ExtensionServiceImpl extends ExtensionService with Daoisms with Logging {
	import Restrictions._	

	def getExtensionById(id: String) = getById[Extension](id)
}