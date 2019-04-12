package uk.ac.warwick.tabula.data.model

import java.io.Serializable
import java.sql.{PreparedStatement, ResultSet, Types}

import enumeratum.EnumEntry
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import uk.ac.warwick.tabula.data.EnumCompanionHelper

import scala.language.reflectiveCalls
import scala.reflect.classTag
import scala.reflect.runtime.universe

import scala.collection.JavaConverters._

abstract class EnumSeqUserType[A <: EnumEntry](implicit tt: universe.TypeTag[A]) extends UserType with EnumCompanionHelper {

  override def sqlTypes(): Array[Int] = Array(Types.ARRAY)

  override def returnedClass: Class[_] = classTag[Seq[A]].runtimeClass

  final override def nullSafeGet(resultSet: ResultSet, names: Array[String], impl: SharedSessionContractImplementor, owner: Object): Seq[A] = {
    Option(resultSet.getArray(names.head))
      .map(_.getArray())
      .map(a => a.asInstanceOf[Array[String]].toSeq)
      .map(_.map(companionOf[A].withName))
      .orNull
  }

  final override def nullSafeSet(stmt: PreparedStatement, value: Any, index: Int, impl: SharedSessionContractImplementor) {
    value match {
      case v: Seq[A] =>
        val array = stmt.getConnection.createArrayOf("VARCHAR", v.map(_.entryName).asJava.toArray)
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