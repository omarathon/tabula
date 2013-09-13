<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>


<div>
	Create a new member note for:  ${command.member.fullName}

	<@f.form id="member-note-form" method="post" enctype="multipart/form-data" action="" commandName="command" class="form-horizontal double-submit-protection">
		<@form.labelled_row "title" "Title">
			<@f.input type="text" path="title" cssClass="input-block-level" maxlength="255" placeholder="The title" />
		</@form.labelled_row>

		<@form.labelled_row "note" "The Note">
			<@f.input type="text" path="note" cssClass="input-block-level" maxlength="255" placeholder="Your note" />
		</@form.labelled_row>

		<#assign fileTypes=command.attachmentTypes />
		<@form.filewidget basename="file" types=fileTypes />

		<div class="submit-buttons">
			<input type="submit" class="btn btn-primary" value="Save">
		</div>

	</@f.form>


</div>