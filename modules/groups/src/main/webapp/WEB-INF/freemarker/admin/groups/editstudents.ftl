<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Edit small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

	<#if saved!false>
		<div class="alert alert-success">
			<button type="button" class="close" data-dismiss="alert">&times;</button>
			Students saved for ${smallGroupSet.name}.
		</div>
	</#if>

	<div class="fix-area">
		<@f.form id="editStudents" method="POST" commandName="command" class="form-horizontal dirty-check">
			<@components.set_wizard false 'students' smallGroupSet />

			<#include "_editStudents.ftl" />

			<div class="submit-buttons fix-footer">
				<input
					type="submit"
					class="btn btn-success update-only"
					value="Save"
					/>
				<input
					type="submit"
					class="btn btn-primary"
					name="create"
					value="Save and exit"
					/>
				<a class="btn dirty-check-ignore" href="<@routes.depthome module=smallGroupSet.module academicYear=smallGroupSet.academicYear/>">Cancel</a>
			</div>
		</@f.form>
	</div>
</#escape>