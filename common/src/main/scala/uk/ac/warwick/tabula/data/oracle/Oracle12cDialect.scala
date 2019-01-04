package uk.ac.warwick.tabula.data.oracle
import java.sql.Types
import org.hibernate.Hibernate
import org.hibernate.`type`.StandardBasicTypes

/**
 * odjbc6 seems to trigger this error:
 * org.hibernate.MappingException: No Dialect mapping for JDBC type: -9
 * -9 is the value of Types.NVARCHAR, for some reason it can't figure out what type it is.
 */
class Oracle12cDialect extends org.hibernate.dialect.Oracle12cDialect {
	registerHibernateType(Types.NVARCHAR, StandardBasicTypes.STRING.getName)
	registerHibernateType(Types.NCLOB, StandardBasicTypes.CLOB.getName)
	registerHibernateType(Types.NCHAR, StandardBasicTypes.STRING.getName)
	registerHibernateType(Types.BOOLEAN, "boolean")

	registerColumnType(Types.BOOLEAN, "boolean")

	//registerColumnType(Types.NCLOB, "clob")
}