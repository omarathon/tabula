<#-- 

Adding or editing a new markscheme

-->
<#if view_type="add">
	<#assign submit_text="Create" />
<#elseif view_type="edit">
	<#assign submit_text="Save" />
</#if>

<#assign department=command.department />

<#escape x as x?html>
<#compress>

<h1>Define mark scheme</h1>
<#assign commandName="command" />

<@f.form method="post" action="${form_url}" commandName=commandName cssClass="form-horizontal">
<@f.errors cssClass="error form-errors" />

<#--
Common form fields.
-->
<@form.labelled_row "name" "Name">
	<@f.input path="name" cssClass="text" />
	<div class="help-block">
		A descriptive name that will be used to refer to this mark scheme elsewhere.
	</div>
</@form.labelled_row>

<@form.labelled_row "markingMethod" "Marking Method">
	<@f.select disabled="${hasSubmissions?string}" path="markingMethod">
		<@f.option />
		<@f.option value="StudentsChooseMarker" label="Students choose marker" />
		<@f.option class="uses-second-markers" value="SeenSecondMarking" label="Seen second marking" />
	</@f.select>
	<#if hasSubmissions>
		<div class="help-block">
			It is not possible to change the marking method as submissions exist.
		</div>
	</#if>
</@form.labelled_row>

<@form.labelled_row "firstMarkers" "First markers">
	<@form.userpicker path="firstMarkers" list=true multiple=true />
</@form.labelled_row>

<div class="second-markers-container hide">
<@form.labelled_row "secondMarkers" "Second markers">
	<@form.userpicker path="secondMarkers" list=true multiple=true />
</@form.labelled_row>
</div>

<script>

</script>

<div class="submit-buttons">
<input type="submit" value="${submit_text}" class="btn btn-primary">
<a class="btn" href="<@routes.markschemelist department />">Cancel</a>
</div>

</@f.form>

</#compress>
</#escape>