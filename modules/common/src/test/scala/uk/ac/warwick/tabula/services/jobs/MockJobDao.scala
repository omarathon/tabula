package uk.ac.warwick.tabula.services.jobs

import scala.collection.Seq
import scala.collection.mutable.ArrayBuffer
import java.util.UUID

class MockJobDao extends JobDao {

	val instances = ArrayBuffer[JobInstance]()
	
	def findOutstandingInstances(max: Int) = {
		instances.iterator.filterNot( _.started ).take(max).toSeq
	}

	def saveJob(instance: JobInstance) = {
		instance.id = UUID.randomUUID.toString
		instances += instance
		instance
	}
	
	def unfinishedInstances = instances.toSeq.filterNot( _.finished )
	
	def listRecent(start: Int, count: Int) = instances.toSeq.filter( _.finished ).slice(start, start+count)
	
	def getById(id:String) = instances.find( id == _.id )
	
	def clear = instances.clear
	
	def update(instance: JobInstance) {}

}