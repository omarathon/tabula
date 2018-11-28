<#import "../../_profile_link.ftl" as pl />
<#escape x as x?html>

<#macro check label property>
	<li class="${property?string("true", "false")}">
		<strong>${label}</strong>
		<#nested />
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
	<#assign sourcemessage>This relationship is imported from SITS <#if result.department??>for ${result.department.shortName}</#if></#assign>
	<hr class="full-width" />
	<div id="profile-modal" class="modal fade profile-subset"></div>
	<div class = "row">
		<div class="col-md-4">
			<h2>Student details</h2>
			<ul class="boolean-list">
				<@check "Is a student in Tabula" result.student><#if result.studentUser.warwickId??> <@pl.profile_link result.studentUser.warwickId/></#if></@check>
				<@check "Is present in SITS" result.upstreamStudent />
				<@check "Is a current student" result.currentStudent />
				<@check "Has current course details in SITS" result.freshCourse />
			</ul>
		</div>
		<div class="col-md-8">
			<h2>Personal tutor</h2>
			<ul class="boolean-list">
				<@check sourcemessage result.personalTutorCheck.sitsIsSource />
				<@check "Is a taught student (PGR's don't have Personal Tutors)" result.personalTutorCheck.taught />
				<@check "Tutor is a Tabula member" result.personalTutorCheck.member><#if result.personalTutorCheck.tutorId??> <@pl.profile_link result.personalTutorCheck.tutorId /></#if></@check>
			</ul>
			<#if result.personalTutorCheck.rawData??>
				<#assign data = result.personalTutorCheck.rawData />
				<table class="sits-debug table table-condensed">
					<thead><tr><th>Course Type</th><th>SCJ Tutor</th><th>SPR Tutor</th></tr></thead>
					<tbody><tr>
						<td>${data.courseCode}</td>
						<td class="${(result.personalTutorCheck.fieldUsed.value == "SCJ")?string("selected", "")}">${data.scjTutor!"NULL"}</td>
						<td class="${(result.personalTutorCheck.fieldUsed.value == "SPR")?string("selected", "")}">${data.sprTutor!"NULL"}</td>
					</tr></tbody>
				</table>
			</#if>
			<#foreach r in result.relationships>
				<h2>${r.relationshipType.description}s</h2>
				<ul class="boolean-list"><@check sourcemessage r.sitsIsSource /></ul>
				<#if r.rows?has_content>
					<table class="sits-debug table table-condensed">
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
								<tr class="${row.canImport?string("true", "false")}">
									<td>
										<#assign popover>
											<ul class="boolean-list">
												<@check "Is for the students current course" row.forCurrentCourse  />
												<@check "Agent has a personnel record in SITS (entry in INS_PRS)" row.hasPersonnelRecord />
												<@check "Agent is a member in Tabula" row.member />
												<@check "Agent is current in upstream systems (FIM or SITS depending on the user)" row.upstreamMember />
											</ul>
										</#assign>
										<a class="use-popover" href="#" data-html="true" data-content="${popover}" data-container="body">
											<strong><#if row.canImport>&#x2714;<#else>&#x2718; see why</#if></strong>
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
				<#else>
					<ul class="boolean-list"><@check "No ${r.relationshipType.description} data in SITS" false /></ul>
				</#if>
			</#foreach>
		</div>
	</div>
</#if>


</#escape>