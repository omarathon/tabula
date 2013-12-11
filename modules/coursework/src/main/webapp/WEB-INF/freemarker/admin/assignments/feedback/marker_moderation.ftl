<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>


<div class="well">
	<h3>First markers feedback</h3>
	<#assign feedback = firstMarkerFeedback />
	<#include "_feedback_summary.ftl">
</div>

<div class="feedback">
	<#if command.submission??>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</#if>
	<div class="form onlineFeedback">

		<#if isCompleted>
			<#include "_moderation_summary.ftl">
		<#else>
			<#assign submit_url>
				<@routes.markerModerationform assignment command.student.warwickId />
			</#assign>
			<@f.form cssClass="form-horizontal double-submit-protection" method="post" enctype="multipart/form-data"
				commandName="command" action="${submit_url}">

				<@form.row>
					<@form.field>
						<label class="radio inline">
							<@f.radiobutton path="approved" value="true" />
							Approve
						</label>
						<label class="radio inline">
							<@f.radiobutton cssClass="reject" path="approved" value="false" />
							Reject
						</label>
					</@form.field>
				</@form.row>

				<div class="rejection-fields" style="display:none;">

					<@form.labelled_row "rejectionComments" "Comments">
						<@f.textarea path="rejectionComments" cssClass="big-textarea" />
						<div class="help-block">
							Please add any comments about your rejection here.
						</div>
					</@form.labelled_row>

					<#if assignment.collectMarks>
						<@form.row>
							<@form.label path="mark">Adjusted Mark</@form.label>
							<@form.field>
								<div class="input-append">
									<@f.input path="mark" cssClass="input-small" />
									<span class="add-on">%</span>
								</div>
								<@f.errors path="mark" cssClass="error" />
							</@form.field>
						</@form.row>

						<@form.row>
							<@form.label path="grade">Adjusted Grade</@form.label>
							<@form.field>
								<@f.input path="grade" cssClass="input-small" />
								<@f.errors path="grade" cssClass="error" />
							</@form.field>
						</@form.row>
						<div class="control-group">
							<div class="controls">
								<span class="help-block">
									If you feel that a mark or grade other than the mark given is more appropriate you can include
									it here.
								</span>
	                        </div>
	                    </div>
					</#if>
				</div>

				<div class="submit-buttons">
					<input class="btn btn-primary" type="submit" value="Save">
					<a class="btn cancel-feedback" href="">Discard</a>
				</div>
			</@f.form>
		</#if>
	</div>
</div>
