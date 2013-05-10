<#escape x as x?html>
	<h3>Personal Tutee: ${student.firstName} ${student.lastName} (${student.universityId})</h3>

	<#if user.staff>
		<#include "form.ftl" />
	
		<section class="tutor-edit">
			<#if displayOptionToSave>
				<@f.form 
					id="saveTutor"
					method="post" 
					action="/profiles/tutor/${student.universityId}/edit"
					commandName="editTutorCommand" 
					class="form-horizontal">
					
					<#if editTutorCommand.currentTutor??>
						<@spring.bind path="currentTutor">
							<input id="currentTutor" name="${status.expression}" type="hidden" value="${editTutorCommand.currentTutor.universityId}" />
						</@spring.bind>
					</#if>

					<@spring.bind path="tutor">
						<input id="tutor" name="${status.expression}" type="hidden" value="${editTutorCommand.tutor.universityId}" />
					</@spring.bind>
					
					<div class="control-group">
						<div class="controls">			
							<label class="checkbox">
								<@f.checkbox path="notifyCommand.notifyTutee" value="true" /> 
								Notify tutee of change<br />
								
								<@f.checkbox path="notifyCommand.notifyOldTutor" value="true" /> 
								Notify old tutor of change<br />
								
								<@f.checkbox path="notifyCommand.notifyNewTutor" value="true" /> 
								Notify new tutor of change<br />
							</label>	

							<input id="storeTutor" name="storeTutor" type="hidden" value="true" />
							<div style="text-align:right">
								<button type="submit" class="btn btn-primary">Save</button
							</div>
						</div>		
					</div>			
				</@f.form>
			</#if>
		</section>

	</#if>
	<section class="tutor-edit-return">
		<a href="<@routes.profile_by_id student="${student.universityId}" />" class="btn">Return to ${student.firstName}'s profile page</a>
	</section>
</#escape>
