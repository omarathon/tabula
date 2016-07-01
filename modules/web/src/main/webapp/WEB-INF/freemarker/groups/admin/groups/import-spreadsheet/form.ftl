<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.groups.import_spreadsheet dept academicYear /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader "Import small groups from spreadsheet" route_function "for" />

<p>Oh, go on then, if you insist.</p>

<@f.form method="post" enctype="multipart/form-data" action="" commandName="command" cssClass="form-horizontal double-submit-protection">
	<@bs3form.labelled_form_group path="file.upload" labelText="Choose your spreadsheet">
		<input type="file" name="file.upload" />
	</@bs3form.labelled_form_group>
</@f.form>

</#escape>