package uk.ac.warwick.tabula.api.web.helpers

import scala.collection.immutable.TreeMap

case class APIFieldRestriction(name: String, fields: Map[String, APIFieldRestriction] = Map()) {
	def restrict[A](key: String)(fn: => Option[A]): Option[A] =
		if (isAllowed(key)) fn
		else None

	def isAllowed(key: String): Boolean =
		fields.isEmpty || (key.split("\\.", 2) match {
			case Array(prefix, suffix) => fields.get(prefix).exists(_.isAllowed(suffix))
			case Array(prefix) => fields.contains(prefix)
		})

	def nested(key: String): Option[APIFieldRestriction] =
		if (fields.isEmpty) Some(APIFieldRestriction(key))
		else key.split("\\.", 2) match {
			case Array(prefix, suffix) => fields.get(prefix).flatMap(_.nested(suffix))
			case Array(prefix) => fields.get(prefix)
		}
}

object APIFieldRestriction {
	private def caseInsensitiveMap: TreeMap[String, APIFieldRestriction] = TreeMap[String, APIFieldRestriction]()(Ordering.by(_.toLowerCase))

	def parse(in: String): Map[String, APIFieldRestriction] =
		caseInsensitiveMap ++
			in.split(',')
				.filterNot(_.isEmpty)
				.map { key =>
					key.split("\\.", 2) match {
						case Array(prefix, suffix) => prefix -> Some(suffix)
						case Array(prefix) => prefix -> None
					}
				}
				.groupBy { case (p, _) => p }
				.map { case (p, v) => p -> v.flatMap(_._2).toSeq }
				.map { case (p, v) => p -> APIFieldRestriction(p, parse(v.mkString(","))) }

	def restriction(key: String, in: String): APIFieldRestriction =
		APIFieldRestriction.parse(in) match {
			case fr if fr.contains(key) => fr(key)
			case fr => APIFieldRestriction(key, fr)
		}
}