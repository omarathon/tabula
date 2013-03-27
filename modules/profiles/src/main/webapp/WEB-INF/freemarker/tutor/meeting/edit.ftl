<#assign heading>
	<h2>Record a meeting</h2>
	<h6>
		<span class="muted">between tutor</span> ${tutorName}
		<span class="muted">and tutee</span> ${student.fullName}
	</h6>
</#assign>

<#if modal??>
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		${heading}
	</div>
<#else>
	${heading}
</#if>
	
<@f.form method="post" action="${url('/tutor/meeting/' + student.universityId + '/create')}" commandName="createMeetingRecordCommand" class="form-horizontal">
	<#if modal??>
		<div class="modal-body">
	</#if>
	
	<@form.labelled_row "title" "Title">
		<@f.input type="text" path="title" cssClass="input-block-level" maxlength="255" placeholder="Subject of meeting" />
	</@form.labelled_row>
	
	<@form.labelled_row "meetingDate" "Date of meeting">
		<div class="input-append">
			<@f.input type="text" path="meetingDate" cssClass="input-medium date-picker" placeholder="Pick the date" />
			<span class="add-on"><i class="icon-calendar"></i></span>
		</div>
	</@form.labelled_row>
	
	<#-- TODO: TinyMCE editor, bleh -->
	<@form.labelled_row "description" "Description (optional)">
		<@f.textarea rows="6" path="description" cssClass="input-block-level" />
	</@form.labelled_row>
	
	<#-- TODO: file upload to follow in TAB-359 -->
	
	<#if modal??>
		</div>
		<div class="modal-footer">
			<input type="hidden" name="modal" value="true" />
			<button class="btn btn-primary" type="submit" name="submit">
				Publish <#-- TODO: 'Submit for approval' to follow in TAB-402 et alia, ad infinitum -->
			</button>
			<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
		</div>
	<#else>
		<div class="form-actions">
			<button class="btn btn-primary" type="submit" name="submit">
				Publish <#-- TODO: 'Submit for approval' to follow in TAB-402 et alia, ad infinitum -->
			</button>
			<a class="btn" href="<@routes.profile student />">Cancel</a>
		</div>
	</#if>
</@f.form>