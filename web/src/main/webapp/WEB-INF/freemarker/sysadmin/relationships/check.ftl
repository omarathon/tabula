<#import "../../_profile_link.ftl" as pl />
<#escape x as x?html>

<#macro check label property>
	<li class="${property?string("success", "fail")}">
		<strong>${label}</strong>
	</li>
</#macro>

<h1>Check SITS for student relationship data</h1>

<@f.form method="get" action="${url('/sysadmin/relationships/check')}" commandName="checkStudentRelationshipImportCommand">

	<@f.errors cssClass="error form-errors" />

	<@bs3form.labelled_form_group "student" "Student">
		<@bs3form.flexipicker path="student" placeholder="Enter name or ID" membersOnly="true">
		</@bs3form.flexipicker>
	</@bs3form.labelled_form_group>

	<@bs3form.form_group>
		<input type="submit" value="Check" class="btn btn-primary">
		<a class="btn btn-default" href="<@url page="/sysadmin/relationships" />">Cancel</a>
	</@bs3form.form_group>

</@f.form>

<#if result??>
	<hr class="full-width" />
	<div id="profile-modal" class="modal fade profile-subset"></div>
	<div class = "row">
		<div class="col-md-4">
			<h2>Student details</h2>
			<ul class="boolean-list">
				<@check "Is a student in Tabula" result.student />
				<@check "Is present in SITS" result.upstreamStudent />
				<@check "Is a current student" result.currentStudent />
				<@check "Has current course details in SITS" result.freshCourse />
			</ul>
		</div>
		<div class="col-md-8">
			<#foreach r in result.relationships>
				<h2>${r.relationshipType.description}s</h2>
				<#assign sourcemessage>This relationship is imported from SITS <#if result.department??>for ${result.department.shortName}</#if></#assign>
				<ul class="boolean-list"><@check sourcemessage r.sitsIsSource /></ul>
				<#if r.rows?has_content>
					<table class="sits-debug">
						<thead>
							<tr>
								<th>Will import</th>
								<th>Student SCJ code</th>
								<th>University ID</th>
								<th>Personnel Record</th>
								<th>Percentage</th>
							</tr>
						</thead>
						<tbody>
							<#foreach row in r.rows>
								<tr class="${row.canImport()?string("success", "fail")}">
									<td>
										<#assign popover>
											<ul class="boolean-list">
												<@check "Is for the students current course" row.forCurrentCourse  />
												<@check "Has a personnel record in SITS (entry in INS_PRS)" row.hasPersonnelRecord />
												<@check "Is a member in Tabula" row.member />
												<@check "Is current in upstream systems (FIM or SITS depending on the user)" row.upstreamMember />
											</ul>
										</#assign>
										<a class="use-popover" href="#" data-html="true" data-content="${popover}" data-container="body">
											<strong><#if row.canImport()>&#x2714;<#else>&#x2718; see why</#if></strong>
										</a>
									</td>
									<td>${row.rawData.scjCode!"NULL"}</td>
									<td>
										<#if row.rawData.agentId??><@pl.profile_link row.rawData.agentId/> </#if>${row.rawData.agentId!"NULL"}
									</td>
									<td>${row.rawData.personnelRecord!"NULL"}</td>
									<td>${row.rawData.percentage!"NULL"}</td>
								</tr>
							</#foreach>
						</tbody>
					</table>
				</#if>
			</#foreach>
		</div>
	</div>
</#if>


</#escape>