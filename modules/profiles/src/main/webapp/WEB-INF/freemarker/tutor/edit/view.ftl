<#escape x as x?html>
	<h3>Personal Tutee: ${student.firstName} ${student.lastName} (${student.universityId})</h3>

	<#if user.staff>
		<#include "form.ftl" />
	
		<section class="tutor-edit">
			<#if displayOptionToSave>
				<hr class="full-width">
				<@f.form 
					id="saveTutor"
					method="post" 
					action="/profiles/tutor/${studentUniId}/edit?tutorUniId=${pickedTutor.universityId}"
					commandName="editTutorCommand" 
					class="form-horizontal">

					&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
					<@f.checkbox path="notifyTutee" value="true" /> Notify tutee of change<br />
					&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
					<@f.checkbox path="notifyOldTutor" value="true" /> Notify old tutor of change<br />
					&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
					<@f.checkbox path="notifyNewTutor" value="true" /> Notify new tutor of change
					<br />
					<input id="save" name="save" type="hidden" value="true" />
					<div style="text-align:right">
						<button type="submit" class="btn btn-primary">Save</button
					</div>
				</@f.form>

			</#if>
		</section>

	</#if>
	<section class="tutor-edit-return">
		&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
		<a href="<@routes.profile_by_id studentUniId="${student.universityId}" />" class="btn">Return to ${student.firstName}'s profile page</a>
	</section>
</#escape>
