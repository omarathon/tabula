package uk.ac.warwick.tabula.helpers

import java.util

import freemarker.template.utility.DeepUnwrap
import freemarker.template.{DefaultMapAdapter, SimpleHash, TemplateMethodModelEx, TemplateModel}

/**
 * Freemarker can't cope with maps who's keys are not strings
 *
 * Here's a helper to try and fix it.
 */
class FreemarkerMapHelper extends TemplateMethodModelEx {

	private val underlyingMapField = {
		val f = classOf[SimpleHash].getDeclaredField("map")
		f.setAccessible(true)
		f
	}

	private def toMap(m: SimpleHash): util.Map[_, _] =
		// I thought, I found a way to enter / it's just a reflektor
		underlyingMapField.get(m).asInstanceOf[util.Map[_, _]]

	private def toMap(m: DefaultMapAdapter): util.Map[_, _] =
		m.getAdaptedObject(classOf[util.Map[_, _]]).asInstanceOf[util.Map[_, _]]

	/**
		* The parameters here may vary depending on FreeMarker versions and the type of the key.
		*
		* https://freemarker.apache.org/docs/versions_2_3_22.html
		* - FreeMarker 2.3.21 and earlier will be a SimpleHash
		* - FreeMarker 2.3.22 and later will be a DefaultMapAdapter
		*
		* The key may or may not be wrapped
		*/
	override def exec(p1: util.List[_]): AnyRef =
		// The key may already be a wrapped object
		(p1.get(0), p1.get(1)) match {
			case (m: DefaultMapAdapter, k: TemplateModel) =>
				DeepUnwrap.unwrap(k) match {
					case key: String => DeepUnwrap.unwrap(m.get(key))
					case key => toMap(m).get(key) match {
						case r: AnyRef => r
						case _ => null
					}
				}

			case (m: SimpleHash, k: TemplateModel) =>
				DeepUnwrap.unwrap(k) match {
					case key: String => DeepUnwrap.unwrap(m.get(key))
					case key => toMap(m).get(key) match {
						case r: AnyRef => r
						case _ => null
					}
				}

			case (m: DefaultMapAdapter, k: String) =>
				DeepUnwrap.unwrap(m.get(k))

			case (m: SimpleHash, k: String) =>
				DeepUnwrap.unwrap(m.get(k))

			case (m: SimpleHash, k: AnyRef) =>
				toMap(m).get(k) match {
					case r: AnyRef => r
					case _ => null
				}

			// You've called with arguments that don't look like a wrapped map & a key
			case _ =>
				null
		}

}
