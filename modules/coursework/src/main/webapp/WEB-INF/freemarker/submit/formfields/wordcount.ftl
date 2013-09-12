<#assign help>
	The number of words in the attachment you are submitting.
	${field.conventions!""}
	<#if (field.min)!0 gt 0 && (field.max)!assignment.MaximumWordCount lt assignment.MaximumWordCount>
		It must be between ${(field.min)!0} and ${(field.max)!assignment.MaximumWordCount} words.
	<#elseif (field.min)!0 == 0>
		It must be <@fmt.p (field.max)!assignment.MaximumWordCount "word" /> or less.
	<#else>
		It must be <@fmt.p (field.min)!0 "word" /> or more.
	</#if>
</#assign>

<@form.labelled_row "fields[${field.id}].value" "Word count" "" help>
	<@f.input id="fields[${field.id}].value" path="fields[${field.id}].value" cssClass="input-small" maxlength="${assignment.MaximumWordCount?c?length}" />
</@form.labelled_row>