<#import "../submissionsandfeedback/_submission_details.ftl" as sd />

<div class="feedback">
	<#if command.submission??>
		<#assign submission = command.submission />

		<div class="well">
			<h3>Submission</h3>

			<div class="labels">
				<#if submission.late>
					<span class="label label-important use-tooltip" title="<@sd.lateness submission />" data-container="body">Late</span>
				<#elseif  submission.authorisedLate>
					<span class="label label-info use-tooltip" title="<@sd.lateness submission />" data-container="body">Within Extension</span>
				</#if>

				<#if submission.suspectPlagiarised>
					<span class="label label-important use-tooltip" title="Suspected of being plagiarised" data-container="body">Plagiarism Suspected</span>
				</#if>
			</div>

			<div>
				<@spring.message code=command.submissionState /><@sd.submission_details command.submission />
			</div>
		</div>
	</#if>

	<div class="form">
		<form class="form-horizontal">
			<div class="control-group">
				<label class="control-label" for="inputEmail">Email</label>
				<div class="controls">
				  <input type="text" id="inputEmail" placeholder="Email">
				</div>
			  </div>
			  <div class="control-group">
				<label class="control-label" for="inputPassword">Password</label>
				<div class="controls">
				  <input type="password" id="inputPassword" placeholder="Password">
				</div>
			  </div>
			  <div class="control-group">
				<div class="controls">
				  <label class="checkbox">
					<input type="checkbox"> Remember me
				  </label>
				  <button type="submit" class="btn">Sign in</button>
				</div>
			  </div>
  		</form>
	</div>
</div>