package uk.ac.warwick.courses.services.turnitin

import uk.ac.warwick.courses.helpers.Logging
import dispatch._
import dispatch.mime.Mime._
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.DateTimeFormatter
import org.joda.time.DateTime
import org.apache.commons.codec.digest.DigestUtils
import scala.xml.NodeSeq
import scala.xml.Elem
import java.io.File
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import java.io.FileInputStream
import uk.ac.warwick.courses.data.model.FileAttachment
import org.apache.commons.io.FilenameUtils

case class FileData(val file:File, val name:String)

object Turnitin {
	/** Quoted supported types are...
	  * "MS Word, Acrobat PDF, Postscript, Text, HTML, WordPerfect (WPD) and Rich Text Format". */
	val validExtensions = Seq("doc","docx","pdf","rtf","txt","wpd","htm","html","ps")
	
	def validFileType(file: FileAttachment): Boolean = 
		Turnitin.validExtensions contains FilenameUtils.getExtension(file.name).toLowerCase
}

/**
 * Service for accessing the Turnitin plagiarism API.
 * 
 * The methods you call to do stuff are defined in [[TurnitinMethods]].
 */
@Service
class Turnitin extends TurnitinMethods with Logging with DisposableBean with InitializingBean {
	
	import TurnitinDates._
	
	/** The top level account ID (usually for University of Warwick account) */
	@Value("${turnitin.aid}") var aid: String = null
	/** Sub-account ID underneath University of Warwick */
	@Value("${turnitin.said}") var said: String = null
	/** Shared key as set up on the University of Warwick account's Open API settings */
	@Value("${turnitin.key}") var sharedSecretKey: String = null
	
	/** If this is set to true, responses are returned with HTML debug info,
	  * and also it doesn't make any changes - the server just lets you know whether
	  * your request looks okay.
	  */
	var diagnostic = false
	
	val userAgent = "Coursework submission app, University of Warwick, coursework@warwick.ac.uk"
	
	/** URL to call for all requests. _could_ make it configurable, I suppoooosse. */
	val endpoint = url("https://submit.ac.uk/api.asp") <:< Map("User-Agent" -> userAgent)
	
	private val http = new Http with thread.Safety
	
	val excludeFromMd5 = Seq("dtend")

		
	/** All API requests call the same URL and require the same MD5
	  * signature parameter.
	  * 
	  * If you start getting an "MD5 NOT AUTHENTICATED" on an API method you've
	  * changed, it's usually because it doesn't recognise one of the parameters.
	  * We MD5 on all parameters but the server will only MD5 on the parameters
	  * it recognises, hence the discrepency.
	  */
	override def doRequest
			(functionId: String, // API function ID
			pdata: Option[FileData], // optional file to put in "pdata" parameter
			params: Pair[String, String]*) // POST parameters
			: TurnitinResponse = {
		
		val parameters = Map("fid" -> functionId) ++ commonParameters ++ params
		val postWithParams = endpoint.POST << (parameters + md5hexparam(parameters))
		val req = addPdata(pdata, postWithParams)
			
		http(
			if (diagnostic) req >- {(text) => TurnitinResponse.fromDiagnostic(text)}
			else req <> { (node) => TurnitinResponse.fromXml(node) } )
	}
	
	/**
	 * Returns either the request with a file added on, or the original
	 * request if there's no file to add.
	 */
	def addPdata(file:Option[FileData], req:Request) = 	
		file map ( d => 
			req <<* ("pdata", d.name, { () => new FileInputStream(d.file) }) 
		) getOrElse req
	
	/**
	 * Parameters that we need in every request.
	 */
	def commonParameters = Map(
		"diagnostic" -> (if (diagnostic) "1" else "0"),
		"gmtime" -> gmtTimestamp,
		"encrypt" -> "0",
		"aid" -> aid,
		"fcmd" -> "2",
		"uem" -> "coursework@warwick.ac.uk",
		"ufn" -> "Coursework",
		"uln" -> "App",
		"utp" -> "2"
	) ++ (subAccountParameter)
	
	
	private def subAccountParameter: Map[String,String] =
		if (said == null || said.isEmpty) 
			Map.empty
		else 
			Map("said" -> said)
			
	
	def md5hexparam(map:Map[String,String]) = ("md5" -> md5hex(map))
	
	/**
	 * Sort parameters by key, concatenate all the values with
	 * the shared key and MD5hex that.
	 */
	def md5hex(map:Map[String,String]) = {
		DigestUtils.md5Hex(
			((map filterKeys includeInMd5).toSeq sortBy mapKey map mapValue mkString("")) + sharedSecretKey
		)
	}
	
	def includeInMd5(key:String) = !excludeFromMd5.contains(key) 
	
	private def mapKey[K](pair:Pair[K,_]) = pair._1
	private def mapValue[V](pair:Pair[_,V]) = pair._2 
	
	override def destroy {
		http.shutdown()
	}
	
	override def afterPropertiesSet {
		
	}
}