package uk.ac.warwick.courses.helpers

import scala.collection.mutable

/**
 * Implicit type conversions that help make the rest of the code a bit cleaner.
 * 
 * Until Scala support automatic conversion from lambda function to anonymous Java interface,
 * this will be a good place to put conversions from e.g. lambda to HibernateCallback, so
 * that you can use a lambda where a HibernateCallback is expected.
 */
trait ImplicitConversions {
  
	// Implicitly convert a Map to a Properties where it fits
  	implicit def ScalaMapAsProperties(map:Map[String,String]) : java.util.Properties = map match {
	  case _ => new java.util.Properties {
	    putAll(map)
	  }
	}
	
}