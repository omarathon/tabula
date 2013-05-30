package uk.ac.warwick.tabula.services

import org.hibernate.criterion.{Projections, Restrictions, Order}
import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.Logging

trait SmallGroupService {
	def getSmallGroupSetById(id: String): Option[SmallGroupSet]
	def getSmallGroupById(id: String): Option[SmallGroup]
	def getSmallGroupEventById(id: String): Option[SmallGroupEvent]
	def saveOrUpdate(smallGroupSet: SmallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent)
}

@Service(value = "smallGroupService")
class SmallGroupServiceImpl 
	extends SmallGroupService
		with Daoisms 
		with Logging {
	import Restrictions._
	
	def getSmallGroupSetById(id: String) = getById[SmallGroupSet](id)
	def getSmallGroupById(id: String) = getById[SmallGroup](id)
	def getSmallGroupEventById(id: String) = getById[SmallGroupEvent](id)
	
	def saveOrUpdate(smallGroupSet: SmallGroupSet) = session.saveOrUpdate(smallGroupSet) 
	def saveOrUpdate(smallGroup: SmallGroup) = session.saveOrUpdate(smallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent) = session.saveOrUpdate(smallGroupEvent)

}