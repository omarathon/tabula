package uk.ac.warwick.tabula.system
import org.springframework.beans.PropertyEditorRegistrar
import org.springframework.beans.PropertyEditorRegistry
import java.beans.PropertyEditor
import uk.ac.warwick.util.web.bind.TrimmedStringPropertyEditor

class CommonPropertyEditors extends PropertyEditorRegistrar {

	// define a neater `register` method for P.E.R.
	implicit def cleverRegistry(registry: PropertyEditorRegistry) = new {
		def register[A](editor: PropertyEditor)(implicit m: Manifest[A]) =
			registry.registerCustomEditor(m.erasure, editor)
	}

	override def registerCustomEditors(registry: PropertyEditorRegistry) {
		registry.register[String](new TrimmedStringPropertyEditor)
	}

}