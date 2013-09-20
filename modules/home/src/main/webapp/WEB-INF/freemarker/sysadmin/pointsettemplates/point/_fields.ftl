<@form.labelled_row "name" "Name">
	<@f.input path="name" cssClass="input-block-level"/>
</@form.labelled_row>

<@form.labelled_row "defaultValue" "Default value">
	<@f.select path="defaultValue">
		<@f.option value="true">Attended</@f.option>
		<@f.option value="false">Missed</@f.option>
	</@f.select>
</@form.labelled_row>

<@form.labelled_row "week" "Term week">
	<@f.select path="week">
		<#list 1..52 as week>
			<@f.option value="${week}">Week ${week}</@f.option>
		</#list>
	</@f.select>
</@form.labelled_row>