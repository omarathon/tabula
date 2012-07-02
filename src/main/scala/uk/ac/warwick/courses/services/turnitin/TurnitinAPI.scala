package uk.ac.warwick.courses.services.turnitin

import java.io.File

trait TurnitinAPI {
	def createClass(className:String): Response
	def createAssignment(className:String, assignmentName:String, update:Boolean=false): Response
	def deleteAssignment(className:String, assignmentName:String): Response
	def submitPaper(className:String, assignmentName:String, paperTitle:String, file:File, authorFirstName:String, authorLastName:String): Response
	def getReport(paperId:String): Response
	def listSubmissions(className:String, assignmentName:String): Response
	/** 
	 * Delete submission from assignment. Not fully synchronous, i.e. if you list submissions straight after
	 * deletion it may still list the submission as present.
	 */
	def deleteSubmission(className:String, assignmentName:String, oid:String): Response
}