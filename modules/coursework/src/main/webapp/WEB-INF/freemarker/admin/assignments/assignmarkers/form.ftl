<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#macro assignStudents studentList markerList markerMapping class name>
	<div class="btn-toolbar">
		<a class="random btn btn-mini"
		   href="#" >
			<i class="icon-random"></i> Randomly allocate
		</a>
		<a class="remove-all btn btn-mini"
		   href="#" >
			<i class="icon-arrow-left"></i> Remove all
		</a>
	</div>
	<div class="row-fluid">
		<div class="students span3">
			<h3>Students</h3>
			<div class="student-list">
				<ul class="member-list">
					<#list studentList as student>
						<li>
							<div class="student" data-student-id="${student}">
								<i class="icon-user"></i> ${student}
							</div>
						</li>
					</#list>
				</ul>
			</div>
		</div>
		<div class="${class} span9">
			<h3>${name}</h3>
			<ul class="member-list">
				<#list markerList as marker>
					<#assign existingStudents = markerMapping[marker] />
					<li>
						<div class="marker" data-marker-id="${marker}">
							<span>${marker}</span>
							<span class="count badge badge-info">${existingStudents?size}</span>
							<div id="container-${marker}" class="student-container hidden">
								<ul class="student-list">
									<#list existingStudents as student>
										<li>
											${student}
											<a class="remove-student btn btn-mini"
												href="#" data-marker-id="${marker}"
												data-student-id="${student}">
												<i class="icon-remove"></i> Remove
											</a>
										</li>
									</#list>
								</ul>
								<#list existingStudents as student>
									<input type="hidden" name="markerMapping[${marker}][${student_index}]" value="${student}">
								</#list>
							</div>
							<a id="tool-tip-${marker}" class="btn btn-mini" data-toggle="button" href="#">
								<i class="icon-list"></i>
								List
							</a>
							<script type="text/javascript">
								jQuery(function($){
									$("#tool-tip-${marker}").popover({
										placement: 'right',
										html: true,
										content: function(){
											return $('<div />').append($('#container-${marker} .student-list').clone()).html();
										},
										title: 'Students to be marked by ${marker}'
									});
								});
							</script>
						</div>
					</li>
				</#list>
			</ul>
		</div>
	</div>
</#macro>


<#escape x as x?html>
	<h1>Assign markers for ${assignment.name}</h1>
	<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/assign-markers')}" commandName="assignMarkersCommand">
	<div id="assign-markers">
		<ul class="nav nav-tabs">
			<li class="active">
				<a href="#first-markers">
					First markers  <!-- ${assignMarkersCommand.firstMarkerStudents?size} students unassigned -->
				</a>
			</li>
			<li>
				<a href="#second-markers">
					Second markers  <!-- ${assignMarkersCommand.secondMarkerStudents?size} students unassigned -->
				</a>
			</li>
		</ul>
		<div class="tab-content">
			<div class="tab-pane active" id="first-markers">
				<@assignStudents
					assignMarkersCommand.firstMarkerStudents
					assignMarkersCommand.firstMarkers
					assignMarkersCommand.markerMapping
					"first-markers"
					"First Markers"
				/>
			</div>
			<div class="tab-pane" id="second-markers">
				<@assignStudents
					assignMarkersCommand.secondMarkerStudents
					assignMarkersCommand.secondMarkers
					assignMarkersCommand.markerMapping
					"second-markers"
					"Second Markers"
				/>
			</div>
		</div>
	</div>
	<div class="submit-buttons">
		<input type="submit" class="btn btn-primary" value="Save">
		<a href="<@routes.depthome module />" class="btn">Cancel</a>
	</div>
	</@f.form>
</#escape>


