	<#if ((canSubmit && !submission??) || canReSubmit) && submitAssignmentCommand??>
		<#if submission??>
			<hr>
			<h2>Resubmit</h2>
			<#if assignment.openEnded>
				<p>You can still resubmit your work in case you've made a mistake.</p>
			<#else>
				<p>You can resubmit your work in case you've made a mistake,
					<#if isExtended>
						up until the end of your extension, <@fmt.date date=extension.expiryDate /> (in ${durationFormatter(extension.expiryDate)}).
					<#else>
						up until the deadline, <@fmt.date date=assignment.closeDate /> (in ${durationFormatter(assignment.closeDate)}).
					</#if>
				</p>
			</#if>
		</#if>

		<#if assignment.closed && !isExtended>
			<div class="alert alert-warning">
				<h3>Submission date has passed</h3>
				<p>
					You can still submit to this assignment but your mark may be affected.
				</p>
			</div>
		</#if>

		<#assign submitUrl><@routes.cm2.assignment assignment /></#assign>

		<@f.form cssClass="double-submit-protection" enctype="multipart/form-data" method="post" action="${submitUrl}#submittop" modelAttribute="submitAssignmentCommand">

			<#if errors.hasErrors()>
				<div class="alert alert-warning">
					<button type="button" class="close" data-dismiss="alert">&times;</button>

					<h4>Your submission was not accepted</h4>

					<p>Some of the information in your submission was not accepted. Please check the errors in red below and resubmit the form.</p>
				</div>

				<script type="text/javascript">
					jQuery(function($) {
						$(".alert-danger").each(function() {
							$("html, body").animate({
								scrollTop: $(this).offset().top - 35
							}, 300);
						});
					});
				</script>
			</#if>
			<@f.errors cssClass="alert alert-warning"></@f.errors>

			<@bs3form.labelled_form_group path="" labelText="University ID">
					<p class="very-subtle">
						${user.studentIdentifier}
					</p>
			</@bs3form.labelled_form_group>

			<@bs3form.labelled_form_group path="" labelText="Assignment information">
				<div>
					<#if !assignment.openEnded>
						<#macro extensionButtonContents label assignment>
							<a href="<@routes.cm2.extensionRequest assignment=assignment />?returnTo=<@routes.cm2.assignment assignment=assignment />" class="btn btn-default btn-xs">
								${label}
							</a>
						</#macro>

						<#macro extensionButton extensionRequested isExtended>
							<#if extensionRequested>
								<@extensionButtonContents "Review extension request" assignment />
							<#elseif !isExtended && assignment.newExtensionsCanBeRequested>
								<@extensionButtonContents "Request an extension" assignment />
							</#if>
						</#macro>

						<#assign time_remaining = durationFormatter(assignment.closeDate) />
						<p>
							<#if isExtended>
								<#assign extension_time_remaining = durationFormatter(extension.expiryDate) />

								<span>Assignment due:</span> You have an extension until <@fmt.date date=extension.expiryDate />,
								<span class="very-subtle">${extension_time_remaining}</span>.
								<@extensionButton extensionRequested isExtended />
							<#else>
								<span>Assignment due:</span> <@fmt.date date=assignment.closeDate /> -
								<span class="very-subtle">${time_remaining}</span>.
								<@extensionButton extensionRequested isExtended />
							</#if>
						</p>

						<#if assignment.allowResubmission && (!assignment.closed || isExtended)>
							<p class="very-subtle">
								You can resubmit your assignment multiple times until the deadline.
								We only accept your latest submission.
								You cannot resubmit work after the deadline.
							</p>
						</#if>

						<#if assignment.allowLateSubmissions>
							<p class="very-subtle">
								If do not submit your assignment before the deadline, you can submit late work only once. Your mark may be affected.
							</p>
						</#if>
					<#else>
						<p class="very-subtle">
							This assignment does not have a deadline.

							<#if assignment.allowResubmission>
								You can submit to this assignment multiple times, but only
								the latest submission of your work will be kept.
							</#if>
						</p>
					</#if>
				</div>
			</@bs3form.labelled_form_group>

			<div>

				<#list assignment.submissionFields as field>
					<div>
						<#include "./formfields/${field.template}.ftl" >
					</div>
				</#list>

				<#if hasDisability>
					<div>
						<#include "./formfields/disability.ftl" >
					</div>
				</#if>

				<#if assignment.displayPlagiarismNotice>
				<div class="panel panel-default">
					<div class="panel-body">
						<@bs3form.labelled_form_group path="" labelText="Plagiarism declaration">
							<p>
								In submitting my work I confirm that:
							</p>

							<ol>
								<li>I have read the guidance on plagiarism/cheating provided in the handbook and understand the University regulations in relation to plagiarism/cheating. I am aware of the potential consequences of committing plagiarism/cheating. I declare that the work is all my own, except where I have stated otherwise.</li>
								<li>No substantial part(s) of the work submitted here has also been submitted by me in other assessments for accredited courses of study (other than in the case of a resubmission of a piece of work), and I acknowledge that if this has been done an appropriate reduction in the mark I might otherwise have received will be made.</li>
								<li>I understand that should this piece of work raise concerns requiring investigation in relation to points 1) and/or 2) above, it is possible that other work I have submitted for assessment will be checked, even if the marking process has been completed.</li>
								<li>Where a proof reader, paid or unpaid was used, I confirm that the proof reader was made aware of and has complied with the <a target='_blank' href='https://warwick.ac.uk/proofreadingpolicy'>University’s proof reading policy</a>.</li>
							</ol>
						</@bs3form.labelled_form_group>
					</div>
				</div>

				</#if>

				<#if features.privacyStatement>
					<@bs3form.labelled_form_group path="" labelText="Privacy statement">
						<p>
							The data on this form relates to your submission of coursework. The date and time of your submission,
							your identity and the work you have submitted will be stored. We will only use this data to administer
							and record your coursework submission.
						</p>
					</@bs3form.labelled_form_group>
				</#if>
			</div>

			<div class="submit-buttons">
				<input class="btn btn-large btn-primary" type="submit" value="Submit">
				<a class="btn btn-default" href="<@routes.cm2.home />">Cancel</a>
				<#if willCheckpointBeCreated>
					<div class="alert alert-info" style="display: inline-block;">
						Submitting this assignment will mark a monitoring point as attended
					</div>
				</#if>
			</div>
		</@f.form>

		<script>
			jQuery(function($){
				$('form#submitAssignmentCommand').on('submit', function (e) {
					e.preventDefault();
					var form = this;
					$.post({
						url: '<@routes.cm2.submission_attempt assignment />',
						complete: function () {
							form.submit();
						}
					});
				});
			});
		</script>

	<#elseif !submission??>

		<#if !assignment.alive>
			<p>
				This assignment is no longer collecting submissions through Tabula because it has been archived.
			</p>

			<h3>Expecting your feedback?</h3>

			<p>
				Sorry, but there doesn't seem to be anything here for you.
				If you've been told to come here to retrieve your feedback
				or submit your assignment then you'll need to get in touch directly with your
				course/module convenor as the assignment has now been archived.
			</p>
		<#elseif !assignment.collectSubmissions>
			<p>
				This assignment isn't collecting submissions through this system, but you may get
				an email to retrieve your feedback from here.
			</p>

			<h3>Expecting your feedback?</h3>

			<p>
				Sorry, but there doesn't seem to be anything here for you.
				If you've been told to come here to retrieve your feedback
				then you'll need to get in touch directly with your
				course/module convenor to see why it hasn't been published yet.
				When it's published you'll receive an automated email.
			</p>
		<#elseif !assignment.opened>
			<p>This assignment isn't open yet - it will open <@fmt.date date=assignment.openDate at=true />.</p>
		<#elseif assignment.closed && !isExtended>
			<div class="alert alert-warning">
				<h3>Submission date has passed</h3>

				This assignment doesn't allow late submissions.
			</div>
		</#if>

	</#if>