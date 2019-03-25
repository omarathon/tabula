package uk.ac.warwick.tabula.data.model

import java.sql.Types

import org.hibernate.`type`._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._

/**
  * Opinionated: this will treat "" (or strings containing only whitespace) as
  * None
  */
class OptionStringUserType extends AbstractBasicUserType[Option[String], String] {
  def convertToObject(input: String): Option[String] = input.maybeText

  def convertToValue(obj: Option[String]): String = obj.filter(_.hasText).orNull

  def sqlTypes = Array(Types.VARCHAR)

  val basicType: StringType = StandardBasicTypes.STRING
  val nullValue: String = null
  val nullObject: Option[String] = None
}

class OptionIntegerUserType extends AbstractBasicUserType[Option[Int], JInteger] {
  def convertToObject(input: JInteger) = Option(input)

  def convertToValue(obj: Option[Int]): JInteger = JInteger(obj)

  def sqlTypes = Array(Types.INTEGER)

  val basicType: IntegerType = StandardBasicTypes.INTEGER
  val nullValue: JInteger = null
  val nullObject: Option[Int] = None
}

class OptionBooleanUserType extends AbstractBasicUserType[Option[Boolean], JBoolean] {
  def convertToObject(input: JBoolean) = Option(input)

  def convertToValue(obj: Option[Boolean]): JBoolean = JBoolean(obj)

  def sqlTypes = Array(Types.BOOLEAN)

  val basicType: BooleanType = StandardBasicTypes.BOOLEAN
  val nullValue: JBoolean = null
  val nullObject: Option[Boolean] = None
}

class OptionBigDecimalUserType extends AbstractBasicUserType[Option[BigDecimal], JBigDecimal] {
  def convertToObject(input: JBigDecimal) = Option(input)

  def convertToValue(obj: Option[BigDecimal]): JBigDecimal = JBigDecimal(obj)

  def sqlTypes = Array(Types.NUMERIC)

  val basicType: BigDecimalType = StandardBasicTypes.BIG_DECIMAL
  val nullValue: JBigDecimal = null
  val nullObject: Option[BigDecimal] = None
}