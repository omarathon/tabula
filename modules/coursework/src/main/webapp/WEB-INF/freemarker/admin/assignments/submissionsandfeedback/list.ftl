<#escape x as x?html>
<h1>${assignment.name} (${assignment.module.code?upper_case})</h1>

<#assign module=assignment.module />

<#if hasPublishedFeedback>
	<p>
		<#if stillToDownload?has_content>
			<@fmt.p whoDownloaded?size "student has" "students have" /> downloaded their feedback to date <span class="subtle">(recent downloads may take a couple of minutes to show up.)</span>
			<@fmt.p stillToDownload?size "student" "students" /> still have not downloaded their feedback.
			<a id="tool-tip" class="btn btn-mini" data-toggle="button" href="#">
				<i class="icon-list"></i>
				List
			</a>
			<div id="tip-content" class="hide">
				<ul><#list stillToDownload as student>
					<li>
						<#if module.department.showStudentName>
							${student.fullName}
						<#else>
							${student.warwickId}
						</#if>
					</li>
				</#list></ul>
			</div>
			<script type="text/javascript">
				jQuery(function($){
					$("#tool-tip").popover({
						placement: 'right',
						html: true,
						content: function(){return $('#tip-content').html();},
						title: 'Students that haven\'t downloaded feedback'
					});
				});
			</script>
		<#else>
			All students have downloaded their feedback
		</#if>
	</p>
</#if>

<#macro originalityReport attachment>
<#local r=attachment.originalityReport />
<div>
${attachment.name}
<img src="<@url resource="/static/images/icons/turnitin-16.png"/>">
<span class="similarity-${r.similarity}">${r.overlap}% similarity</span>
<span class="similarity-subcategories">
(Web: ${r.webOverlap}%,
Student papers: ${r.studentOverlap}%,
Publications: ${r.publicationOverlap}%)
</span>
<div>
</#macro>


<#macro unSubmitted student extension="">
	<tr class="itemContainer awaiting-submission">
		<td></td>
		<td>
			<#if module.department.showStudentName>
				${student.fullName}
			<#else>
				${student.warwickId}
			</#if>
		</td>
		<td></td>
		<td>
			<#if extension?has_content>
				<#assign date>
					<@fmt.date date=extension.expiryDate capitalise=true />
				</#assign>
				<span class="label-blue">Unsubmitted</span>
				<span class="label-blue use-tooltip" title="${date}">Within Extension</span>
			<#else>
				<span class="label-blue">Unsubmitted</span>
			</#if>
		</td>
		<#if assignment.wordCountField??><td></td></#if>
		<#if assignment.markScheme??><td></td></#if>
		<#if assignment.collectMarks><td></td></#if>
		<td></td><td></td><td></td><td></td>
		<#if hasOriginalityReport><td></td></#if>
	</tr>
</#macro>


<#if students?size = 0>
	<p>There are no submissions or feedbacks yet for this assignment.</p>
<#else>

<!-- Extra junk that most people probably won't care about -->
<div class="btn-group" id="assignment-extra-dropdown" style="float:right">
	<a class="btn btn-mini dropdown-toggle" data-toggle="dropdown" href="#">
		<i class="icon-wrench"></i>
		Extra
		<span class="caret"></span>
	</a>
	<ul class="dropdown-menu pull-right">
		<li>
			<a title="Export submissions info as XML, for advanced users." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions.xml'/>">XML</a>
		</li>
		<li>
			<a title="Export submissions info as CSV, for advanced users." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions.csv'/>">CSV</a>
		</li>
	</ul>
</div>

<div class="btn-toolbar">
	<a class="btn long-running use-tooltip must-have-selected" title="Download the submission files for the selected students as a ZIP file." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions.zip'/>" id="download-selected-button"><i class="icon-download"></i>
		Download submissions
	</a>
	<a class="btn long-running use-tooltip must-have-selected" title="Download the feedback files for the selected students as a ZIP file." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedbacks.zip'/>" id="download-selected-button"><i class="icon-download"></i>
		Download feedback
	</a>
	<#if features.turnitin>
		<a class="btn" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/turnitin' />" id="turnitin-submit-button">Submit to Turnitin</a>
	</#if>
	<div class="btn-group">
		<a id="modify-selected" class="btn dropdown-toggle must-have-selected" data-toggle="dropdown" href="#">
			Update selected
			<span class="caret"></span>
		</a>
		<ul class="dropdown-menu">
			<li>
				<a class="use-tooltip" title="Toggle whether the selected students' submissions are possibly plagiarised." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/mark-plagiarised' />" id="mark-plagiarised-selected-button">Mark plagiarised</a>
			</li>
			<#if features.markSchemes && mustReleaseForMarking>
				<li>
					<a class="use-tooltip" title="Release the submissions for marking. First markers will be able to download their submissions from the app." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/release-submissions' />" id="release-submissions-button">Release for marking</a>
				</li>
			</#if>
			<li>
				<a class="" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/delete' />" id="delete-selected-button">Delete</a>
			</li>
		</ul>
	</div>
</div>

<div class="submission-feedback-list">
	<div class="clearfix">
		<@form.selector_check_all />
		<a class="btn btn-mini hide-awaiting-submission" href="#">
			<span class="hide-label" ><i class="icon-chevron-up"></i> Hide awaiting submission</span>
			<span class="show-label hide"><i class="icon-chevron-down"></i> Show awaiting submission</span>
		</a>
	</div>
	<table id="submission-table" class="table table-bordered table-striped">
		<tr>
			<th></th>
			<th>Student</th>
			<th>Submitted</th>
			<th>Submission status</th>
			<#if assignment.wordCountField??>
				<th title="Declared word count">Words</th>
			</#if>
			<#if assignment.markScheme??>
				<th>First Marker</th>
			</#if>
			<#if assignment.collectMarks>
				<th>Mark</th>
			</#if>
			<th>Files</th>
			<th>Feedback</th>
			<th>Feedback Uploaded</th>
			<th>Feedback status</th>
			<#if hasOriginalityReport><th>Originality report</th></#if>
		</tr>
		<#list awaitingSubmission?keys as universityId>
			<tr class="itemContainer awaiting-submission">
				<td></td>
				<td>				
				<#if module.department.showStudentName>
					${awaitingSubmission[universityId]}
				<#else>
					${universityId}
				</#if>
				</td>
				<td></td>
				<td><span class="label-blue">Unsubmitted</span></td>
				<#if assignment.wordCountField??><td></td></#if>
				<td></td>
				<#if assignment.collectMarks><td></td></#if>
				<td></td><td></td><td></td><td></td>
				<#if hasOriginalityReport><td></td></#if>
			</tr>
		</#list>
		<#list awaitingSubmission as student>
			<@unSubmitted student />
		</#list>
		<#list students as student>
			<#assign enhancedSubmission=student.enhancedSubmission>
			<#assign submission=enhancedSubmission.submission>
			
			<#if submission.submittedDate?? && (submission.late || submission.authorisedLate)>
				<#assign lateness = "${durationFormatter(assignment.closeDate, submission.submittedDate)} after close" />
			<#else>
				<#assign lateness = "" />
			</#if>
			
			<tr class="itemContainer" <#if submission.suspectPlagiarised> data-plagiarised="true" </#if> >
				<td><@form.selector_check_row "students" student.uniId /></td>
				<td class="id">
				<#if module.department.showStudentName>
					${student.fullName}
				<#else>
					${student.uniId}
				</#if>
				</td>
				<#-- TODO show student name if allowed by department --> 
				<td class="submitted">
					<#if submission.submittedDate??>
						<span class="date use-tooltip" title="${lateness!''}">
							<@fmt.date date=submission.submittedDate seconds=true capitalise=true />
						</span>
					</#if>
				</td>
				<td class="submission-status">
					<#if submission.late>
						<span class="label-red use-tooltip" title="${lateness!''}">Late</span>
					<#elseif  submission.authorisedLate>
						<span class="label-blue use-tooltip" title="${lateness!''}">Within Extension</span>
					</#if>
					<#if enhancedSubmission.downloaded>
						<span class="label-green">Downloaded</span>
					</#if>
					<#if submission.state?? && submission.state.toString == "ReleasedForMarking">
						<span class="label-green">Markable</span>
					</#if>
					<#if submission.suspectPlagiarised>
						<span class="label-orange">Suspect Plagiarised</span>
					</#if>
				</td>
				<#if assignment.wordCountField??>
					<td>
						<#if submission.valuesByFieldName[assignment.defaultWordCountName]??>
							${submission.valuesByFieldName[assignment.defaultWordCountName]?number}
						</#if>
					</td>
				</#if>
				<#if submission.assignment.markScheme??>
					<td>
						<#if submission.assignment??>${submission.firstMarker!""}</#if>
					</td>
				</#if>
				 <#if assignment.collectMarks>
					<td class="mark">
						${(student.feedback.actualMark)!''}
					</td>
				</#if>
				<td nowrap="nowrap" class="files">
					<#assign attachments=submission.allAttachments />
					<#if attachments?size gt 0>
					<a class="btn long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions/download/${submission.id}/submission-${submission.universityId}.zip'/>">
						<i class="icon-download"></i>
						${attachments?size}
						<#if attachments?size == 1> file
						<#else> files
						</#if>
					</a>
					</#if>
				</td>
				<td nowrap="nowrap" class="download">
					<#if student.feedback??>
						<#assign attachments=student.feedback.attachments />
						<#if attachments?size gt 0>
						<a class="btn long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/download/${student.feedback.id}/feedback-${student.feedback.universityId}.zip'/>">
							<i class="icon-download"></i>
							${attachments?size}
							<#if attachments?size == 1> file
							<#else> files
							</#if>
						</a>
						</#if>
					</#if>
				</td>
				<td class="uploaded"><#if student.feedback??><@fmt.date date=student.feedback.uploadedDate seconds=true capitalise=true /></#if></td>
				<td class="feedbackReleased">
					<#if student.feedback??>
						<#if student.feedback.released>Published
						<#else>Not yet published
						</#if>
					</#if>
				</td>
				<#if hasOriginalityReport>
					<td>
						<#list submission.allAttachments as attachment>
                    		<!-- Checking originality report for ${attachment.name} ... -->
                        	<#if attachment.originalityReport??>
                            	<@originalityReport attachment />
                        		<a target="turnitin-viewer" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/turnitin-report/${attachment.id}'/>">View report</a>
                        	</#if>
						</#list>
					</td>
				</#if>
			</tr>
		</#list>
	</table>
</div>
</#if>
</#escape>
