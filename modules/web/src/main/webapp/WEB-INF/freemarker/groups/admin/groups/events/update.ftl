<#escape x as x?html>
	<#import "*/group_components.ftl" as components />

<h1>Update event from Syllabus+</h1>
<h4><span class="muted">for</span>
	${smallGroup.name},
	<@components.eventShortDetails smallGroupEvent />

	<#noescape><#assign popoverContent><@components.eventDetails smallGroupEvent /></#assign>
	<a class="use-popover"
	   data-html="true"
	   data-content="${popoverContent?html}"><i class="icon-question-sign" style="font-size: 0.85em;"></i></a></#noescape>
</h4>

<p>Here are all the small group events for <@fmt.module_name module false /> in the central timetabling system Syllabus+.</p>

	<@f.form method="post" action="" commandName="command" cssClass="form-horizontal">
		<@f.errors cssClass="error form-errors" />
		<@f.errors path="index" cssClass="error form-errors" />

		<table class="table table-bordered table-striped">
			<thead>
				<tr>
					<th>Timetable event</th>
				</tr>
			</thead>
			<tbody>
				<#list command.timetableEvents as timetableEvent>
					<tr>
						<td>
							<div class="pull-left">
								<@f.radiobutton path="index" value=timetableEvent_index cssClass="radio inline" />
							</div>
							<div style="margin-left: 20px;">
								<@components.timetableEventDetails timetableEvent smallGroupSet.academicYear, module.adminDepartment />
							</div>
						</td>
					</tr>
				</#list>
			</tbody>
		</table>

		<div class="submit-buttons">
			<input
					type="submit"
					class="btn btn-primary"
					name="create"
					value="Save"
					/>
			<a class="btn" href="${cancelUrl}">Cancel</a>
		</div>
	</@f.form>
</#escape>