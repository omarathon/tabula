<#escape x as x?html>

<h1>Record missed monitoring points in eVision (SITS)</h1>

<@f.form commandName="command" action="" method="POST" cssClass="form-horizontal">

	<#if command.availablePeriods?size == 0>
		<div class="alert alert-info">
			There are no monitoring periods available for the students you have chosen.
		</div>
	<#else>
		<p>Choose the monitoring period to report:</p>
		<@form.labelled_row "period" "">

			<#list command.availablePeriods as period>
				<#assign disabledClass><#if !period._2()>disabled</#if></#assign>
				<@form.label clazz="radio ${disabledClass}" checkbox=true>
					<input id="period_${period_index}" name="period" type="radio" value="${period._1()}" ${disabledClass}>
					<#if disabledClass?has_content>
						<a class="use-tooltip" data-content="All of the chosen students have already reported for this period">
						${period._1()}
						</a>
					<#else>
						${period._1()}
					</#if>
				</@form.label>
			</#list>
		</@form.labelled_row>

		<div class="submit-buttons">
			<div class="pull-right">
				<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Loading&hellip;">
					Next
				</button>
				<a class="btn" href="<@routes.viewDepartmentStudentsWithAcademicYear command.department command.academicYear command.serializeFilter />">Cancel</a>
			</div>
		</div>
	</#if>

</@f.form>

</#escape>