<#-- 

Adding or editing a new marking workflow

-->
<#if view_type="add">
	<#assign submit_text="Create" />
<#elseif view_type="edit">
	<#assign submit_text="Save" />
</#if>

<#assign department=command.department />

<#escape x as x?html>
<#compress>

<h1>Define marking workflow</h1>
<#assign commandName="command" />

<@f.form method="post" action="${form_url}" commandName=commandName cssClass="form-horizontal">
<@f.errors cssClass="error form-errors" />

<#--
Common form fields.
-->
<@form.labelled_row "name" "Name">
	<@f.input path="name" cssClass="text" />
	<div class="help-block">
		A descriptive name that will be used to refer to this marking workflow elsewhere.
	</div>
</@form.labelled_row>

<@form.labelled_row "markingMethod" "Marking Method">

	<#assign isDisabled><#if view_type=="edit">true<#else>false</#if></#assign>

	<@f.select disabled="${isDisabled}" path="markingMethod">
		<option <#if !command.markingMethod?has_content>selected="selected"</#if> value=""></option>
		<option value="StudentsChooseMarker"
				<#if ((command.markingMethod.toString)!"") = "StudentsChooseMarker">selected="selected"</#if>
				data-firstrolename="Marker"
				data-firstrolename=""
				>
			Students choose marker
		</option>
		<option value="SeenSecondMarking" class="uses-second-markers"
				<#if ((command.markingMethod.toString)!"") = "SeenSecondMarking">selected="selected"</#if>
				data-firstrolename="First marker"
				data-secondrolename="Second marker"
				>
			Seen second marking
		</option>
		<option value="ModeratedMarking" class="uses-second-markers"
				<#if ((command.markingMethod.toString)!"") = "ModeratedMarking">selected="selected"</#if>
				data-firstrolename="Marker"
				data-secondrolename="Moderator"
				>Moderated marking
		</option>
	</@f.select>


	<#if view_type=="edit">
		<div class="help-block">
			It is not possible to modify the marking method once a marking workflow has been created.
		</div>
	</#if>
</@form.labelled_row>

<@form.labelled_row "firstMarkers" "${command.firstMarkerRoleName!'Marker'}">
	<@form.flexipicker path="firstMarkers" list=true multiple=true />
</@form.labelled_row>

<div class="second-markers-container hide">
<@form.labelled_row "secondMarkers" "${command.secondMarkerRoleName!'Second Marker'}">
	<@form.flexipicker path="secondMarkers" list=true multiple=true />
</@form.labelled_row>
</div>

<script>

</script>

<div class="submit-buttons">
<input type="submit" value="${submit_text}" class="btn btn-primary">
<a class="btn" href="<@routes.markingworkflowlist department />">Cancel</a>
</div>

</@f.form>

</#compress>
</#escape>