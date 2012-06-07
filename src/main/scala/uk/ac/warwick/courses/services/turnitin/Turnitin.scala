package uk.ac.warwick.courses.services.turnitin

import uk.ac.warwick.courses.helpers.Logging
import dispatch._
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.DateTimeFormatter
import org.joda.time.DateTime
import scala.collection.immutable.SortedMap
import org.apache.commons.codec.digest.DigestUtils

sealed abstract class FunctionId(val id: Int, val description: String)
case class CreateAssignmentFunction() extends FunctionId(4, "Create assignment")
case class SubmitPaperFunction() extends FunctionId(5, "Submit paper")
case class GenerateReportFunction() extends FunctionId(6, "Generate report")

class Turnitin extends Logging {
	val endpoint = :/ ("submit.ac.uk") / "api.asp" secure
	def aid = "299"
	def said = "8432"
	var sharedSecretKey = "this is not the shared key"
	var diagnostic = false

	def submitPaper = {
		doRequest(SubmitPaperFunction())
	}

	private def doRequest(f: FunctionId) {
		val parameters = Map("fid" -> f.id.toString) ++ commonParameters
		val req = endpoint.POST << parameters << Map("md5" -> md5(parameters))
		Http(req >>> Console.err)
	}
	
	/**
	 * Parameters that we need in every request.
	 */
	def commonParameters = Map(
		"diagnostic" -> (if (diagnostic) "1" else "0"),
		"gmtime" -> gmtTimestamp,
		"encrypt" -> "0",
		"aid" -> aid,
		"said" -> said,
		"fcmd" -> "2",
		"uem" -> "n.howes@warwick.ac.uk",
		"ufn" -> "Nick",
		"uln" -> "Howes",
		"utp" -> "2"
	)
	
	/**
	 * Sort parameters by key, concatenate all the values with
	 * the shared key and MD5hex that.
	 */
	def md5(map:Map[String,String]) = {
		println(map.toSeq sortBy mapKey map mapKey mkString(" + "))
		println((map.toSeq sortBy mapKey map mapValue mkString("")) + sharedSecretKey)
		DigestUtils.md5Hex(
			(map.toSeq sortBy mapKey map mapValue mkString("")) + sharedSecretKey
		)
	}
	
	val timestampFormat = DateTimeFormat.forPattern("YYYYMMddHHm").withZoneUTC
	def gmtTimestamp = timestampFormat print DateTime.now
	
	private def mapKey[K](pair:Pair[K,_]) = pair._1
	private def mapValue[V](pair:Pair[_,V]) = pair._2 
}