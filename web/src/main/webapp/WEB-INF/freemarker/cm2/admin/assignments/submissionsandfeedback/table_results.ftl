<#import "*/coursework_components.ftl" as components />
<#import "_submission_details.ftl" as sd />
<#escape x as x?html>


	<#if results.students??>
		<#function hasSubmissionOrFeedback students>
			<#local result = [] />
			<#list students as student>
				<#if student.coursework.enhancedSubmission?? || student.coursework.enhancedFeedback??>
					<#local result = result + [student] />
				</#if>
			</#list>
			<#return result />
		</#function>

		<#if !results.submissionFeedbackExists>
			<p>There are no submissions or feedback yet for this assignment.</p>
		<#else>
			<div class="submission-feedback-results">
				<#if (results.students?size > 0)>
					<table id="submission-feedback-info" class="table table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
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
							<#if assignment.cm2Assignment>
								<#assign submissionColspan=submissionColspan+results.workflowMarkers?size />
								<#list results.workflowMarkers as marker_col>
									<col class="${marker_col}" />
								</#list>
							<#else>
								<#assign submissionColspan=submissionColspan+2 />
								<col class="first-marker" />
								<col class="second-marker" />
							</#if>
						</#if>
					</colgroup>

					<#if results.hasOriginalityReport>
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
							<col class="feedback-mark" />
							<col class="feedback-grade" />
						</#if>
						<col class="viewFeedback" />
						<col class="status" />
					</colgroup>

					<thead>
					<tr>
						<th class="for-check-all sorter-false"><input  type="checkbox" class="collection-check-all" title="Select all/none" /> </th>
						<th class="sorter-false">Student</th>

						<th class="submission" colspan="${submissionColspan?c}">
							Submission
						</th>

						<#if results.hasOriginalityReport>
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
						<th class="submission">Status</th>
						<#if assignment.wordCountField??>
							<th class="submission" title="Declared word count">Words</th>
						</#if>
						<#if assignment.markingWorkflow??>
							<#if assignment.cm2Assignment>
								<#assign submissionColspan=submissionColspan+results.workflowMarkers?size />
								<#list results.workflowMarkers as marker_col>
									<th class="submission">${marker_col}</th>
								</#list>
							<#else>
								<th class="submission">First Marker</th>
								<th class="submission">Second Marker</th>
							</#if>
						</#if>


						<#if results.hasOriginalityReport>
							<th class="plagiarism">Report</th>
						</#if>

						<th class="feedback">Files</th>
						<th class="feedback1">Updated</th>
						<#if assignment.collectMarks>
							<th class="feedback">Mark</th>
							<th class="feedback">Grade</th>
						</#if>
						<th class="feedback">Summary</th>
						<th class="feedback">Status</th>
					</tr>
					</thead>

					<tbody>
						<#macro row submissionfeedbackinfo>
							<#local coursework=submissionfeedbackinfo.coursework>
							<#if coursework.enhancedSubmission??>
								<#local enhancedSubmission=coursework.enhancedSubmission>
								<#local submission=enhancedSubmission.submission>
							</#if>
							<#if coursework.enhancedFeedback??>
								<#local enhancedFeedback=coursework.enhancedFeedback>
							</#if>
							<#if coursework.enhancedExtension??>
								<#local enhancedExtension=coursework.enhancedExtension>
							</#if>
							<#local lateness><@sd.lateness submission /></#local>

						<tr class="itemContainer<#if !enhancedSubmission??> awaiting-submission</#if>" <#if enhancedSubmission?? && submission.suspectPlagiarised> data-plagiarised="true" </#if> >
							<td><@bs3form.selector_check_row "students" submissionfeedbackinfo.user.userId /></td>
							<td class="id">
								<#if module.adminDepartment.showStudentName>
									${submissionfeedbackinfo.user.fullName} <#if submissionfeedbackinfo.user.warwickId??><@pl.profile_link submissionfeedbackinfo.user.warwickId /><#else><@pl.profile_link submissionfeedbackinfo.user.userId /></#if>
								<#else>
									<#if submissionfeedbackinfo.user.warwickId??>${submissionfeedbackinfo.user.warwickId}<#else>${submissionfeedbackinfo.user.userId}</#if>
								</#if>
							</td>

							<td class="files">

								<#if submission??>
									<#local attachments=submission.allAttachments />
									<#if attachments?size gt 0>
										<#if attachments?size == 1>
											<#local filename = "${attachments[0].name}">
											<#local downloadUrl><@routes.cm2.downloadSubmission submission filename/>?single=true</#local>
										<#else>
											<#local filename = "submission-${submission.studentIdentifier}.zip">
											<#local downloadUrl><@routes.downloadSubmission submission filename/></#local>
										</#if>
										<a class="long-running" href="${downloadUrl}">
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
									<span class="date use-tooltip" title="${lateness!''}" data-container="body">
										<@fmt.date date=submission.submittedDate seconds=true capitalise=true shortMonth=true split=true />
										</span>
								</#if>
							</td>
							<td class="submission-status">
							<#-- Markable - ignore placeholder submissions -->
								<#if assignment.isReleasedForMarking(submissionfeedbackinfo.user.userId)>
									<span class="label label-info">Markable</span>
								</#if>
								<#if submission??>
								<#-- Downloaded -->
									<#if enhancedSubmission.downloaded>
										<span class="label label-info">Downloaded</span>
									</#if>
								<#-- Plagiarised -->
									<#if submission.suspectPlagiarised>
										<i class="icon-exclamation-sign use-tooltip" title="Suspected of being plagiarised"></i>
									<#elseif submission.investigationCompleted>
										<i class="icon-ok-sign use-tooltip" title="Plagiarism investigation completed"></i>
									</#if>
								</#if>
								<@sd.submission_status submission coursework.enhancedExtension coursework.enhancedFeedback submissionfeedbackinfo />
							</td>
							<#if assignment.wordCountField??>
								<td class="word-count">
									<#if submission?? && submission.valuesByFieldName[assignment.defaultWordCountName]??>
											${submission.valuesByFieldName[assignment.defaultWordCountName]?number}
										</#if>
								</td>
							</#if>


							<#if assignment.markingWorkflow??>

								<#if assignment.cm2Assignment>
									<#if enhancedFeedback??>
										<#local feedback=enhancedFeedback.feedback />
										<#list results.workflowMarkers as markerRole>
											<#local markerUser=feedback.feedbackMarkerByRole(markerRole) />
											<td>
												<#if markerUser??>
												${markerUser.fullName}
											</#if>
											</td>
										</#list>
									</#if>
								<#else>
									<td>
										<#if (assignment.getStudentsFirstMarker(student.user.userId)!"")?has_content>
											${assignment.getStudentsFirstMarker(student.user.userId).fullName}
										</#if>
									</td>
									<td>
										<#if (assignment.getStudentsSecondMarker(student.user.userId)!"")?has_content>
											${assignment.getStudentsSecondMarker(student.user.userId).fullName}
										</#if>
									</td>

								</#if>
							</#if>
							<#if results.hasOriginalityReport>
								<td class="originality-report">
									<#if submission??>
										<#list submission.allAttachments as attachment>
											<!-- Checking originality report for ${attachment.name} ... -->
											<#if attachment.originalityReportReceived>
												<@components.originalityReport attachment />
											</#if>
										</#list>
									</#if>
								</td>
							</#if>

							<td class="download">
								<#if enhancedFeedback??>
									<#local attachments=enhancedFeedback.feedback.attachments />
									<#if attachments?size gt 0>
										<#if attachments?size == 1>
											<#local attachmentExtension = enhancedFeedback.feedback.attachments[0].fileExt>
										<#else>
											<#local attachmentExtension = "zip">
										</#if>
										<a class="long-running" href="<@url page='/cm2/admin/assignments/${assignment.id}/feedback/download/${enhancedFeedback.feedback.id}/feedback-${enhancedFeedback.feedback.studentIdentifier}.${attachmentExtension}'/>">
										${attachments?size}
											<#if attachments?size == 1> file
											<#else> files
											</#if>
										</a>
									</#if>
								</#if>
							</td>
							<td class="uploaded">
								<#if enhancedFeedback?? && !enhancedFeedback.feedback.placeholder>
										<@fmt.date date=enhancedFeedback.feedback.updatedDate seconds=true capitalise=true shortMonth=true split=true />
									</#if>
							</td>

							<#if assignment.collectMarks>
								<td class="feedback-mark">
									<#if enhancedFeedback??>
										<#local feedback = enhancedFeedback.feedback>
									${(feedback.actualMark)!''}%
										<#if feedback.hasPrivateOrNonPrivateAdjustments>
											(Adjusted to - ${feedback.latestMark}%)
										</#if>
									</#if>
								</td>
								<td class="grade">
									<#if enhancedFeedback??>
										<#local feedback = enhancedFeedback.feedback>
									${(feedback.actualGrade)!''}
										<#if feedback.hasPrivateOrNonPrivateAdjustments && feedback.latestGrade??>
											(Adjusted to - ${feedback.latestGrade})
										</#if>
									</#if>
								</td>
							</#if>

							<td>
								<#if enhancedFeedback??>
									<a href="<@routes.cm2.feedbackSummary assignment submissionfeedbackinfo.user.userId/>"
										 class="ajax-modal"
										 data-target="#feedback-modal">
										View
									</a>
								</#if>
							</td>

							<td class="feedbackReleased">
								<#if enhancedFeedback??>
									<#if enhancedFeedback.feedback.released>
										<#if enhancedFeedback.downloaded><span class="label label-success">Downloaded</span>
										<#else><span class="label label-info">Published</span>
										</#if>
									<#else>
										<span class="label label-danger">Not yet published</span>
									</#if>
									<#assign queueSitsUploadEnabled=(features.queueFeedbackForSits && department.uploadCourseworkMarksToSits) />
									<#if queueSitsUploadEnabled>
										<#if enhancedFeedback.feedbackForSits??>
											<#assign feedbackSitsStatus=enhancedFeedback.feedbackForSits.status />
											<#assign sitsWarning = feedbackSitsStatus.dateOfUpload?has_content && feedbackSitsStatus.status.code != "uploadNotAttempted" && (
											(feedbackSitsStatus.actualMarkLastUploaded!0) != (student.enhancedFeedback.feedback.latestMark!0) || (feedbackSitsStatus.actualGradeLastUploaded!"") != (student.enhancedFeedback.feedback.latestGrade!"")
											) />
											<#if feedbackSitsStatus.code == "failed">
												<a href="<@routes.checkSitsUpload enhancedFeedback.feedback />" target="_blank">
														<span style="cursor: pointer;" class="label label-important use-tooltip" title="There was a problem uploading to SITS. Click to try and diagnose the problem.">
														${feedbackSitsStatus.description}
														</span><#--
													--></a>
											<#elseif sitsWarning>
												<span class="label label-important use-tooltip" title="The mark or grade uploaded differs from the current mark or grade. You will need to upload the marks to SITS again.">
												${feedbackSitsStatus.description}
													</span>
											<#elseif feedbackSitsStatus.code == "successful">
												<span class="label label-success">${feedbackSitsStatus.description}</span>
											<#else>
												<span class="label label-info">${feedbackSitsStatus.description}</span>
											</#if>
										<#else>
											<span class="label label-info">Not queued for SITS upload</span>
										</#if>
									</#if>
								</#if>
							</td>
						</tr>
						</#macro>

						<#list results.students as submissionfeedbackinfo>
							<@row submissionfeedbackinfo />
						</#list>
					</tbody>
				</table>
					<#assign users=[] />
					<#list results.students as student>
						<#assign users = users + [student.user] />
					</#list>
					<p><@fmt.bulk_email_students users /></p>
				<#else>
					<p>There are no records for selected filter criteria.</p>
				</#if>
		</div>
		</#if>

	</#if>

<script type="text/javascript">
	(function ($) {

			var $overlapPercentageCheckbox = $('.dropdown-menu.filter-list input[type=checkbox][value="PlagiarismStatuses.WithOverlapPercentage"]');
			var checked = $overlapPercentageCheckbox.is(':checked');
			var $percentageOverlapDiv = $('div.plagiarism-filter');
			if (checked) {
				$percentageOverlapDiv.show();
			} else {
				$percentageOverlapDiv.hide();
			}

		$('.fixed-container').fixHeaderFooter();

		$('.submission-table1').sortableTable({
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



		$submissionFeedbackResultsTable = $(".submission-feedback-results table");
		$submissionFeedbackResultsTable.tablesorter({
			headers: {
					0: { sorter: false },
					2: { sorter: false },
					3: { sorter: false },
					4: { sorter: false },
					5: { sorter: false }
			},
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
		$submissionFeedbackResultsTable.bigList();
		$('.submission-feedback-results').wideTables();
		// We probably just grew a scrollbar, so let's trigger a window resize
		$(window).trigger('resize.ScrollToFixed');


	})(jQuery);
</script>
</#escape>