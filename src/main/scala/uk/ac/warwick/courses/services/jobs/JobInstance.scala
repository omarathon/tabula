package uk.ac.warwick.courses.services.jobs

import collection.mutable
import collection.JavaConversions._
import uk.ac.warwick.courses.JavaImports._

/**
 * Interface for a Job to update its status in the database.
 * 
 * The main implementation of this is JobDefinition which is
 * stores in the database.
 */
trait JobInstance {
	protected def propsMap: mutable.Map[String,Any]
	
	def jobType: String
	
	var id: String
	
	def getString(name:String) = propsMap.get(name).toString
	def setString(name:String, value:String) = propsMap.put(name, value)
	
	def getStrings(name:String) = propsMap.get(name).asInstanceOf[JList[String]].toSeq
	def setStrings(name:String, value:Seq[String]) = propsMap.put(name, value:JList[String])
	
	var status: String
	var progress: Int
	
	var started: Boolean
	var finished: Boolean
	var succeeded: Boolean
}