<section class="tutor-search">	
	<@f.form method="get" action="${url('/tutor/search')}" commandName="searchTutorsCommand" class="form-horizontal">
		<input id="student" name="student" type="hidden" value="${student.universityId}" />
		<div class="control-group">
			<label class="control-label" for="personal-tutor"><b>Personal Tutor</b></label>
			<div class="controls">
				
				<#if tutorToDisplay??>
					<@f.input type="text" id="personal-tutor" name="personal-tutor" path="query" 
						placeholder="${tutorToDisplay.firstName} ${tutorToDisplay.lastName} (${tutorToDisplay.universityId})" />
				<#else>
					<@f.input type="text" id="personal-tutor" name="personal-tutor" path="query"  />
				</#if>

				<button type="submit" class="btn btn-primary">
					<i id="tutor-search-icon" class="icon-search icon-white"></i>
				</button>
				<a href="<@routes.tutor_edit_no_tutor student="${student.universityId}" />" class="btn">Reset</a>
			</div>
		</div>
	</@f.form>
</section>

