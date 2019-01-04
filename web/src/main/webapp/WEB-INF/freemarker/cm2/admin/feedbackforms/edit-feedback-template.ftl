<#escape x as x?html>
<#assign actionUrl><@routes.cm2.editfeedbacktemplate department template /></#assign>
<@f.form enctype="multipart/form-data"
	method="post"
	action="${actionUrl}"
	modelAttribute="editFeedbackTemplateCommand">
	<@f.hidden path="id"/>

	<@bs3form.filewidget
		basename="file"
		labelText="Replace feedback form"
		types=[]
		multiple=false
	/>

	<@bs3form.labelled_form_group path="name" labelText="Name">
		<@f.input path="name" cssClass="form-control text" />

		<div class="help-block">
			The name that will appear when administrators are asked to choose which feedback template to use for a
			module.
		</div>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="description" labelText="Description">
		<@f.textarea path="description" class="form-control text big-textarea" />

		<div class="help-block">
			This field is to help administrators select appropriate feedback templates for their assignments.
			It will not be visible to students. You can leave this blank if you wish.
		</div>
	</@bs3form.labelled_form_group>
</@f.form>
</#escape>