<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/coursework_components.ftl" as components />
<#import "*/modal_macros.ftl" as modal />

<#macro markingTabSection order enhancedMarkerFeedbacks readOnly=true>
	<div class="marking-stage">
			<#if order.hasFeedback>
				<#if !readOnly>
					<#if order.headerStage.nextStagesDescription?has_content>
						<a class="btn btn-primary must-have-selected must-have-ready-next-stage form-post" href="${markingCompleted}">Confirm selected and send to ${order.headerStage.nextStagesDescription?lower_case}</a>
					</#if>
					<#if order.headerStage.canFinish(assignment.cm2MarkingWorkflow)>
						<a class="btn btn-primary must-have-selected must-have-ready-next-stage form-post" href="${finishMarking}">Confirm selected and send to admin</a>
					</#if>
					<#if features.bulkModeration && order.headerStage.allowsBulkAdjustments && assignment.module.adminDepartment.assignmentGradeValidation>
						<a class="btn btn-primary" data-toggle="modal" data-target="#bulk-adjustment-modal" href="<@routes.cm2.bulkModeration assignment order.headerStage marker />">Bulk moderate submissions</a>
					</#if>
				</#if>
				<div class="btn-group">
					<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
						Download <span class="caret"></span>
					</button>
					<ul class="dropdown-menu">
						<li class="must-have-selected">
							<a class="form-post" href="<@routes.cm2.downloadMarkerSubmissions assignment marker />">
								Download all selected submissions
							</a>
						</li>
						<li class="must-have-selected">
							<a class="download-pdf" data-target="#download-pdf-modal-${order.headerStage.name}" href="<@routes.cm2.downloadMarkerSubmissionsPdf assignment marker />">
								Download all selected submissions as pdf
							</a>
						</li>
						<#if features.feedbackTemplates && assignment.hasFeedbackTemplate>
							<li>
								<a class="use-tooltip" title="Download feedback templates for all students as a ZIP file." href="<@routes.cm2.markerTemplatesZip assignment marker/>" data-container="body">
									Download feedback templates
								</a>
							</li>
						</#if>
						<#list workflowType.allPreviousStages(order.headerStage) as pStage>
							<li class="must-have-selected">
								<a class="form-post" href="<@routes.cm2.downloadMarkerFeedbackStage assignment marker pStage />">Download feedback from ${pStage.description?lower_case}</a>
							</li>
						</#list>
					</ul>
				</div>
				<#if !readOnly>
					<div class="btn-group">
						<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
							Upload <span class="caret"></span>
						</button>
						<ul class="dropdown-menu">
							<li><a href="<@routes.cm2.markerUploadFeedback assignment marker />">Upload attachments</a></li>
							<li><a href="<@routes.cm2.markerUploadMarks assignment marker />">Upload marks & feedback</a></li>
						</ul>
					</div>
				</#if>
				<table class="table table-striped marking-table table-sortable<#if order.headerStage.summarisePreviousFeedback> preview-marks</#if>">
					<thead>
					<tr>
						<th class="check-col"><@bs3form.selector_check_all /></th>
						<th class="student-col sortable">
							<#if assignment.anonymity.equals(AssignmentAnonymity.FullyAnonymous)>
								ID
							<#else>
								University ID
							</#if>
						</th>
						<#if assignment.showSeatNumbers>
							<th class="student-col sortable">Seat number</th>
						</#if>
						<#if assignment.anonymity.equals(AssignmentAnonymity.NameAndID)>
							<th class="student-col sortable">First name</th>
							<th class="student-col sortable">Last name</th>
						</#if>
						<#if order.headerStage.summariseCurrentFeedback>
								<th class="sortable">Mark</th>
						</#if>
						<#if order.headerStage.summarisePreviousFeedback>
							<#list order.headerStage.previousStages as prevStage>
								<th class="sortable">${prevStage.allocationName}</th>
								<th class="sortable">${prevStage.allocationName} Mark</th>
							</#list>
						</#if>
						<th colspan="2">Progress</th>
					</tr>
					</thead>
					<tbody>
						<#list order.enhancedFeedbackByStage?keys as stage>
							<#assign maxPrevMarkerCols = 0 />

							<#list enhancedMarkerFeedbacks as emf>
								<#assign mf = emf.markerFeedback />
								<#assign student = mf.student />
								<#assign studentId><#if assignment.anonymity.equals(AssignmentAnonymity.FullyAnonymous)>${mf.feedback.anonymousId}<#else>${student.userId}</#if></#assign>
								<tr
									data-toggle="collapse"
									data-target="#${stage.name}-${studentId}"
									class="clickable collapsed expandable-row <#if mf.readyForNextStage>ready-next-stage</#if>"
								>
									<td class="check-col">
										<@bs3form.selector_check_row name="markerFeedback" value="${mf.id}" />
									</td>
									<td class="toggle-icon-large student-col">
										<#assign colspan = 4>
										<#if assignment.anonymity.equals(AssignmentAnonymity.FullyAnonymous)>
											Student${mf.feedback.anonymousId}
										<#else>
											${mf.feedback.studentIdentifier!""}
										</#if>
									</td>
									<#if assignment.showSeatNumbers>
										<#assign colspan = colspan + 1/>
										<td class="student-col">${assignment.getSeatNumber(student)!""}</td>
									</#if>
									<#if assignment.anonymity.equals(AssignmentAnonymity.NameAndID)>
										<#assign colspan = colspan + 2/>
										<td class="student-col">${student.firstName}</td>
										<td class="student-col">${student.lastName}&nbsp;<#if student.warwickId??><@pl.profile_link student.warwickId /><#else><@pl.profile_link student.userId /></#if></td>
									</#if>
									<#if order.headerStage.summariseCurrentFeedback>
										<td><#if mf.mark??>${mf.mark}</#if></td>
										<#assign colspan = colspan + 1>
									</#if>
									<#if order.headerStage.summarisePreviousFeedback>
										<#if emf.previousMarkerFeedback?has_content>
											<#list emf.previousMarkerFeedback as prevMf>
											<td><#if prevMf.marker??>${prevMf.marker.fullName}</#if></td>
											<td><#if prevMf.mark??>${prevMf.mark}</#if></td>
												<#assign colspan = colspan + 2>
												<#assign maxPrevMarkerCols = maxPrevMarkerCols + 2>
											</#list>
										<#elseif order.numPreviousMarkers(stage) gt 0>
											<td colspan="${order.numPreviousMarkers(stage) * 2}"></td>
										</#if>
									</#if>
									<td class="progress-col">
										<@components.individual_stage_progress_bar emf.workflowStudent.stages assignment student />
									</td>
									<td>
										<#if emf.workflowStudent.nextAction?has_content>
										<@components.workflowMessage emf.workflowStudent.nextAction assignment student />
										</#if>
									</td>
								</tr>
								<#assign detailUrl><@routes.cm2.markerOnlineFeedback assignment stage marker student /></#assign>
								<tr id="${stage.name}-${studentId}" data-detailurl="${detailUrl}" class="collapse detail-row">
									<td colspan="${colspan}" class="detailrow-container">
										<i class="fa fa-spinner fa-spin"></i> Loading
									</td>
								</tr>
							</#list>
						</#list>
					</tbody>
				</table>
				<#if !readOnly && order.headerStage.nextStagesDescription?has_content>
					<a class="btn btn-primary must-have-selected must-have-ready-next-stage form-post" href="${markingCompleted}">Confirm selected and send to ${order.headerStage.nextStagesDescription?lower_case}</a>
				</#if>
				<div id="download-pdf-modal-${order.headerStage.name}" class="modal fade">
					<@modal.wrapper>
						<@modal.header>
							<h3 class="modal-title">Download submissions as PDF</h3>
						</@modal.header>
						<@modal.body>
							<p>There are <span class="count"></span> submissions that have files that are not PDFs (shown below). The download will not include these files.</p>
							<p><a class="form-post btn btn-primary"
										data-href="<@routes.cm2.downloadMarkerSubmissionsPdf assignment marker />?download" href="">Download submissions as PDF</a>
							</p>
							<ul class="submissions"></ul>
						</@modal.body>
					</@modal.wrapper>
				</div>
			<#else>
				No students found
			</#if>
	</div>
</#macro>


<div>
	<#if feedbackByOrderAndStage?size gt 1>
		<ul class="nav nav-tabs" role="tablist" style="margin-top: 20px;margin-bottom: 20px;" id="markingStageTabs">
			<#list feedbackByOrderAndStage as order>
				<li role="presentation"<#if activeStage.name == order.headerStage.name> class="active"</#if>>
					<a href="#${order.headerStage.name}" role="tab" data-toggle="tab" data-stage="${order.headerStage.name}">${order.headerStage.description}</a>
				</li>
			</#list>
		</ul>
	</#if>
<div class="tab-content" id="markingTabContent">
<#list feedbackByOrderAndStage as order>
	<div class="tab-pane<#if activeStage.name == order.headerStage.name> active</#if>" role="tabpanel" id="${order.headerStage.name}">
		<#assign markingCompleted><@routes.cm2.markingCompleted assignment order.headerStage.order marker /></#assign>
		<#assign finishMarking><@routes.cm2.finishMarking assignment order.headerStage.order marker /></#assign>

		<#assign enhancedMarkerFeedbacks = mapGet(order.enhancedFeedbackByStage, order.headerStage)/>
		<#assign stage = order.headerStage />

		<div class="marking-tab-section">
			<h4>Submissions to ${stage.verb}</h4>

			<#if enhancedMarkerFeedbacks.readyToMark?size != 0>
				<p>These submissions are ready for you to ${stage.verb}.</p>

				<@markingTabSection order enhancedMarkerFeedbacks.readyToMark false />
			<#else>
				<p>No submissions are ready for you to ${stage.verb}.</p>
			</#if>
		</div>

		<div class="marking-tab-section">
			<h4>
				<a href="#" data-toggle="collapse" data-target="#${stage.name}-notReadyToMarkSubmissions">
					<i class="fa fa-fw fa-chevron-right"></i>Not ready to ${stage.verb} (${enhancedMarkerFeedbacks.notReadyToMark?size})
				</a>
			</h4>

			<div id="${stage.name}-notReadyToMarkSubmissions" class="marking-collapse collapse">
				<#if enhancedMarkerFeedbacks.notReadyToMark?size != 0>
					<p>These submissions are allocated to you for ${stage.presentVerb}, but haven't yet been released.</p>

					<@markingTabSection order enhancedMarkerFeedbacks.notReadyToMark true />
				<#else>
					<p>No submissions to show.</p>
				</#if>
			</div>
		</div>

		<div class="marking-tab-section">
			<h4>
				<a href="#" data-toggle="collapse" data-target="#${stage.name}-markedSubmissions">
					<i class="fa fa-fw fa-chevron-right"></i>${stage.pastVerb?cap_first} (${enhancedMarkerFeedbacks.marked?size})
				</a>
			</h4>

			<div id="${stage.name}-markedSubmissions" class="marking-collapse collapse">
				<#if enhancedMarkerFeedbacks.marked?size != 0>
					<p>You've finished ${stage.presentVerb} these submissions, so can't make any further changes.</p>

					<@markingTabSection order enhancedMarkerFeedbacks.marked true />
				<#else>
					<p>No submissions to show.</p>
				</#if>
			</div>
		</div>
	</div>
</#list>
	<div id="bulk-adjustment-modal" class="modal fade"></div>
	</div>
</div>

<script>
	jQuery(function ($) {
		$('.marking-tab-section > h4 > [data-toggle=collapse]').on('click', function(e) {
			e.preventDefault();
		});

		$('.marking-collapse').on('show.bs.collapse', function (e) {
			if (e.target === this) {
				$('[data-toggle=collapse][data-target="#' + e.target.id + '"]').children('.fa').removeClass('fa-chevron-right').addClass('fa-chevron-down');
			}
		});

		$('.marking-collapse').on('hide.bs.collapse', function (e) {
			if (e.target === this) {
				$('[data-toggle=collapse][data-target="#' + e.target.id + '"]').children('.fa').removeClass('fa-chevron-down').addClass('fa-chevron-right');
			}
		});

		$('#markingStageTabs').on('show.bs.tab', function (e) {
			$('#filtersActiveStage').val($(e.target).data('stage'));
		});

		$('#markingTabContent').on('shown.bs.collapse', function (e) {
			var tabId = $(e.target).closest('.tab-pane').attr('id');

			$(e.target).closest('.marking-collapse').collapse('show');

			$('#markingStageTabs').find('a[href="#' + tabId + '"]').tab('show');

			e.target.scrollIntoView();
		});

		$('#markingTabContent').on('click', 'a[data-toggle=modal]', function(e){
			e.preventDefault();
			var $this = $(this);
			var target = $this.attr('data-target');
			var url = $this.attr('href');
			$(target).load(url);
		});

		$("#bulk-adjustment-modal").on('submit', function(e){
			e.preventDefault();
			var $this = $(this);
			var $form = $this.find("form");
			jQuery.post($form.attr('action'), $form.serialize(), function(data){
				if(data.status === "successful") {
					$this.modal('toggle');
					window.location = data.redirect;
				} else {
					$this.html(data);
				}
			});
		});
		//# sourceURL=marker_feedback_list.ftl
	})
</script>
