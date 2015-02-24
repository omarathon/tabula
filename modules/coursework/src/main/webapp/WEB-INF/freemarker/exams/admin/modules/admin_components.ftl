<#ftl strip_text=true />
<#escape x as x?html>
<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#macro admin_section module expand_by_default=true>
	<#local can_manage=can.do("Module.ManageAssignments", module) />
	<#local exams = (mapGet(examMap, module))![] />

	<div id="${module_anchor(module)}" class="module-info striped-section<#if exams?has_content> collapsible<#if expand_by_default> expanded</#if></#if><#if !expand_by_default && !exams?has_content> empty</#if>"
		<#if exams?has_content && !expand_by_default>
			data-populate=".striped-section-contents"
			data-href="<@routes.moduleHomeWithYear module academicYear />"
			data-name="${module_anchor(module)}"
		</#if>
	>
		<div class="clearfix">
			<div class="btn-group module-manage-button section-manage-button">
			  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench"></i> Manage <span class="caret"></span></a>
			  <ul class="dropdown-menu pull-right">
			  		<#if can_manage>
						<li><a href="<@routes.moduleperms module />">
							<i class="icon-user"></i> Edit module permissions
						</a></li>
					</#if>

					<li>
						<#local create_url><@routes.createExam module academicYear /></#local>
						<@fmt.permission_button
							permission='Assignment.Create'
							scope=module
							action_descr='create a new exam'
							href=create_url>
							<i class="icon-plus"></i> Create new exam
						</@fmt.permission_button>
					</li>
			  </ul>
			</div>

			<h2 class="section-title with-button"><@fmt.module_name module /></h2>
		</div>


		<#if exams?has_content>
			<div class="module-info-contents striped-section-contents">
				<#if exams?has_content && expand_by_default>
					<@admin_exams module exams/>
				</#if>
			</div>
		</#if>

	</div>
</#macro>

<#macro admin_exams module exams>
	<#list exams as exam>
		<#if !exam.deleted>
			<div class="assignment-info">
				<div class="column1">
					<h3 class="name">
						<small>
						${exam.name}
						</small>
					</h3>
				</div>

				<div class="stats">
					<#local membershipInfo = exam.membershipInfo />
					<#if membershipInfo.totalCount == 0>
						<span class="label">No enrolled students</span>
					<#else>
						<i class="icon-file"></i>
						<a href="<@routes.viewExam exam />">
							<span class="use-tooltip" title="View all students and feedback">
								<@fmt.p membershipInfo.sitsCount "enrolled student"/> from SITS
							</span>
						</a>
					</#if>
					<#local requiresMarks = exam.requiresMarks />
					<#if (requiresMarks > 0)>
						<br/><span class="label label-warning"><@fmt.p number=requiresMarks singular="student requires" plural="students require"/> marks</span>
					</#if>
				</div>

				<div class="assignment-buttons">
					<div class="btn-group">
						<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-cog"></i> Actions <span class="caret"></span></a>
						<ul class="dropdown-menu pull-right">
							<li>
								<#local edit_url><@routes.editExam exam /></#local>
								<@fmt.permission_button
									permission='Assignment.Update'
									scope=exam
									action_descr='edit exam properties'
									href=edit_url
								>
									<i class="icon-wrench"></i> Edit properties
								</@fmt.permission_button>
							</li>

							<li class="divider"></li>

							<li>
								<#local marks_url><@routes.examAddMarks exam /></#local>
								<@fmt.permission_button
									permission='Marks.Create'
									scope=exam
									action_descr='add marks'
									href=marks_url
								>
									<i class="icon-check"></i> Add marks
								</@fmt.permission_button>
							</li>
						</ul>
					</div>
				</div>
				<div class="end-assignment-info"></div>
			</div>
		</#if>
	</#list>
</#macro>

</#escape>