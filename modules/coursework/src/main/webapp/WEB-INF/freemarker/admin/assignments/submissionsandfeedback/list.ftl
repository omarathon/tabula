<#import "_submission_details.ftl" as sd />
<#import "/WEB-INF/freemarker/admin/_profile_link.ftl" as pl />
<#escape x as x?html>

<div id="feedback-modal" class="modal fade"></div>

<div class="fixed-container">
	<h1>${assignment.name} (${assignment.module.code?upper_case})</h1>

	<#if assignment.openEnded>
		<p class="dates">
			<@fmt.interval assignment.openDate />, never closes
			(open-ended)
			<#if !assignment.opened>
				<span class="label label-warning">Not yet open</span>
			</#if>
		</p>
	<#else>
		<p class="dates">
			<@fmt.interval assignment.openDate assignment.closeDate />
			<#if assignment.closed>
				<span class="label label-warning">Closed</span>
			</#if>
			<#if !assignment.opened>
				<span class="label label-warning">Not yet open</span>
			</#if>
		</p>
	</#if>

	<#assign module=assignment.module />
	<#assign department=module.department />

	<div class="fix-header pad-when-fixed">
		<#include "_filter.ftl" />
		<#assign currentView = "table" />
		<#include "_action-bar.ftl" />
	</div>

	<#if students??>
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

		<#function hasSubmissionOrFeedback students>
			<#local result = [] />
			<#list students as student>
				<#if student.coursework.enhancedSubmission?? || student.coursework.enhancedFeedback??>
					<#local result = result + [student] />
				</#if>
			</#list>
			<#return result />
		</#function>

		<#if hasSubmissionOrFeedback(students)?size = 0>
			<p>There are no submissions or feedbacks yet for this assignment.</p>
		</#if>

		<div class="submission-feedback-list">
			<table class="table table-bordered table-striped table-condensed submission-table">
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
						<#assign submissionColspan=submissionColspan+2 />
						<col class="first-marker" />
						<col class="second-marker" />
					</#if>
				</colgroup>

				<#if hasOriginalityReport>
					<colgroup class="plagiarism">
						<col class="report" />
					</colgroup>
				</#if>

				<colgroup class="feedback">
					<#assign feedbackColspan=4 />

					<col class="files" />
					<col class="uploaded" />
					<#if assignment.collectMarks>
						<#assign feedbackColspan=feedbackColspan+2 />
						<col class="mark" />
						<col class="grade" />
					</#if>
					<col class="viewFeedback" />
					<col class="status" />
				</colgroup>


				<thead class="fix-header">
					<tr>
						<th class="check-col student"><div class="check-all checkbox"><input type="checkbox" class="collection-check-all"></div></th>
						<th class="sortable student">Student</th>

						<th class="submission" colspan="${submissionColspan?c}">
							Submission
						</th>

						<#if hasOriginalityReport>
							<th class="plagiarism">Plagiarism</th>
						</#if>

						<th class="feedback" colspan="${feedbackColspan?c}">
							Feedback
						</th>
					</tr>
					<tr>
						<th class="student" colspan="2"></th>

						<th class="submission">Files</th>
						<th class="submission">Submitted</th>
						<th class="sortable submission">Status</th>
						<#if assignment.wordCountField??>
							<th class="sortable submission" title="Declared word count">Words</th>
						</#if>
						<#if assignment.markingWorkflow??>
							<th class="sortable submission">First Marker</th>
							<th class="sortable submission">Second Marker</th>
						</#if>

						<#if hasOriginalityReport>
							<th class="sortable plagiarism">Report</th>
						</#if>

						<th class="feedback">Files</th>
						<th class="feedback">Uploaded</th>
						<#if assignment.collectMarks>
							<th class="feedback">Mark</th>
							<th class="feedback">Grade</th>
						</#if>
						<th class="feedback">Summary</th>
						<th class="sortable feedback">Status</th>
					</tr>
				</thead>

				<tbody>
					<#macro row student>
						<#if student.coursework.enhancedSubmission??>
							<#local enhancedSubmission=student.coursework.enhancedSubmission>
							<#local submission=enhancedSubmission.submission>
						</#if>

						<#if submission?? && submission.submittedDate?? && (submission.late || submission.authorisedLate)>
							<#local lateness = "${durationFormatter(assignment.closeDate, submission.submittedDate)} after close" />
						<#else>
							<#local lateness = "" />
						</#if>

						<tr class="itemContainer<#if !enhancedSubmission??> awaiting-submission</#if>" <#if enhancedSubmission?? && submission.suspectPlagiarised> data-plagiarised="true" </#if> >
							<td><#if student.coursework.enhancedSubmission?? || student.coursework.enhancedFeedback??><@form.selector_check_row "students" student.user.warwickId /></#if></td>
							<td class="id">
							<#if module.department.showStudentName>
								${student.user.fullName} <@pl.profile_link student.user />
							<#else>
								${student.user.warwickId}
							</#if>
							</td>

							<td class="files">
								<#if submission??>
									<#local attachments=submission.allAttachments />
									<#if attachments?size gt 0>
										<#if attachments?size == 1>
											<#local filename = "${attachments[0].name}">
										<#else>
											<#local filename = "submission-${submission.universityId}.zip">
										</#if>
										<a class="long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions/download/${submission.id}/${filename}'/>">
											${attachments?size}
											<#if attachments?size == 1> file
											<#else> files
											</#if>
										</a>
									</#if>
								</#if>
							</td>
							<td class="submitted">
								<#if submission?? && submission.submittedDate??>
									<span class="date use-tooltip" title="${lateness!''}">
										<@fmt.date date=submission.submittedDate seconds=true capitalise=true shortMonth=true split=true />
									</span>
								</#if>
							</td>
							<td class="submission-status">
								<#if submission??>
									<#-- Downloaded -->
									<#if enhancedSubmission.downloaded>
										<span class="label label-success">Downloaded</span>
									</#if>

									<#-- Markable - ignore placeholder submissions -->
									<#if submission.assignment?? && submission.releasedForMarking>
										<span class="label label-success">Markable</span>
									</#if>

									<#-- Plagiarised -->
									<#if submission.suspectPlagiarised>
										<i class="icon-exclamation-sign use-tooltip" title="Suspected of being plagiarised"></i>
									<#elseif submission.investigationCompleted>
										<i class="icon-ok-sign use-tooltip" title="Plagiarism investigation completed"></i>
									</#if>
								</#if>
								<@sd.submission_status submission student.coursework.enhancedExtension student.coursework.enhancedFeedback />
							</td>
							<#if assignment.wordCountField??>
								<td class="word-count">
									<#if submission?? && submission.valuesByFieldName[assignment.defaultWordCountName]??>
										${submission.valuesByFieldName[assignment.defaultWordCountName]?number}
									</#if>
								</td>
							</#if>
							<#if assignment.markingWorkflow??>
								<td>
									<#if submission?? && submission.assignment?? && submission.firstMarker?has_content>
										${submission.firstMarker.fullName}
									</#if>
								</td>
								<td>
									<#if submission?? && submission.assignment?? && submission.secondMarker?has_content>
										${submission.secondMarker.fullName}
									</#if>
								</td>
							</#if>

							<#if hasOriginalityReport>
								<td class="originality-report">
									<#if submission??>
										<#list submission.allAttachments as attachment>
											<!-- Checking originality report for ${attachment.name} ... -->
											<#if attachment.originalityReport??>
												<@originalityReport attachment />
											</#if>
										</#list>
									</#if>
								</td>
							</#if>

							<td class="download">
								<#if student.coursework.enhancedFeedback??>
									<#local attachments=student.coursework.enhancedFeedback.feedback.attachments />
									<#if attachments?size gt 0>
									<#if attachments?size == 1>
										<#local attachmentExtension = student.coursework.enhancedFeedback.feedback.attachments[0].fileExt>
									<#else>
										<#local attachmentExtension = "zip">
									</#if>
									<a class="long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/download/${student.coursework.enhancedFeedback.feedback.id}/feedback-${student.coursework.enhancedFeedback.feedback.universityId}.${attachmentExtension}'/>">
										${attachments?size}
										<#if attachments?size == 1> file
										<#else> files
										</#if>
									</a>
									</#if>
								</#if>
							</td>
							<td class="uploaded">
								<#if student.coursework.enhancedFeedback?? && !student.coursework.enhancedFeedback.feedback.placeholder>
									<@fmt.date date=student.coursework.enhancedFeedback.feedback.uploadedDate seconds=true capitalise=true shortMonth=true split=true />
								</#if>
							</td>

							 <#if assignment.collectMarks>
								<td class="mark">
									${(student.coursework.enhancedFeedback.feedback.actualMark)!''}
								</td>
								<td class="grade">
									${(student.coursework.enhancedFeedback.feedback.actualGrade)!''}
								</td>
							</#if>

							<td>
								<#if student.coursework.enhancedFeedback??>
									<a href="<@routes.feedbackSummary assignment student.user.warwickId!''/>"
									   class="ajax-modal"
									   data-target="#feedback-modal">
										View
									</a>
								</#if>
							</td>

							<td class="feedbackReleased">
								<#if student.coursework.enhancedFeedback??>
									<#if student.coursework.enhancedFeedback.feedback.released>
										<#if student.coursework.enhancedFeedback.downloaded><span class="label label-success">Downloaded</span>
										<#else><span class="label label-info">Published</span>
										</#if>
									<#else><span class="label label-warning">Not yet published</span>
									</#if>
								</#if>
							</td>
						</tr>
					</#macro>

					<#list students as student>
						<@row student />
					</#list>
				</tbody>
			</table>

			<script type="text/javascript">
				(function($) {
					$('.fixed-container').fixHeaderFooter();

					$('.submission-table').sortableTable({
						textExtraction: function(node) {
							var $el = $(node);
							if ($el.hasClass('originality-report')) {
								var $tooltip = $el.find('.similarity-tooltip').first();
								if ($tooltip.length) {
									return $tooltip.text().substring(0, $tooltip.text().indexOf('%'));
								} else {
									return '0';
								}
							} else if ($el.hasClass('word-count')) {
								return $el.text().trim().replace(',','');
							} else {
								return $el.text().trim();
							}
						}
					});

				})(jQuery);
			</script>
		</div>
	</#if>
</div><!-- end fixed container -->
</#escape>
