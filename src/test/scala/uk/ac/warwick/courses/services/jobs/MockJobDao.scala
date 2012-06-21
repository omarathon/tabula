package uk.ac.warwick.courses.services.jobs

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
		instance.id
	}
	
	def getById(id:String) = instances.find( id == _.id )
	
	def clear = instances.clear

}