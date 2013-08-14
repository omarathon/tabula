<#escape x as x?html>

<#-- This might end up being AJAXically loaded into the top of the main screen -->

<h1>New monitoring point set for ${command.route.name}</h1>

<@f.form action="" method="POST" commandName="command">
	<@f.hidden path="route" />
	<@f.errors path="route" cssClass="error" />

	<@form.labelled_row "year" "Year of study">
		<@f.select path="year">
			<#list 1..8 as year>
			<@f.option value="${year}" />
			</#list>
		</@f.select>
	</@form.labelled_row>

	<@form.labelled_row "templateName" "Template name">
		<@f.select path="templateName">
			<@f.option value="1st year Undergraduate" />
			<@f.option value="2nd year Undergraduate" />
			<@f.option value="3rd year Undergraduate" />
			<@f.option value="4th year Undergraduate" />
			<@f.option value="Postgraduate research" />
			<@f.option value="Postgraduate taught" />
			<@f.option value="" label="Other&hellip;" htmlEscape="false" />
		</@f.select>
		<@f.input path="customTemplateName" />
	</@form.labelled_row>


	<div class="submit-buttons">
		<button class="btn btn-primary btn-large">Save</button>
	</div>
</@f.form>


</#escape>