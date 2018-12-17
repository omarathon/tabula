	<#if !submission?? && assignment.collectSubmissions && assignment.alive>
		<#include "_assignment_deadline.ftl" />
	</#if>

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

		<#assign submitUrl><@routes.coursework.assignment assignment /></#assign>

		<@f.form cssClass="submission-form double-submit-protection form-horizontal" enctype="multipart/form-data" method="post" action="${submitUrl}#submittop" modelAttribute="submitAssignmentCommand">

			<#if errors.hasErrors()>
				<div class="alert alert-error animated flash">
					<button type="button" class="close" data-dismiss="alert">&times;</button>

					<h4>Your submission was not accepted</h4>

					<p>Some of the information in your submission was not accepted. Please check the errors in red below and resubmit the form.</p>
				</div>

				<script type="text/javascript">
					jQuery(function($) {
						$(".alert-error").each(function() {
							$("html, body").animate({
								scrollTop: $(this).offset().top - 35
							}, 300);
						});
					});
				</script>
			</#if>
			<@f.errors cssClass="error form-errors"></@f.errors>

			<@form.labelled_row "" "Your University ID">
				<div class="uneditable-input">${user.studentIdentifier}</div>
			</@form.labelled_row>

			<div class="submission-fields">

				<#list assignment.submissionFields as field>
					<div class="submission-field">
						<#include "/WEB-INF/freemarker/coursework/submit/formfields/${field.template}.ftl" >
					</div>
				</#list>

				<#if hasDisability>
					<div class="submission-field">
						<#include "/WEB-INF/freemarker/coursework/submit/formfields/disability.ftl" >
					</div>
				</#if>

				<#if features.privacyStatement>
					<@form.row>
						<label class="control-label">Privacy statement</label>
						<@form.field>
							<p class="privacy-field">
								The data on this form relates to your submission of
								coursework. The date and time of your submission, your
								identity and the work you have submitted will all be
								stored, but will not be used for any purpose other than
								administering and recording your coursework submission.
							</p>
						</@form.field>
					</@form.row>
				</#if>

				<#if assignment.displayPlagiarismNotice>
					<@form.row>
						<label class="control-label">Plagiarism declaration</label>
						<@form.field>
							<p class="plagiarism-field">
								In submitting my work I confirm that:
								<ol>
									<li>I have read the guidance on plagiarism/cheating provided in the handbook and understand the University regulations in relation to plagiarism/cheating. I am aware of the potential consequences of committing plagiarism/cheating. I declare that the work is all my own, except where I have stated otherwise.</li>
									<li>No substantial part(s) of the work submitted here has also been submitted by me in other assessments for accredited courses of study (other than in the case of a resubmission of a piece of work), and I acknowledge that if this has been done an appropriate reduction in the mark I might otherwise have received will be made.</li>
									<li>I understand that should this piece of work raise concerns requiring investigation in relation to points 1) and/or 2) above, it is possible that other work I have submitted for assessment will be checked, even if the marking process has been completed.</li>
									<li>Where a proof reader, paid or unpaid was used, I confirm that the proof reader was made aware of and has complied with the <a target='_blank' href='https://warwick.ac.uk/proofreadingpolicy'>University’s proof reading policy</a>.</li>
								</ol>
							</p>
						</@form.field>
					</@form.row>
				</#if>

				<@form.row>
					<label class="control-label">Assignment information</label>
					<@form.field>
						<div class="assignment-info-field">
							<#if !assignment.openEnded>
								<#assign time_remaining = durationFormatter(assignment.closeDate) />
								<p>
									The deadline for this assignment is <@fmt.date date=assignment.closeDate />,
									<span class="time-remaining">${time_remaining}</span>.

									<#if isExtended>
										<#assign extension_time_remaining = durationFormatter(extension.expiryDate) />

										You have an extension until <@fmt.date date=extension.expiryDate />,
										<span class="time-remaining">${extension_time_remaining}</span>.
									</#if>
								</p>

								<#if assignment.allowResubmission && (!assignment.closed || isExtended)>
									<p>
										You can submit to this assignment multiple times up to the deadline. Only
										the latest submission of your work will be accepted, and you will not be able
										to change this once the deadline has passed.
									</p>
								</#if>

								<#if assignment.allowLateSubmissions>
									<p>
									  You can submit<#if assignment.allowResubmission> once only</#if> to this assignment after the deadline, but your mark
									  may be affected.
									</p>
								</#if>
							<#else>
								<p>
									This assignment does not have a deadline.

									<#if assignment.allowResubmission>
										You can submit to this assignment multiple times, but only
										the latest submission of your work will be kept.
									</#if>
								</p>
							</#if>
						</div>
					</@form.field>
				</@form.row>

			</div>

			<div class="submit-buttons">
				<input class="btn btn-large btn-primary" type="submit" value="Submit">
				<#if willCheckpointBeCreated>
					<div class="alert alert-info" style="display: inline-block;">
						Submitting this assignment will mark a monitoring point as attended
					</div>
				</#if>
			</div>
		</@f.form>

		<script>
			jQuery(function($){
				$('form#submitAssignmentCommand').on('submit', function(){
					$.post('<@routes.coursework.submission_attempt assignment />')
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
