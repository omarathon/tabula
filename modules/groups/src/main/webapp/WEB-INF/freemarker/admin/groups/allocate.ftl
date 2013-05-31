<#assign set=allocateStudentsToGroupsCommand.set />

<#macro student_item student bindpath="">
<li class="student">
	<i class="icon-white icon-user"></i> ${student.fullName}
	<input type="hidden" name="${bindpath}" value="${student.userCode}" />
</li>
</#macro>

<#escape x as x?html>
	<#if saved??>
		<div class="alert alert-success">
			<a class="close" data-dismiss="alert">&times;</a>
			<p>Changes saved.</p>
		</div>
	</#if>

	<h1>Assign students for ${set.name}</h1>
	
	<noscript>
		<div class="alert">This page requires Javascript.</div>
	</noscript>
	
	<p>Drag students by their <i class="icon-th"></i> onto a group. To select multiple students,
	drag a box from one module name to another. You can also hold the <kbd>Ctrl</kbd> key to add to a selection</p>
	
	<@spring.hasBindErrors name="allocateStudentsToGroupsCommand">
		<#if errors.hasErrors()>
			<div class="alert alert-error">
				<h3>Some problems need fixing</h3>
				<#if errors.hasGlobalErrors()>
					<#list errors.globalErrors as e>
						<div><@spring.message message=e /></div>
					</#list>
				<#else>
					<div>See the errors below.</div>
				</#if>
			</div>
		</#if>
	</@spring.hasBindErrors>
	
	<#assign submitUrl><@routes.allocateset set /></#assign>
	<@f.form method="post" action="${submitUrl}" commandName="allocateStudentsToGroupsCommand">
		<div class="btn-toolbar">
			<a class="random btn btn-mini"
			   href="#" >
				<i class="icon-random"></i> Randomly allocate
			</a>
			<a class="return-items btn btn-mini"
			   href="#" >
				<i class="icon-arrow-left"></i> Remove all
			</a>
		</div>
		<div class="row-fluid">
			<div class="students span4">
				<h3>Students</h3>
				<div class="student-list drag-target">
					<ul class="drag-list return-list" data-bindpath="unallocated">
						<@spring.bind path="unallocated">
							<#list status.actualValue as student>
								<@student_item student "${status.expression}[${student_index}]" />
							</#list>
						</@spring.bind>
					</ul>
				</div>
			</div>
			<div class="span8">
				<#macro mods department modules>
					<div class="drag-target">
						<h1>${department.name}</h1>
						<ul class="drag-list full-width" data-bindpath="mapping[${department.code}]">
						<#list modules as module>
							<li class="label" title="${module.name}">
								${module.code?upper_case}
								<input type="hidden" name="mapping[${department.code}][${module_index}]" value="${module.id}" />
							</li>
						</#list>
						</ul>
					</div>
				</#macro>
			
				<#list set.groups as group>
					<#assign existingStudents = allocateStudentsToGroupsCommand.mappingById[group.id]![] />
					<div class="drag-target">
						<span class="name">${group.name}</span>
						<span class="drag-count badge badge-info">${existingStudents?size}</span>
	
						<ul class="drag-list hide" data-bindpath="mapping[${group.id}]">
							<#list existingStudents as student>
								<@student_item student "mapping[${group.id}][${student_index}]" />
							</#list>
						</ul>
	
						<a href="#" class="btn show-list" data-title="Students in ${group.name}"><i class="icon-list"></i> List</a>
	
					</div>
				</#list>
			</div>
		</div>		
		
		<div class="submit-buttons">
			<input type="submit" class="btn btn-primary" value="Save">
			<a href="<@routes.depthome module />" class="btn">Cancel</a>
		</div>
	</@f.form>
</#escape>