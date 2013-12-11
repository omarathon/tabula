<#if command.approved>
	<div class="well alert alert-success">
		<h3 style="color: inherit;">You approved this feedback at <@fmt.date completedDate /></h3>
	</div>
<#else>
	<div class="well alert alert-danger">
		<h3 style="color: inherit;">You rejected this feedback at <@fmt.date completedDate /></h3>
	</div>
	<div class="form-horizontal">
		<@form.labelled_row "command.rejectionComments" "Comments">
			<@f.textarea path="command.rejectionComments" cssClass="big-textarea" disabled = "true" />
		</@form.labelled_row>

		<#if assignment.collectMarks>
			<@form.row>
				<@form.label path="command.mark">Adjusted Mark</@form.label>
				<@form.field>
	                <div class="input-append">
						<@f.input path="command.mark" cssClass="input-small" disabled = "true" />
	                    <span class="add-on">%</span>
	                </div>
				</@form.field>
			</@form.row>

			<@form.row>
				<@form.label path="command.grade">Adjusted Grade</@form.label>
				<@form.field>
					<@f.input path="command.grade" cssClass="input-small" disabled = "true" />
				</@form.field>
			</@form.row>
		</#if>
	</div>
</#if>