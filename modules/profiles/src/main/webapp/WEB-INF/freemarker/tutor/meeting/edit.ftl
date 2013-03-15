<section class="edit-meeting">	
	<h2>Record a meeting</h2>
	<h6>
		<span class="muted">between tutor</span> ${tutorName}
		<span class="muted">and tutee</span> ${student.fullName}
	</h6>
		
	<@f.form method="post" action="${url('/tutor/meeting/' + student.universityId + '/create')}" commandName="command" class="form-horizontal">
		<@form.labelled_row "title" "Title">
			<@f.input type="text" path="title" cssClass="input-xxlarge" maxlength="255" placeholder="Subject of meeting" />
		</@form.labelled_row>
		
		<@form.labelled_row "meetingDate" "Date of meeting">
			<div class="input-append">
				<@f.input type="text" path="meetingDate" cssClass="input-medium date-time-picker" placeholder="dd-mmm-yyyy hh:mm" />
				<span class="add-on"><i class="icon-calendar"></i></span>
			</div>
		</@form.labelled_row>
		
		<#-- TODO: TinyMCE editor, bleh -->
		<@form.labelled_row "description" "Description (optional)">
			<@f.textarea rows="6" path="description" cssClass="input-xxlarge" />
		</@form.labelled_row>
		
		<#-- TODO: file upload to follow in TAB-359 -->
		
		<div class="form-actions">
			<button type="submit" name="submit" class="btn btn-primary">
				Publish <#-- TODO: 'Submit for approval' to follow in TAB-402 et alia, ad infinitum -->
			</button>
			<a class="btn" href="<@routes.profile student />">Cancel</a>
		</div>
	</@f.form>
</section>