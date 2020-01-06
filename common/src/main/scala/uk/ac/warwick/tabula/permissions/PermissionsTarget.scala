package uk.ac.warwick.tabula.permissions

/**
  * Applying this trait to an object signifies that it fulfils a contract for
  * being a target for permissions checking. Roles can grant permissions against
  * objects of this type, or a parent of this object.
  *
  * For example, Assignment may extend PermissionsTarget. A role may grant the
  * permission Assignment.Delete() either directly to the Assignment, or to the
  * module in which the Assignment contains. Permissions checking code can therefore
  * check against the Assignment itself, or (iteratively) against its permissions
  * container.
  *
  * If this object is at the top of its permissions tree (for example, a Department)
  * then permissionsParents should return an empty Seq().
  */
trait PermissionsTarget {

  /**
    * This should return a sequence of *DIRECT* permission parents. Usually this will
    * return a singleton LazyList(parent) or a LazyList.empty (for a top-level permission element
    * such as a Department) but there are some situations (such as for an object that
    * exists in multiple departments) where it will return more than one.
    */
  def permissionsParents: LazyList[PermissionsTarget]

  /**
    * A unique identifier. If we were to pass this in as a PathVariable, we'd expect to be
    * able to get the object back out.
    */
  def id: String

  /**
    * A human-readable identifier, for a module this may be EC205 and for a Member it may be Mathew Mannion.
    */
  def humanReadableId: String = toString()

  def urlCategory: String = getClass.getSimpleName.toLowerCase()

  def urlSlug: String = id

}

object PermissionsTarget {
  final val GlobalScopeType: String = "___GLOBAL___"

  final val Global = new PermissionsTarget {
    override def permissionsParents: LazyList[Nothing] = LazyList.empty

    override def id = null

    override def humanReadableId = null

    override def urlCategory = null

    override def toString: String = "Global"
  }
}
