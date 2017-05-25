<#import "*/coursework_components.ftl" as components />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<#escape x as x?html>
	<#macro studentIdentifier user><#compress>
		<#if user.warwickId??>${user.warwickId}<#else>${user.userId!}</#if>
	</#compress></#macro>

	<div id="profile-modal" class="modal fade profile-subset"></div>
	<div id="feedback-modal" class="modal fade"></div>

	<div class="submission-feedback-results">
		<#if (results.students?size > 0)>
			<table id="submission-feedback-progress" class="cm2-progress-table submission-feedback-list table table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
				<thead class="fix-header">
					<tr>
						<th class="for-check-all"><input type="checkbox" class="collection-check-all" title="Select all/none" /> </th>
						<#if department.showStudentName>
							<th class="student-col sortable">First name</th>
							<th class="student-col sortable">Last name</th>
						</#if>

						<th class="student-col sortable">University ID</th>

						<th class="progress-col">Progress</th>
						<th class="action-col">Next action</th>
						<th></th>
					</tr>
				</thead>
				<tbody>
					<#macro details student>
						<#local coursework = student.coursework />
						<#if coursework.enhancedSubmission??>
							<#local enhancedSubmission = coursework.enhancedSubmission />
							<#local submission = enhancedSubmission.submission />
						</#if>
						<#if coursework.enhancedFeedback??>
							<#local enhancedFeedback = coursework.enhancedFeedback />
							<#local feedback = enhancedFeedback.feedback>
						</#if>
						<#if coursework.enhancedExtension??>
							<#local enhancedExtension=coursework.enhancedExtension />
						</#if>
						<#local identifier><@studentIdentifier student.user /></#local>
						<#local lateness><@components.lateness submission /></#local>

						<div class="details">
							<ul class="nav nav-tabs" role="tablist">
								<li role="presentation" class="active">
									<a href="#${identifier}-submission" aria-controls="${identifier}-submission" role="tab" data-toggle="tab">
										<#if assignment.collectSubmissions>
											Submission details
										<#else>
											Details
										</#if>
									</a>
								</li>

								<#-- One tab for each stage in the workflow; previous stages are active, incomplete stages are not -->
								<#if assignment.cm2MarkingWorkflow??>
									<#list assignment.cm2MarkingWorkflow.allStages as markingStage>
										<#local incomplete = feedback?? && (feedback.notReleasedToMarkers || !feedback.isMarkedByStage(markingStage)) />
										<li role="presentation"<#if incomplete> class="disabled"</#if>>
											<a<#if !incomplete> href="#${identifier}-${markingStage.name}" aria-controls="${identifier}-${markingStage.name}" role="tab" data-toggle="tab"</#if>>
												${markingStage.description}
											</a>
										</li>
									</#list>
								</#if>

								<li role="presentation">
									<a href="#${identifier}-audit" aria-controls="${identifier}-audit" role="tab" data-toggle="tab">
										<#if assignment.collectSubmissions>
											Submission history
										<#else>
											History
										</#if>
									</a>
								</li>
							</ul>
							<div class="tab-content tab-content-equal-height">
								<div role="tabpanel" class="tab-pane active" id="${identifier}-submission">
									<@components.student_workflow_details student />
								</div>

								<#-- One tab for each stage in the workflow; previous stages are active, incomplete stages are not -->
								<#if assignment.cm2MarkingWorkflow??>
									<#list assignment.cm2MarkingWorkflow.allStages as markingStage>
										<#local incomplete = feedback?? && (feedback.notReleasedToMarkers || !feedback.isMarkedByStage(markingStage)) />
										<div role="tabpanel" class="tab-pane" id="${identifier}-${markingStage.name}">
											<#local markerFeedback = mapGet(feedback.feedbackByStage, markingStage) />
											<@components.marker_feedback_summary markerFeedback markingStage />
										</div>
									</#list>
								</#if>

								<div role="tabpanel" class="tab-pane" id="${identifier}-audit">
									TODO
								</div>
							</div>
						</div>
					</#macro>

					<#macro row student>
						<#local coursework = student.coursework />
						<#if coursework.enhancedSubmission??>
							<#local enhancedSubmission = coursework.enhancedSubmission />
							<#local submission = enhancedSubmission.submission />
						</#if>
						<#if coursework.enhancedFeedback??>
							<#local enhancedFeedback = coursework.enhancedFeedback />
							<#local feedback = enhancedFeedback.feedback>
						</#if>
						<#if coursework.enhancedExtension??>
							<#local enhancedExtension=coursework.enhancedExtension />
						</#if>
						<#local identifier><@studentIdentifier student.user /></#local>
						<#local lateness><@components.lateness submission /></#local>

						<tr class="itemContainer<#if !enhancedSubmission??> awaiting-submission</#if>" <#if enhancedSubmission?? && submission.suspectPlagiarised> data-plagiarised="true"</#if> data-contentid="${identifier}">
							<td><@bs3form.selector_check_row "students" student.user.userId /></td>

							<#if department.showStudentName>
								<td class="student toggle-cell toggle-icon" data-profile="${identifier}">
									${student.user.firstName!}
								</td>
								<td class="student toggle-cell">${student.user.lastName!}</td>
							</#if>
							<td class="id toggle-cell">
								${identifier} <@pl.profile_link identifier />
							</td>

							<td class="progress-col content-cell toggle-cell">
								<dl style="margin: 0; border-bottom: 0;">
									<dt><@components.individual_stage_progress_bar student.stages?values /></dt>
									<dd style="display: none;" class="table-content-container" data-contentid="${identifier}">
										<div id="content-${identifier}" class="content-container" data-contentid="${identifier}">
											<@details student />
										</div>
									</dd>
								</dl>
							</td>
							<td class="action-col">
								<#if student.nextStage?has_content>
									<@spring.message code=student.nextStage.actionCode />
								</#if>
							</td>
							<td>
								<div class="btn-group">
									<a class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown">Actions <span class="caret"></span></a>
									<ul class="dropdown-menu pull-right">
										<#if submission??>
											<li>
												<#local download_url><@routes.cm2.submissionsZip assignment /></#local>
												<@fmt.permission_button
													permission='Submission.Read'
													scope=submission
													action_descr='download submission'
													classes='form-post-single'
													href=download_url
													tooltip='Download the submission files for this student as a ZIP file'>
														Download submission
												</@fmt.permission_button>
											</li>
											<li>
												<#local download_url><@routes.cm2.submissionsPdf assignment /></#local>
												<@fmt.permission_button
													permission='Submission.Read'
													scope=submission
													action_descr='download submission'
													classes='form-post-single'
													href=download_url
													tooltip='Download the submission files for this student as a PDF file'>
														Download submission as PDF
												</@fmt.permission_button>
											</li>
											<li>
												<#local deletesubmissionurl><@routes.cm2.deleteSubmissions assignment /></#local>
												<@fmt.permission_button
													permission='Submission.Delete'
													scope=submission
													action_descr='delete submission'
													classes='form-post-single'
													href=deletesubmissionurl
													tooltip='Delete this submission'>
														Delete submission
												</@fmt.permission_button>
											</li>
										</#if>

										<#-- TODO other actions -->
									</ul>
								</div>
							</td>
						</tr>
					</#macro>

					<#list results.students as student>
						<@row student />
					</#list>
				</tbody>
			</table>
		<#else>
			<p>There are no records for selected filter criteria.</p>
		</#if>
	</div>

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
			$('a.ajax-modal').ajaxModalLink();

			var $submissionFeedbackResultsTable = $(".submission-feedback-results table");
			$submissionFeedbackResultsTable.expandingTable();

			Coursework.initBigList();

			$('.submission-feedback-results').wideTables();
			$('.content-container').on('tabula.expandingTable.parentRowExpanded', function () {
				Coursework.equalHeightTabContent($(this));
			});

			// We probably just grew a scrollbar, so let's trigger a window resize
			$(window).trigger('resize.ScrollToFixed');


		})(jQuery);
	</script>
</#escape>