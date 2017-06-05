<#import "*/cm2_macros.ftl" as cm2 />
<#assign commandName="command" />
<@spring.bind path=commandName>
	<#assign hasErrors=status.errors.allErrors?size gt 0 />
</@spring.bind>
<#assign verbed_your_noun="received your files"/>
<#assign isfile=RequestParameters.isfile/>
<#if isfile = "true">
	<#assign text_acknowledge="I've ${verbed_your_noun} and I found marks and feedback for"/>
	<#assign text_problems="However, there were some problems with its contents, which are shown below.
				You'll need to correct these problems with the spreadsheet and try again.
				If you choose to confirm without fixing the spreadsheet any rows with errors
				will be ignored."/>
	<#assign column_headings_warning="Remember that the first row in all spreadsheets is assumed to be column headings and ignored."/>
<#else>
	<#assign text_acknowledge="You are submitting marks for "/>
	<#assign text_problems="However, there were some problems, which are shown below.
				You'll need to return to the previous page, correct these problems and try again.
				If you choose to confirm without fixing the data any rows with errors
				will be ignored."/>
	<#assign column_headings_warning=""/>
</#if>

<#escape x as x?html>
<@cm2.assignmentHeader "Submit marks and feedback" assignment />
<div class="fix-area">
	<@f.form method="post" action=formUrl commandName=commandName>

		<@spring.bind path="marks">
			<#assign itemsList=status.actualValue />
			<#assign modifiedCount = 0 />
			<#list itemsList as item>
				<#if item.valid><#assign modifiedCount = modifiedCount + 1 /></#if>
			</#list>
			<p>
				<#if itemsList?size gt 0>
				${text_acknowledge} ${modifiedCount} students.
					<#if hasErrors>
					${text_problems}
					</#if>
				<#else>
					I've ${verbed_your_noun} but I couldn't find any rows that looked like
					marks. ${column_headings_warning}
				</#if>
			</p>
		</@spring.bind>

		<@spring.bind path="marks">
			<#assign itemList=status.actualValue />
			<#if itemList?size gt 0>
				<table class="table table-bordered upload-marks-preview">
					<tr>
						<th>ID</th>
						<th>Marks</th>
						<th>Grade</th>
						<th>Feedback</th>
					</tr>
					<#list itemList as item>
						<@spring.nestedPath path="marks[${item_index}]">
							<tr>
								<@f.hidden path="id" />
								<@f.hidden path="actualMark" />
								<@f.hidden path="actualGrade" />
								<@f.hidden path="feedbackComment" />
								<@f.hidden path="valid" />
								<td>
										<@spring.bind path="id">${status.value}</@spring.bind>
										<@f.errors path="id" cssClass="error" />
										<#if item.modified>
											<span class="warning">Feedback and/or marks have already been uploaded for this student. This will be overwritten when you click confirm</span>
									</#if>
								</td>
								<td>
									<@spring.bind path="actualMark">${status.value}</@spring.bind>
									<@f.errors path="actualMark" cssClass="error" />
								</td>
								<td>
									<@spring.bind path="actualGrade">${status.value}</@spring.bind>
									<@f.errors path="actualGrade" cssClass="error" />
								</td>
								<td>
									<@spring.bind path="feedbackComment">${status.value}</@spring.bind>
									<@f.errors path="feedbackComment" cssClass="error" />
								</td>
							</tr>
						</@spring.nestedPath>
					</#list>
				</table>
			</#if>
		</@spring.bind>

		<div class="submit-buttons form-actions fix-footer">
			<input type="hidden" name="confirm" value="true">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<a class="btn btn-default cancel" href="${cancelUrl}">Cancel</a>
		</div>
	</@f.form>
</div>

</#escape>