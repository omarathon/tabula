package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Restrictions._
import scala.collection.mutable
import scala.collection.JavaConverters._
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

class ScalaRestriction(val underlying: Criterion) extends Aliasable {	
	def apply[A](c: ScalaCriteria[A]) = c.add(this)
	
	override final def equals(other: Any) = other match {
		case that: ScalaRestriction =>
			new EqualsBuilder()
				.append(underlying, that.underlying)
				.append(aliases, that.aliases)
				.build()
		case _ => false
	}
	
	override final def hashCode =
		new HashCodeBuilder()
			.append(underlying)
			.append(aliases)
			.build()
			
	override final def toString =
		new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("underlying", underlying)
			.append("aliases", aliases)
			.build()
}

object ScalaRestriction {
	import Aliasable._
	
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

trait Aliasable {
	final val aliases: mutable.Map[String, String] = mutable.Map()
	
	def alias(property: String, alias: String) = {
		aliases.put(property, alias) match {
			case Some(other) if other != alias => 
				// non-duplicate
				throw new IllegalArgumentException("Tried to alias %s to %s, but it is already aliased to %s!".format(property, alias, other))
			case _ =>
		}
		this
	}
}

object Aliasable {
	def addAliases[A <: Aliasable](aliasable: A, aliases: (String, String)*): A = {
		aliases.foreach { case (property, alias) => aliasable.alias(property, alias) }
		aliasable
	}
}