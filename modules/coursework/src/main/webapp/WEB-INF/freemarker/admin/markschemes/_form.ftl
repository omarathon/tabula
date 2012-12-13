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

<@form.labelled_row "firstMarkers" "First markers">
	<@form.userpicker path="firstMarkers" list=true multiple=true />
</@form.labelled_row>

<div class="control-group">
	<label class="control-label">Students must select their marker</label>
	<div class="controls">
		<input type="checkbox" checked="checked" disabled="disabled"/>
		<div class="help-block">
			When checked, students will choose their marker out of the list of first markers
			using a drop-down select box on the submission form. You will be able to un-check this option when
			more mark schemes are released in upcoming versions of the app.
		</div>
	</div>
</div>
<#-- TODO - replace with block below when more options become available - but disable it if existingSubmissions is true -->
<#--@form.labelled_row "studentsChooseMarker" "">
	<label class="checkbox">
		<@f.checkbox path="studentsChooseMarker" />
		Students must select their marker
	</label>
	<div class="help-block">
		When checked, students will choose their marker out of the list of first markers
		using a drop-down select box on the submission form.
	</div>
</@form.labelled_row-->


<div class="submit-buttons">
<input type="submit" value="${submit_text}" class="btn btn-primary">
or <a class="btn" href="<@routes.markschemelist department />">Cancel</a>
</div>

</@f.form>

</#compress>
</#escape>