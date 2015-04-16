<#import "*/courses_macros.ftl" as courses_macros />

<#function markingId user>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId! />
	</#if>
</#function>

<div class="content feedback-adjustment feedback-summary">

<#if command.feedback?? && command.feedback.latestPrivateOrNonPrivateAdjustment?has_content>
	<div class="well">
		<h3>Latest adjustment</h3>
		<p>
			Adjusted mark - ${command.feedback.latestPrivateOrNonPrivateAdjustment.mark!}<br>
			Adjusted grade - ${command.feedback.latestPrivateOrNonPrivateAdjustment.grade!}<br>
		</p>
	</div>
</#if>

<#assign submit_url>
	<@routes.examFeedbackAdjustmentForm exam markingId(command.student) />
</#assign>

<@f.form id="adjust-form-${markingId(command.student)}"
	cssClass="form-horizontal double-submit-protection"
	method="post"
	commandName="command"
	action="${submit_url}"
>

	<@form.row>
		<@form.label path="reason">Reason for adjustment</@form.label>
		<@form.field>
			<@f.input type="text" path="reason" placeholder="Enter your reason" />
			<@f.errors path="reason" cssClass="error" />
		</@form.field>
	</@form.row>

	<@form.row>
		<@form.label path="comments">Adjustment comments</@form.label>
		<@form.field>
			<@f.textarea path="comments" cssClass="big-textarea" />
			<@f.errors path="comments" cssClass="error" />
		</@form.field>
	</@form.row>

	<@form.row>
		<@form.label path="adjustedMark">Adjusted mark</@form.label>
		<@form.field>
			<div class="input-append">
				<@f.input path="adjustedMark" cssClass="input-small" />
				<span class="add-on">%</span>
			</div>
			<@f.errors path="adjustedMark" cssClass="error" />
		</@form.field>
	</@form.row>

	<@form.row>
		<#if isGradeValidation>
			<@courses_macros.autoGradeOnline "adjustedGrade" "Adjusted grade" "adjustedMark" markingId(command.student) exam true />
		<#else>
			<@form.label path="adjustedGrade">Adjusted grade</@form.label>
			<@form.field>
				<@f.input path="adjustedGrade" cssClass="input-small" />
				<@f.errors path="adjustedGrade" cssClass="error" />
			</@form.field>
		</#if>
	</@form.row>

	<div class="submit-buttons">
		<input class="btn btn-primary" type="submit" value="Save">
		<a class="btn discard-changes" href="">Cancel</a>
	</div>

</@f.form>

	<script>
		jQuery(function($){
			Exams.prepareAjaxForm($('#adjust-form-${markingId(command.student)}'), function(resp) {
				var $resp = $(resp);
				// there should be an ajax-response class somewhere in the response text
				var $response = $resp.find('.ajax-response').andSelf().filter('.ajax-response');
				var success = $response.length && $response.data('status') == 'success';

				if (success) {
					return "";
				} else {
					return resp;
				}
			});
		});
	</script>
</div>

