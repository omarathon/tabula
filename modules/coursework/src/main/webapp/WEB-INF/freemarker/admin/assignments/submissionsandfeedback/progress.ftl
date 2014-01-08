<#import "_submission_details.ftl" as sd />

<#escape x as x?html>
<div class="fixed-container">
	<div class="persist-header">
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

		<#include "_filter.ftl" />

		<#assign currentView = "summary" />
		<#include "_action-bar.ftl" />

		<div id="feedback-modal" class="modal fade"></div>
		<#if students??>
	</div>

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

	<table class="coursework-progress-table students table table-bordered table-striped tabula-greenLight sticky-table-headers ">
		<thead<#if students?size == 0> style="display: none;"</#if> class="persist-header">
			<tr>
				<th class="check-col" style="padding-right: 0px;"><input type="checkbox" class="collection-check-all"></th>
				<#if department.showStudentName>
					<th class="student-col">First name</th>
					<th class="student-col">Last name</th>
				<#else>
					<th class="student-col">University ID</th>
				</#if>

				<th class="progress-col">Progress</th>
				<th class="action-col">Next action</th>
			</tr>
		</thead>

	<#if students?size gt 0>
		<tbody>
			<#macro action actionCode student>
				<#local studentDetails><span data-profile="${student.user.warwickId}"><#if department.showStudentName>${student.user.fullName}<#else>${student.user.warwickId}</#if></span></#local>
				<#local firstMarker="first marker" />
				<#local secondMarker="second marker" />

				<#if student.coursework.enhancedSubmission??>
					<#if student.coursework.enhancedSubmission?? && student.coursework.enhancedSubmission.submission?? && student.coursework.enhancedSubmission.submission.assignment??>
						<#if student.coursework.enhancedSubmission.submission.firstMarker?has_content>
							<#local firstMarker><span data-profile="${student.coursework.enhancedSubmission.submission.firstMarker.warwickId!}">${student.coursework.enhancedSubmission.submission.firstMarker.fullName}</span></#local>
						</#if>

						<#if student.coursework.enhancedSubmission.submission.secondMarker?has_content>
							<#local secondMarker><span data-profile="${student.coursework.enhancedSubmission.submission.secondMarker.warwickId!}">${student.coursework.enhancedSubmission.submission.secondMarker.fullName}</span></#local>
						</#if>
					</#if>
				</#if>

				<#local text><@spring.message code=actionCode /></#local>

				<#noescape>${(text!"")?replace("[STUDENT]", studentDetails)?replace("[FIRST_MARKER]", firstMarker)?replace("[SECOND_MARKER]", secondMarker)}</#noescape>
			</#macro>

				<#macro stage stage>
					<#if stage.messageCode?default("")?length gt 0>
						<div class="stage<#if !stage.completed> incomplete<#if !stage.preconditionsMet> preconditions-not-met</#if></#if><#if stage.started && !stage.completed> current</#if>">
							<#if stage.completed>
								<#if stage.health.toString == 'Good'>
									<i class="icon-ok"></i>
								<#else>
									<i class="icon-remove"></i>
								</#if>
							<#else>
								<i class="icon-blank"></i>
							</#if>
							<@spring.message code=stage.messageCode /><#nested/>
						</div>
					</#if>
				</#macro>

				<#macro workflow student>
					<#if student.coursework.enhancedSubmission??>
						<#local enhancedSubmission=student.coursework.enhancedSubmission>
						<#local submission=enhancedSubmission.submission>
					</#if>
					<#if student.coursework.enhancedFeedback??>
						<#local enhancedFeedback=student.coursework.enhancedFeedback>
						<#local feedback=enhancedFeedback.feedback>
					</#if>
					<#if student.coursework.enhancedExtension??>
						<#local enhancedExtension=student.coursework.enhancedExtension>
						<#local extension=enhancedExtension.extension>
					</#if>

					<div class="workflow content">
						<#if student.stages?keys?seq_contains('Submission')>
							<div class="stage-group clearfix">
								<h3>Submission</h3>

								<div class="labels">
									<@sd.submission_status submission enhancedExtension enhancedFeedback />
								</div>

								<#-- If the current action is in this section, then add the next action blowout here -->
								<#if student.nextStage?? && ['Submission','DownloadSubmission']?seq_contains(student.nextStage.toString) && student.progress.messageCode != 'workflow.Submission.unsubmitted.failedToSubmit'>
									<div class="alert pull-right">
										<strong>Next action:</strong> <@action student.nextStage.actionCode student />
									</div>
								</#if>

								<@stage student.stages['Submission']><@sd.submission_details submission /></@stage>

								<#if student.stages?keys?seq_contains('DownloadSubmission')>
									<@stage student.stages['DownloadSubmission'] />
								</#if>
							</div>
						</#if>

						<#if student.stages?keys?seq_contains('CheckForPlagiarism')>
							<div class="stage-group clearfix">
								<h3>Plagiarism</h3>

								<div class="labels">
									<#if submission?? && submission.suspectPlagiarised>
										<i class="icon-exclamation-sign use-tooltip" title="Suspected of being plagiarised" data-container="body"></i>
									</#if>
								</div>

								<#-- If the current action is in this section, then add the next action blowout here -->
								<#if student.nextStage?? && ['CheckForPlagiarism']?seq_contains(student.nextStage.toString)>
									<div class="alert pull-right">
										<strong>Next action:</strong> <@action student.nextStage.actionCode student />
									</div>
								</#if>

								<@stage student.stages['CheckForPlagiarism']><#compress>
									<#if submission??>
										<#list submission.allAttachments as attachment>
											<!-- Checking originality report for ${attachment.name} ... -->
											<#if attachment.originalityReport??>
												: <@originalityReport attachment />
											</#if>
										</#list>
									</#if>
								</#compress></@stage>
							</div>
						</#if>

						<#if student.stages?keys?seq_contains('ReleaseForMarking')>
							<div class="stage-group clearfix">
								<h3>Marking</h3>

								<div class="labels">
									<#if submission?? && submission.assignment?? && submission.releasedForMarking>
										<span class="label label-success">Markable</span>
									</#if>
								</div>

								<#-- If the current action is in this section, then add the next action blowout here -->
								<#if student.nextStage?? && ['ReleaseForMarking','FirstMarking','SecondMarking']?seq_contains(student.nextStage.toString)>
									<div class="alert pull-right">
										<strong>Next action:</strong> <@action student.nextStage.actionCode student />
									</div>
								</#if>

								<@stage student.stages['ReleaseForMarking'] />

								<#if student.stages?keys?seq_contains('FirstMarking')>
									<#if submission?? && submission.assignment??>
										<#if submission.firstMarker?has_content>
											<#local firstMarker><span data-profile="${submission.firstMarker.warwickId!}">${submission.firstMarker.fullName}</span></#local>
										</#if>
									</#if>

									<@stage student.stages['FirstMarking']> <#compress>
										<#if firstMarker?default("")?length gt 0>(<#noescape>${firstMarker}</#noescape>)</#if>
									</#compress></@stage>
								</#if>

								<#if student.stages?keys?seq_contains('SecondMarking')>
									<#if submission?? && submission.assignment??>
										<#if submission.secondMarker?has_content>
											<#local secondMarker><span data-profile="${submission.secondMarker.warwickId!}">${submission.secondMarker.fullName}</span></#local>
										</#if>
									</#if>

									<@stage student.stages['SecondMarking']> <#compress>
										<#if secondMarker?default("")?length gt 0>(<#noescape>${secondMarker}</#noescape>)</#if>
									</#compress></@stage>
								</#if>
							</div>
						</#if>

						<#if student.stages?keys?seq_contains('AddMarks') || student.stages?keys?seq_contains('AddFeedback')>
							<div class="stage-group">
								<h3>Feedback</h3>

								<div class="labels">
									<#if enhancedFeedback?? && feedback??>
										<#if feedback.released>
											<#if enhancedFeedback.downloaded><span class="label label-success">Downloaded</span>
											<#else><span class="label label-info">Published</span>
											</#if>
										<#else><span class="label label-warning">Not yet published</span>
										</#if>
									</#if>
								<#if student.nextStage??><strong>Next action:</strong> <@action student.nextStage.actionCode student /></#if>
								</div>
							</#if>

							<#-- not really a stage but the best place to put a link to the feedback summary -->
							<#if feedback?? && !(feedback.placeholder)>
								<div class="stage">
									<i class="icon-eye-open"></i>
									<a href="<@routes.feedbackSummary assignment student.user.warwickId!''/>"
									   class="ajax-modal"
									   data-target="#feedback-modal">
										View feedback
									</a>

								<#-- If the current action is in this section, then add the next action blowout here -->
								<#if student.nextStage?? && ['AddMarks','AddFeedback','ReleaseFeedback','DownloadFeedback']?seq_contains(student.nextStage.toString)>
									<div class="alert pull-right">
										<strong>Next action:</strong> <@action student.nextStage.actionCode student />
									</div>
								</#if>

								<#if student.stages?keys?seq_contains('AddMarks')>
									<@stage student.stages['AddMarks']><#compress>
										<#if feedback?? && feedback.hasMarkOrGrade>
											: <#compress>
												<#if feedback.hasMark>
													${feedback.actualMark!''}<#if feedback.hasGrade>,</#if>
												</#if>
												<#if feedback.hasGrade>
													grade ${feedback.actualGrade!''}
												</#if>
											</#compress>
										</#if>
									</#compress></@stage>
								</#if>

								<#if student.stages?keys?seq_contains('AddFeedback')>
									<@stage student.stages['AddFeedback']><#compress>
										<#if feedback?? && (feedback.hasAttachments || feedback.hasOnlineFeedback)>: <#compress>
											<#local attachments=feedback.attachments />
											<#if attachments?size gt 0>
												<#if attachments?size == 1>
													<#local attachmentExtension = student.coursework.enhancedFeedback.feedback.attachments[0].fileExt>
												<#else>
													<#local attachmentExtension = "zip">
												</#if>
												<a class="long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/download/${student.coursework.enhancedFeedback.feedback.id}/feedback-${student.coursework.enhancedFeedback.feedback.universityId}.${attachmentExtension}'/>"><#compress>
													${attachments?size}
													<#if attachments?size == 1> file
													<#else> files
													</#if>
												</#compress></a>
											<#-- If the feedback was entered online there may not be attachments  -->
											<#elseif feedback?? && feedback.hasOnlineFeedback>
												Comments entered online
											</#if>
											<#if feedback.uploadedDate??><#compress>
													<@fmt.date date=feedback.uploadedDate seconds=true capitalise=true shortMonth=true />
											</#compress></#if>
										</#compress></#if>
									</#compress></@stage>
								</#if>

								<#if student.stages?keys?seq_contains('ReleaseFeedback')>
									<@stage student.stages['ReleaseFeedback']><#compress>
										<#if feedback?? && feedback.releasedDate??><#compress>
											: <@fmt.date date=feedback.releasedDate seconds=true capitalise=true shortMonth=true />
										</#compress></#if>
									</#compress>
									</@stage>
								</#if>

								<#if student.stages?keys?seq_contains('DownloadFeedback')>
									<@stage student.stages['DownloadFeedback'] />
								</#if>
							</div>
						</#if>
					</div>
				</#macro>

				<#macro row student>
					<tr data-contentid="${student.user.warwickId}" class="itemContainer<#if !student.coursework.enhancedSubmission??> awaiting-submission</#if>"<#if student.coursework.enhancedSubmission?? && student.coursework.enhancedSubmission.submission.suspectPlagiarised> data-plagiarised="true"</#if>>
						<td class="check-col"><#if student.coursework.enhancedSubmission?? || student.coursework.enhancedFeedback??><input type="checkbox" class="collection-checkbox" name="students" value="${student.user.warwickId}"></#if></td>
						<#if department.showStudentName>
							<td class="student-col toggle-cell"><h6 class="toggle-icon" data-profile="${student.user.warwickId}">${student.user.firstName}</h6></td>
							<td class="student-col toggle-cell"><h6 data-profile="${student.user.warwickId}">${student.user.lastName}</h6></td>
						<#else>
							<td class="student-col toggle-cell"><h6 class="toggle-icon" data-profile="${student.user.warwickId}">${student.user.warwickId}</h6></td>
						</#if>
						<td class="progress-col content-cell toggle-cell">
							<#if student.stages?keys?seq_contains('Submission') && student.nextStage?? && student.nextStage.toString != 'Submission' && student.stages['Submission'].messageCode != student.progress.messageCode>
								<#local progressTooltip><@spring.message code=student.stages['Submission'].messageCode />. <@spring.message code=student.progress.messageCode /></#local>
							<#else>
								<#local progressTooltip><@spring.message code=student.progress.messageCode /></#local>
							</#if>

							<dl class="progress progress-${student.progress.t} use-tooltip" title="${progressTooltip}" style="margin: 0; border-bottom: 0;" data-container="body">
								<dt class="bar" style="width: ${student.progress.percentage}%;"></dt>
								<dd style="display: none;" class="table-content-container" data-contentid="${student.user.warwickId}">
									<div id="content-${student.user.warwickId}" class="workflow-container content-container">
										<@workflow student />
									</div>
								</dd>
							</dl>
						</td>
						<td class="action-col">
							<#if student.nextStage??>
								<#if student.progress.messageCode == 'workflow.Submission.unsubmitted.failedToSubmit'>
									Failed to submit
								<#else>
									<@action student.nextStage.actionCode student />
								</#if>
							<#elseif student.progress.percentage == 100>
								Complete
							</#if>
						</td>
					</tr>
				</#macro>

				<#list students as student>
					<@row student />
				</#list>
			</tbody>
		</#if>
	</table>

	<#if students?size gt 0>
	<script type="text/javascript">
	(function($) {
		$('.fixed-container').fixHeaderFooter({minimumWindowHeightFix: 630});

		var options = {
			sortList: [<#if department.showStudentName>[3, 0], [2, 0]<#else>[2, 0], [1, 0]</#if>],
			headers: { 0: { sorter: false } },
			textExtraction: function(node) {
				var $el = $(node);
				if ($el.hasClass('progress-col')) {
					var width = $el.find('.bar').width();

					// Add differing amounts for info, success, warning and danger so that they are sorted in the right order.
					// info is the default, so == 0
					if ($el.find('.progress').hasClass('progress-success')) {
						width += 3;
					} else if ($el.find('.progress').hasClass('progress-warning')) {
						width += 2;
					} else if ($el.find('.progress').hasClass('progress-danger')) {
						width += 1;
					}

					return width;
				} else {
					return $el.text().trim();
				}
			}
		};

		$('.coursework-progress-table:not(.floatingHeadTable)').each(function() { $(this).expandingTable({ tableSorterOptions: options }); });
  		$(".floatingHeadTable").addClass("tablesorter");
	})(jQuery);
	</script>
	<#else>
		<#if submissionAndFeedbackCommand.filter.name == 'AllStudents'>
			<p>There are no submissions or feedbacks yet for this assignment.</p>
		<#else>
			<#macro filterDescription filter filterParameters><#compress>
				${filter.description}
				<#if filter.parameters?size gt 0>(<#compress>
					<#list filter.parameters as param>${param._1()}=${filterParameters[param._1()]}<#if param_has_next>, </#if></#list>
				</#compress>)</#if>
			</#compress></#macro>

			<p>There are no <@filterDescription submissionAndFeedbackCommand.filter submissionAndFeedbackCommand.filterParameters /> for this assignment.</p>
		</#if>
	</#if>

	<#else>
	</div>
	</#if>

</div>
</#escape>
