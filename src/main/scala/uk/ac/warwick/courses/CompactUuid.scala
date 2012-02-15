package uk.ac.warwick.courses

import java.math.BigInteger
import scala.util.matching.Regex
import org.apache.commons.codec.binary.Base64

/**
 * Unused
 */
object CompactUuid {
	
	val UuidSplit = new Regex("(.{8})(.{4})(.{4})(.{4})(.{12})")
	
	def uncompact(compactUuid:String) : Option[String] = 
		try {
			decode64(compactUuid).toString(16) match {
				case UuidSplit(a,b,c,d,e) => Some("%s-%s-%s-%s-%s".format(a,b,c,d,e))
				case _ => None
			}
		} catch {
			case e:NumberFormatException => None
		}
	
	
	def compact(uuid:String) : Option[String] = 
		try {
			Some(encode64(new BigInteger(uuid.replace("-",""), 16)))
		} catch {
			case e:NumberFormatException => None
		}
	
	private def encode64(bigint:BigInteger) = 
		new String(Base64.encodeBase64(bigint.toByteArray)).replace("=","")
	
	private def decode64(string:String) =
		new BigInteger(Base64.decodeBase64(string))

}