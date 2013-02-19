<#escape x as x?html>
<h1>${assignment.name} (${assignment.module.code?upper_case})</h1>

<#assign module=assignment.module />
<#assign department = module.department />

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
							${student.uniId}
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

			<span id="tool-tip-${attachment.id}" class="similarity-${r.similarity} similarity-tooltip">${r.overlap}% similarity</span>
      <div id="tip-content-${attachment.id}" class="hide">
				<p>${attachment.name} <img src="<@url resource="/static/images/icons/turnitin-16.png"/>"></p>
				<p class="similarity-subcategories-tooltip">
					Web: ${r.webOverlap}%<br>
					Student papers: ${r.studentOverlap}%<br>
					Publications: ${r.publicationOverlap}%
				</p>
				<p>
					<a target="turnitin-viewer" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/turnitin-report/${attachment.id}'/>">View full report</a>
				</p>
      </div>
      <script type="text/javascript">
        jQuery(function($){
          $("#tool-tip-${attachment.id}").popover({
            placement: 'right',
            html: true,
            content: function(){return $('#tip-content-${attachment.id}').html();},
            title: 'Turnitin report summary'
          });
        });
      </script>

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
		<td></td>
		<td>
			<#if extension?has_content>
				<#assign date>
					<@fmt.date date=extension.expiryDate capitalise=true shortMonth=true />
				</#assign>
				<span class="label label-info">Unsubmitted</span>
				<span class="label label-info use-tooltip" title="${date}">Within Extension</span>
			<#else>
				<span class="label label-info">Unsubmitted</span>
			</#if>
		</td>
		<#if assignment.wordCountField??><td></td></#if>
		<#if assignment.markingWorkflow??><td></td></#if>
		
		<#if hasOriginalityReport><td></td></#if>
		
		<td></td><td></td>
		<#if assignment.collectMarks><td></td></#if>
		<td></td>
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
	<#if features.turnitin && department.plagiarismDetectionEnabled>
		<a class="btn" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/turnitin' />" id="turnitin-submit-button">Check for Plagiarism</a>
	</#if>
	<div class="btn-group">
		<a id="modify-selected" class="btn dropdown-toggle must-have-selected" data-toggle="dropdown" href="#">
			Update selected
			<span class="caret"></span>
		</a>
		<ul class="dropdown-menu">
			<#if department.plagiarismDetectionEnabled>
				<li>
					<a class="use-tooltip" title="Toggle whether the selected students' submissions are possibly plagiarised." href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/mark-plagiarised' />" id="mark-plagiarised-selected-button">Mark plagiarised</a>
				</li>
			</#if>
			<#if features.markingWorkflows && mustReleaseForMarking>
				<li>
					<a class="use-tooltip form-post"
					   title="Release the submissions for marking. First markers will be able to download their submissions from the app."
					   href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/release-submissions' />"
					   id="release-submissions-button">
						Release for marking
					</a>
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
		<colgroup class="student">
			<col class="checkbox" />
			<col class="student-info" />
		</colgroup>
		
		<colgroup class="submission">
			<col class="files" />
			<col class="submitted" />
			<col class="status" />
			<#assign submissionColspan=3 />
			
			<#if assignment.wordCountField??>
				<#assign submissionColspan=submissionColspan+1 />
				<col class="word-count" />
			</#if>
			<#if assignment.markingWorkflow??>
				<#assign submissionColspan=submissionColspan+1 />
				<col class="first-marker" />
			</#if>
		</colgroup>
		
		<#if hasOriginalityReport>
			<colgroup class="plagiarism">
				<col class="report" />
			</colgroup>
		</#if>
		
		<colgroup class="feedback">
			<#assign feedbackColspan=3 />
		
			<col class="files" />
			<col class="uploaded" />
			<#if assignment.collectMarks>
				<#assign feedbackColspan=feedbackColspan+1 />
				<col class="mark" />
			</#if>
			<col class="status" />
		</colgroup>	
		<thead>
			<tr>
				<th></th>
				<th class="sortable">Student</th>
				
				<th colspan="${submissionColspan?c}">
					Submission
				</th>
				
				<#if hasOriginalityReport>
					<th>Plagiarism</th>
				</#if>
				
				<th colspan="${feedbackColspan?c}">
					Feedback
				</th>
			</tr>
			<tr>
				<th colspan="2"></th>
				
				<th>Files</th>
				<th>Submitted</th>
				<th class="sortable">Status</th>
				<#if assignment.wordCountField??>
					<th class="sortable" title="Declared word count">Words</th>
				</#if>
				<#if assignment.markingWorkflow??>
					<th class="sortable">First Marker</th>
				</#if>
				
				<#if hasOriginalityReport>
					<th class="sortable">Report</th>
				</#if>
				
				<th>Files</th>
				<th>Uploaded</th>
				<#if assignment.collectMarks>
					<th>Mark</th>
				</#if>
				<th class="sortable">Status</th>
			</tr>
		</thead>
		<tbody>
			<#list awaitingSubmissionExtended as pair>
				<#assign student=pair._1 />
				<#assign extension=pair._2 />
				<@unSubmitted student extension />
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
					
					<td class="files">
						<#assign attachments=submission.allAttachments />
						<#if attachments?size gt 0>
						<a class="long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions/download/${submission.id}/submission-${submission.universityId}.zip'/>">
							${attachments?size}
							<#if attachments?size == 1> file
							<#else> files
							</#if>
						</a>
						</#if>
					</td>
					<td class="submitted">
						<#if submission.submittedDate??>
							<span class="date use-tooltip" title="${lateness!''}">
								<@fmt.date date=submission.submittedDate seconds=true capitalise=true shortMonth=true split=true />
							</span>
						</#if>
					</td>
					<td class="submission-status">
						<#if submission.late>
							<span class="label label-important use-tooltip" title="${lateness!''}">Late</span>
						<#elseif  submission.authorisedLate>
							<span class="label label-info use-tooltip" title="${lateness!''}">Within Extension</span>
						</#if>
						<#if enhancedSubmission.downloaded>
							<span class="label label-success">Downloaded</span>
						</#if>
						<#if submission.state?? && submission.state.toString == "ReleasedForMarking">
							<span class="label label-success">Markable</span>
						</#if>
						<#if submission.suspectPlagiarised>
							<i class="icon-exclamation-sign use-tooltip" title="Suspected of being plagiarised"></i>
						</#if>
					</td>
					<#if assignment.wordCountField??>
						<td>
							<#if submission.valuesByFieldName[assignment.defaultWordCountName]??>
								${submission.valuesByFieldName[assignment.defaultWordCountName]?number}
							</#if>
						</td>
					</#if>
					<#if assignment.markingWorkflow??>
						<td>
							<#if submission.assignment??>${submission.firstMarker!""}</#if>
						</td>
					</#if>
					
					<#if hasOriginalityReport>
						<td class="originality-report">
							<#list submission.allAttachments as attachment>
								<!-- Checking originality report for ${attachment.name} ... -->
								<#if attachment.originalityReport??>
									<@originalityReport attachment />
								</#if>
							</#list>
						</td>
					</#if>
					
					<td class="download">
						<#if student.enhancedFeedback??>
							<#assign attachments=student.enhancedFeedback.feedback.attachments />
							<#if attachments?size gt 0>
							<a class="long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/download/${student.enhancedFeedback.feedback.id}/feedback-${student.enhancedFeedback.feedback.universityId}.zip'/>">
								${attachments?size}
								<#if attachments?size == 1> file
								<#else> files
								</#if>
							</a>
							</#if>
						</#if>
					</td>
					<td class="uploaded"><#if student.enhancedFeedback??><@fmt.date date=student.enhancedFeedback.feedback.uploadedDate seconds=true capitalise=true shortMonth=true split=true /></#if></td>
					
					 <#if assignment.collectMarks>
						<td class="mark">
							${(student.enhancedFeedback.feedback.actualMark)!''}
						</td>
					</#if>
					<td class="feedbackReleased">
						<#if student.enhancedFeedback??>
							<#if student.enhancedFeedback.feedback.released>
								<#if student.enhancedFeedback.downloaded><span class="label label-success">Downloaded</span>
								<#else><span class="label label-info">Published</span>
								</#if>
							<#else><span class="label label-warning">Not yet published</span>
							</#if>
						</#if>
					</td>
				</tr>
			</#list>
		</tbody>
	</table>
	<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
	<script type="text/javascript">
		(function($) {
			$("#submission-table").sortableTable();
		})(jQuery);
	</script>
</div>
</#if>
</#escape>
