package uk.ac.warwick.tabula.data.model

import java.io.Serializable
import java.sql.{PreparedStatement, ResultSet, Types}

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import play.api.libs.json.{Format, Json}

import scala.jdk.CollectionConverters._

case class ReasonableAdjustment(
  id: String,
  description: String,
)

object ReasonableAdjustment {
  implicit val formatsReasonableAdjustment: Format[ReasonableAdjustment] = Json.format[ReasonableAdjustment]
}

/**
 * Hibernate UserType for storing ReasonableAdjustment as a Postgres Array, storing the JSON representation
 */
class ReasonableAdjustmentsUserType extends UserType {

  override def sqlTypes(): Array[Int] = Array(Types.ARRAY)

  override def returnedClass: Class[_] = classOf[Set[ReasonableAdjustment]]

  final override def nullSafeGet(resultSet: ResultSet, names: Array[String], impl: SharedSessionContractImplementor, owner: Object): Set[ReasonableAdjustment] = {
    Option(resultSet.getArray(names.head))
      .map(_.getArray())
      .map(a => a.asInstanceOf[Array[String]].toSet)
      .map(_.map(Json.parse(_).as[ReasonableAdjustment]))
      .orNull
  }

  final override def nullSafeSet(stmt: PreparedStatement, value: Any, index: Int, impl: SharedSessionContractImplementor) {
    value match {
      case v: Set[ReasonableAdjustment] @unchecked =>
        val array = stmt.getConnection.createArrayOf("VARCHAR", v.toSeq.sortBy(_.id).map(ra => Json.stringify(Json.toJson(ra))).asJava.toArray)
        stmt.setArray(index, array)
      case _ => stmt.setNull(index, Types.ARRAY)
    }
  }

  override def isMutable = false

  override def equals(x: Object, y: Object): Boolean = x == y

  override def hashCode(x: Object): Int = Option(x).getOrElse("").hashCode

  override def deepCopy(x: Object): Object = x

  override def replace(original: Object, target: Object, owner: Object): Object = original

  override def disassemble(value: Object): Serializable = value.asInstanceOf[java.io.Serializable]

  override def assemble(cached: java.io.Serializable, owner: Object): AnyRef = cached.asInstanceOf[AnyRef]

}
