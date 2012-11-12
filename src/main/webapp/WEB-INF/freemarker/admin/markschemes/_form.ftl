<#-- 

Adding or editing a new markscheme

-->
<#if view_type="add">
	<#assign submit_text="Create" />
<#elseif view_type="edit">
	<#assign submit_text="Save" />
</#if>


<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

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
	<div id="firstmarkers-list">
	<@form.userpicker path="firstMarkers" list=true multiple=true />
	<#--
	<@spring.bind path="firstMarkers">
		<#assign _users=status.actualValue />
		<@userlookup ids=_users>
			<#list returned_users?values as _user>
				<div><input type="text" class="text" name="${status.expression}" value="${_user.userId}" /></div>
			</#list>
			<div><input type="text" class="text" name="${status.expression}" /></div>
			<button class="btn" data-expression="${status.expression}">Add</button>
		</@userlookup>
	</@spring.bind>
	-->
	</div>
	<script>
		jQuery('#firstmarkers-list button').on('click', function(e){
			e.preventDefault();
			var name = jQuery(this).data('expression'); 
			var newButton = jQuery('<div><input type="text" class="text" name="'+name+'" /></div>');
			jQuery('#firstmarkers-list button').before(newButton);
			return false;
		});
	</script>
</@form.labelled_row>

<@form.labelled_row "studentsChooseMarker" "">
	<label class="checkbox">
		<@f.checkbox path="studentsChooseMarker" />
		Students must select their marker
	</label>
	<div class="help-block">
		When checked, students will choose their marker out of the list of first markers 
		using a drop-down select box on the submission form.
	</div>
</@form.labelled_row>


<div class="submit-buttons">
<input type="submit" value="${submit_text}" class="btn btn-primary">
or <a class="btn" href="<@routes.markschemelist department />">Cancel</a>
</div>

</@f.form>

</#compress>
</#escape>