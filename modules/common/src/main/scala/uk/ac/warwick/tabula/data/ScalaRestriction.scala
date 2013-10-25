package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Restrictions._
import scala.collection.mutable
import scala.collection.JavaConverters._

class ScalaRestriction(val criterion: Criterion) {
	val aliases: mutable.Map[String, String] = mutable.Map()
	
	def alias(property: String, alias: String) = {
		aliases.put(property, alias) match {
			case Some(other) if other != alias => 
				// non-duplicate
				throw new IllegalArgumentException("Tried to alias %s to %s, but it is already aliased to %s!".format(property, alias, other))
			case _ =>
		}
		this
	}
	
	def apply[A](c: ScalaCriteria[A]) = c.add(this)
}

object ScalaRestriction {
	private def addAliases(restriction: ScalaRestriction, aliases: (String, String)*) = {
		aliases.foreach { case (property, alias) => restriction.alias(property, alias) }
		restriction
	}
	
	def is(property: String, value: Any, aliases: (String, String)*): Option[ScalaRestriction] = 
		Some(addAliases(new ScalaRestriction(Daoisms.is(property, value)), aliases: _*))
		
	def inIfNotEmpty(property: String, collection: Iterable[Any], aliases: (String, String)*): Option[ScalaRestriction] =
		if (collection.isEmpty) None
		else Some(addAliases(new ScalaRestriction(in(property, collection.asJavaCollection)), aliases: _*))
		
	def startsWithIfNotEmpty(property: String, collection: Iterable[String], aliases: (String, String)*): Option[ScalaRestriction] =
		if (collection.isEmpty) None
		else {
			val criterion = disjunction()
			collection.foreach { prefix =>
				criterion.add(like(property, prefix + "%"))
			}
			
			Some(addAliases(new ScalaRestriction(criterion), aliases: _*))
		}
}