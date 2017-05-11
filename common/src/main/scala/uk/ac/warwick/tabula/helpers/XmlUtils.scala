package uk.ac.warwick.tabula.helpers

import org.joda.time.ReadableInstant
import uk.ac.warwick.tabula.DateFormats

import scala.xml.{Elem, Null, UnprefixedAttribute}

trait XmlUtils {
	// Pimp XML elements to allow a map mutator for attributes
	implicit class PimpedElement(elem: Elem) {
		def %(attrs: Map[String, Any]): Elem = {
			val seq = attrs.filter { case (_, v) => v != null }.map { case (k, v) =>
				new UnprefixedAttribute(k, toXMLString(Some(v)), Null)
			}
			(elem /: seq) (_ % _)
		}
	}

	def toXMLString(value: Option[Any]): String = value match {
		case Some(o: Option[Any]) => toXMLString(o)
		case Some(i: ReadableInstant) => isoFormat(i)
		case Some(b: Boolean) => b.toString.toLowerCase
		case Some(i: Int) => i.toString
		case Some(s: String) => s
		case Some(other) => other.toString
		case None => ""
	}

	val isoFormatter = DateFormats.IsoDateTime
	def isoFormat(i: ReadableInstant): String = isoFormatter print i
}

object XmlUtils extends XmlUtils