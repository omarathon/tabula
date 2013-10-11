package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.FileAttachment

trait GroupsObjects[A >: Null, B >: Null] extends BindListener {
	
	/** Mapping from B to an ArrayList of As. */	
	var mapping: JMap[B, JList[A]] =
		LazyMaps.create { key: B => JArrayList(): JList[A] }.asJava
	
	var unallocated: JList[A] = LazyLists.create { () => null } // grower, not a shower
		
	var file: UploadedFile = new UploadedFile

	private def filenameOf(path: String) = new java.io.File(path).getName
		
	def populate(): Unit
	def sort(): Unit
	
	def mappingById: Map[String, JList[A]]
	
	def validateUploadedFile(result: BindingResult): Unit
	def extractDataFromFile(file: FileAttachment, result: BindingResult): Map[B, JList[A]]
	
	override def onBind(result: BindingResult) {
		validateUploadedFile(result)
		
		if (!result.hasErrors) {
			transactional() {
				file.onBind(result)
				if (!file.attached.isEmpty()) {
					processFiles(file.attached.asScala)
				}
	
				def processFiles(files: Seq[FileAttachment]) {
					val data = files.filter(_.hasData).flatMap { extractDataFromFile(_, result) }.toMap
					
					mapping.clear()
					mapping.putAll( data.asJava )
				}
			}
		}
	}

}