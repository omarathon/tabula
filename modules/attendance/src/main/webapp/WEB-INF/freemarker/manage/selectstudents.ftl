<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />
<#import "../attendance_macros.ftl" as attendance_macros />

<h1>Select students</h1>
<h4><span class="muted">for</span> ${findCommand.scheme.displayName}</h4>

<div class="fix-area">

	<form method="POST" action="">
		<details>
			<summary class="large-chevron collapsible">
				<span class="legend">Find students
					<small>Select students by route, year of study etc.</small>
				</span>

			</summary>

			<input type="hidden" name="filterQueryString" value="${findCommand.filterQueryString}">
			<#list findCommand.staticStudentIds as id>
				<input type="hidden" name="staticStudentIds" value="${id}" />
			</#list>
			<#list findCommand.updatedStaticStudentIds as id>
				<input type="hidden" name="updatedStaticStudentIds" value="${id}" />
			</#list>
		</details>


		<details>
			<summary class="large-chevron collapsible">
				<span class="legend">Manually added and removed students
					<small>Add a list of students by university ID or username</small>
				</span>

				<p>
					<@fmt.p editMembershipCommand.updatedIncludedStudentIds?size "student" />
					added manually and
					<@fmt.p editMembershipCommand.updatedExcludedStudentIds?size "student" />
					removed manually
				</p>

				<p>
					<input class="btn" type="submit" name="${manuallyAddFormString}" value="Add students manually" />
				</p>
			</summary>

			<@attendance_macros.manageStudentTable editMembershipCommand.membershipItems />

			<#list editMembershipCommand.includedStudentIds as id>
				<input type="hidden" name="includedStudentIds" value="${id}" />
			</#list>
			<#list editMembershipCommand.updatedIncludedStudentIds as id>
				<input type="hidden" name="updatedIncludedStudentIds" value="${id}" />
			</#list>
			<#list editMembershipCommand.excludedStudentIds as id>
				<input type="hidden" name="excludedStudentIds" value="${id}" />
			</#list>
			<#list editMembershipCommand.updatedExcludedStudentIds as id>
				<input type="hidden" name="updatedExcludedStudentIds" value="${id}" />
			</#list>
		</details>
	</form>


	<div class="fix-footer submit-buttons">
		<form action="<@routes.manageAddStudents editMembershipCommand.scheme />" method="POST">
			<input type="hidden" name="filterQueryString" value="${findCommand.filterQueryString}">
			<#list findCommand.staticStudentIds as id>
				<input type="hidden" name="staticStudentIds" value="${id}" />
			</#list>
			<#list findCommand.updatedStaticStudentIds as id>
				<input type="hidden" name="updatedStaticStudentIds" value="${id}" />
			</#list>
			<#list editMembershipCommand.includedStudentIds as id>
				<input type="hidden" name="includedStudentIds" value="${id}" />
			</#list>
			<#list editMembershipCommand.updatedIncludedStudentIds as id>
				<input type="hidden" name="updatedIncludedStudentIds" value="${id}" />
			</#list>
			<#list editMembershipCommand.excludedStudentIds as id>
				<input type="hidden" name="excludedStudentIds" value="${id}" />
			</#list>
			<#list editMembershipCommand.updatedExcludedStudentIds as id>
				<input type="hidden" name="updatedExcludedStudentIds" value="${id}" />
			</#list>
			<input type="submit" value="Link to SITS" class="btn btn-success" name="${linkToSitsString}">
			<input type="submit" value="Import as list" class="btn btn-primary" name="${importAsListString}">
			<input type="submit" value="Cancel" class="btn" name="${resetString}">
		</form>
	</div>
</div>

</#escape>