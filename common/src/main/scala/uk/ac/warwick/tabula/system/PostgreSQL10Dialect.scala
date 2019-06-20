package uk.ac.warwick.tabula.system

import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.dialect.function.StandardSQLFunction

class PostgreSQL10Dialect extends org.hibernate.dialect.PostgreSQL95Dialect {
  registerFunction(
    "bool_or",
    new StandardSQLFunction("bool_or", StandardBasicTypes.BOOLEAN)
  )
  registerFunction(
    "bool_and",
    new StandardSQLFunction("bool_and", StandardBasicTypes.BOOLEAN)
  )
}
