<#assign markers=field.markers />
<@form.labelled_row "fields[${field.id}]" "Select your marker">
	<@f.select path="fields[${field.id}].value">
		<@f.option value="" label="None"/>
		<#list markers as marker>
			<@f.option value="${marker.userId}" label="${marker.fullName}"/>
		</#list>
	</@f.select>
	<div class="help-block">
		Select the tutor that will mark your submission. If your are unsure which tutor should mark your work then contact
		the module convenor.
	</div>
</@form.labelled_row>