<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "feedback/_feedback_summary.ftl" as fs>
<#escape x as x?html>
	<h1>Feedback audit</h1>
	<h5>
		<span class="muted">for</span>
		${assignment.name} (${assignment.module.code?upper_case})
	</h5>

	<h4>Student - ${student.fullName} (${student.warwickId}) <@pl.profile_link student.warwickId! /></h4>
	<#assign submission=auditData.submission />
	<#include "feedback/_submission_summary.ftl">

	<#list auditData.feedback.allMarkerFeedback as feedback>
		<#if feedback??>
			<div class="well">
				<@fs.feedbackSummary feedback isModerated true/>
				<@fs.secondMarkerNotes feedback isModerated />
			</div>
		</#if>
	</#list>

	<#if auditData.feedback??>
		<#assign feedback = auditData.feedback />
		<#assign isSelf = false />

		<#if feedback.hasAdjustments??>
			<div class="well">
				<div class="feedback-summary-heading">
					<h3>Adjustments</h3>
				</div>
				<div class="feedback-summary">
					<div class="feedback-details">
						<div class="mark-grade" >
							<div>
								<#if feedback.adjustedMark?has_content>
									<div class="mg-label">Adjusted Mark:</div>
									<div>
										<span class="mark">${feedback.adjustedMark!""}</span>
										<span>%</span>
									</div>
								</#if>
								<#if feedback.adjustedGrade?has_content>
									<div class="mg-label" >Adjusted Grade:</div>
									<div>
										<span class="grade">${feedback.adjustedGrade!""}</span>
									</div>
								</#if>
							</div>
						</div>
						<#if feedback.adjustmentReason?has_content>
							<div class="mark-grade" >
								<div>
									<div class="mg-label">Reason for adjustment: </div>
									<div>
										<span>${feedback.adjustmentReason!""}</span>
									</div>
								</div>
							</div>
						</#if>
						<#if feedback.adjustmentComments?has_content>
							<div class="feedback-comments">
								<h5>Adjustment comments</h5>
								<p>${feedback.adjustmentComments!""}</p>
							</div>
						</#if>
					</div>
				</div>
			</div>
		</#if>

		<div class="well">
			<div class="feedback-summary-heading">
				<h3>Feedback delivered to student</h3>
				<h5>Published on <@fmt.date feedback.releasedDate /></h5>
			</div>
			<#include "../../submit/_assignment_feedbackdownload.ftl" />
		</div>
	</#if>

	<div id="profile-modal" class="modal fade profile-subset"></div>
</#escape>
