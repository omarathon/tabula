<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/coursework_components.ftl" as components />
<#list feedbackByStage?keys as stage>
	<#assign markingCompleted><@routes.cm2.markingCompleted assignment stage marker /></#assign>
	<#assign enhancedMarkerFeedbacks = mapGet(feedbackByStage, stage)/>
	<div class="marking-stage">
		<h3>${stage.description}</h3>
		<#if enhancedMarkerFeedbacks?has_content>
			<#if stage.nextStagesDescription?has_content>
				<a class="btn btn-primary must-have-selected form-post" href="${markingCompleted}">Confirm selected and send to ${stage.nextStagesDescription?lower_case}</a>
			</#if>
			<div class="btn-group">
				<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
					Download <span class="caret"></span>
				</button>
				<ul class="dropdown-menu">
					<li><a class="form-post" href="<@routes.cm2.downloadMarkerSubmissions assignment marker />">Download all selected submissions</a></li>
					<li><a href="#">Download all selected submissions as pdf</a></li>
					<#if features.feedbackTemplates && assignment.hasFeedbackTemplate>
						<li>
							<a class="btn use-tooltip" title="Download feedback templates for all students as a ZIP file." href="<@routes.cm2.markerTemplatesZip assignment />" data-container="body">
								Download feedback templates
							</a>
						</li>
					</#if>
					<#list workflowType.allPreviousStages(stage) as pStage>
						<li><a href="#">Download feedback from ${pStage.description?lower_case}</a></li>
					</#list>
				</ul>
			</div>

			<div class="btn-group">
				<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
					Upload <span class="caret"></span>
				</button>
				<ul class="dropdown-menu">
					<li><a href="<@routes.cm2.markerUploadFeedback assignment stage marker />">Upload attachments</a></li>
					<li><a href="<@routes.cm2.markerUploadMarks assignment stage marker />">Upload marks & feedback</a></li>
				</ul>
			</div>

			<table class="table table-striped marking-table">
				<thead>
				<tr>
					<th class="check-col"><@bs3form.selector_check_all /></th>
					<#if assignment.anonymousMarking>
						<th class="student-col">ID</th>
					<#else>
						<th class="student-col">University ID</th>
						<th class="student-col">First name</th>
						<th class="student-col">Last name</th>
					</#if>
					<th colspan="2">Progress</th>
				</tr>
				</thead>
				<tbody>
					<#list enhancedMarkerFeedbacks as emf>
						<#assign mf = emf.markerFeedback />
						<#assign student = mf.student />
						<#assign studentId><#if assignment.anonymousMarking>${mf.feedback.anonymousId}<#else>${student.userId}</#if></#assign>
					<tr
						data-toggle="collapse"
						data-target="#${stage.name}-${studentId}"
						class="clickable collapsed expandable-row <#if mf.readyForNextStage>ready-next-stage</#if>"
					>
						<td class="check-col">
							<@bs3form.selector_check_row name="markerFeedback" value="${mf.id}" />
						</td>
						<#if assignment.anonymousMarking>
							<#assign colspan = 4>
							<td class="toggle-icon-large student-col"><span class=""></span>Student${mf.feedback.anonymousId}</td>
						<#else>
							<#assign colspan = 6>
							<td class="toggle-icon-large student-col">${mf.feedback.studentIdentifier!""}</td>
							<td class="student-col">${student.firstName}</td>
							<td class="student-col">${student.lastName}&nbsp;<#if student.warwickId??><@pl.profile_link student.warwickId /><#else><@pl.profile_link student.userId /></#if></td>
						</#if>
						<td class="progress-col">
							<@components.individual_stage_progress_bar emf.workflowStudent.stages/>
						</td>
						<td>
							<#if emf.workflowStudent.nextAction?has_content>
											<@spring.message code=emf.workflowStudent.nextAction />
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
				</tbody>
			</table>

			<#if feedbackByStage?keys?size gt 0>
				<#if stage.nextStagesDescription?has_content>
					<a class="btn btn-primary must-have-selected form-post" href="${markingCompleted}">Confirm selected and send to ${stage.nextStagesDescription?lower_case}</a>
				</#if>
			</#if>
		<#else>
			No students found
		</#if>
	</div>
</#list>