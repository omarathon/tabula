<section class="edit-meeting">	
	<h2>Record a meeting</h2>
	<h6>between
		<span class="muted">tutor</span> ${tutorName}
		and
		<span class="muted">tutee</span> ${student.fullName}
	</h6>
		
	<@f.form method="post" action="${url('/tutor/meeting/' + student.universityId + '/create')}" commandName="command" class="form-horizontal">
		<@form.errors path="*" />
	
		<div class="control-group">
			<label class="control-label" for="title">Title</label>
			<div class="controls">
				<@f.input type="text" id="title" path="meeting.title" cssClass="input-xxlarge" maxlength="255" placeholder="Subject of meeting" />
			</div>
		</div>
		
		<div class="control-group">
			<label class="control-label" for="date">Date of meeting</label>
			<div class="controls">
				<@f.input type="text" id="date" path="meeting.meetingDate" cssClass="input-medium date-time-picker" />
			</div>
		</div>
		
		<#-- TODO: TinyMCE editor, bleh -->
		<div class="control-group">
			<label class="control-label" for="record">Description (optional)</label>
			<div class="controls">
				<@f.textarea path="meeting.description" id="record" rows="6" cssClass="input-xxlarge" />
			</div>
		</div>
		
		<#-- TODO: file upload to follow in TAB-359 -->

		<div class="control-group">
			<div class="controls">
				<button type="submit" name="submit" class="btn btn-primary">
					Publish <#-- TODO: 'Submit for approval' to follow in TAB-402 et alia, ad infinitum -->
				</button>
				<a class="btn" href="<@routes.profile student />">Cancel</a>
			</div>
		</div>
	</@f.form>
</section>

