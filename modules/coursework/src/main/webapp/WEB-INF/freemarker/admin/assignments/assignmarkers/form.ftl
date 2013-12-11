<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#macro student_item student bindpath="">
<li class="student">
	<i class="icon-white icon-user"></i> ${student.displayValue}
	<input type="hidden" name="${bindpath}" value="${student.userCode}" />
</li>
</#macro>

<#macro assignStudents studentList markerList class name>
<div class="tabula-dnd" data-scroll="true">
	<div class="persist-header">
		<div class="btn-toolbar">
			<a class="random btn btn-mini" data-toggle="randomise" data-disabled-on="empty-list"
			   href="#" >
				<i class="icon-random"></i> Randomly allocate
			</a>
			<a class="return-items btn btn-mini" data-toggle="return" data-disabled-on="no-allocation"
			   href="#" >
				<i class="icon-arrow-left"></i> Remove all
			</a>
		</div>
		<div class="row-fluid">
			<div class="students span4">
				<h3>Students</h3>
			</div>
			<div class="span8">
				<h3>${name}</h3>
			</div>
		</div>
	</div>


	<div class="row-fluid fix-on-scroll-container">
		<div class="students span4">
			<div class="drag-target">   <!-- student list makes it a scrollable div -->
				<ul class="drag-list return-list unstyled" data-nobind="true">
					<#list studentList as student>
						<@student_item student "" />
					</#list>
				</ul>
			</div>
		</div>
		<div class="span8">
			<div class="agentslist ${class} fix-on-scroll">
				<#list markerList as marker>
					<#local existingStudents = marker.students />
					<div class="marker drag-target">
						<span class="name">${marker.fullName}</span>
						<span class="drag-count badge badge-info">${existingStudents?size}</span>

						<ul class="drag-list hide" data-bindpath="markerMapping[${marker.userCode}]">
							<#list existingStudents as student>
								<@student_item student "markerMapping[${marker.userCode}][${student_index}]" />
							</#list>
						</ul>

						<a href="#" class="btn show-list" data-container="#container" data-title="Students to be marked by ${marker.fullName}"><i class="icon-list"></i> List</a>

					</div>
				</#list>
			</div>
		</div>
	</div>
</div>
</#macro>


<#escape x as x?html>

		<h1>Assign markers for ${assignment.name}</h1>

		<p>Drag students by their <i class="icon-reorder"></i> onto a marker.</p>

		<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/assign-markers')}" commandName="assignMarkersCommand">
		<div class="persist-area">
			<div id="assign-markers">
				<ul class="nav nav-tabs">
					<li class="active">
						<a href="#first-markers">
							${firstMarkerRoleName}s
						</a>
					</li>
					<li>
						<a href="#second-markers">
							${secondMarkerRoleName}s
						</a>
					</li>
				</ul>

				<div class="tab-content">
					<div class="tab-pane active" id="first-markers">
						<@assignStudents
							assignMarkersCommand.firstMarkerUnassignedStudents
							assignMarkersCommand.firstMarkers
							"first-markers"
							firstMarkerRoleName
						/>
					</div>
					<div class="tab-pane" id="second-markers">
						<@assignStudents
							assignMarkersCommand.secondMarkerUnassignedStudents
							assignMarkersCommand.secondMarkers
							"second-markers"
							secondMarkerRoleName
						/>
					</div>
				</div>
			</div>

			<div class="persist-footer submit-buttons">
				<input type="submit" class="btn btn-primary" value="Save">
				<a href="<@routes.depthome module />" class="btn">Cancel</a>
			</div>

		</div><!-- end persist-area -->
	</@f.form>

<script type="text/javascript">
(function($) {
	var fixHeaderFooter = $('.persist-area').fixHeaderFooter();
	$(".floatingHeader:hidden").css("max-width", $(".floatingHeader:visible").width());
	var singleColumnDragTargetHeight =  $('.agentslist .drag-target').outerHeight(true);

	$(window).scroll(function() {
		fixHeaderFooter.fixTargetList('.agentslist:visible');
	});

	// onload reformat agents layout
	formatAgentsLayout();

	// debounced on window resize, reformat agents layout...
	on_resize(formatAgentsLayout());


	function formatAgentsLayout() {
		var agentslist = $('.agentslist:visible');
		var heightOfSingleColumnList = agentslist.find('.drag-target').length * singleColumnDragTargetHeight;

		if(agentslist.height() > fixHeaderFooter.viewableArea()) {
			agentslist.addClass('drag-list-three-col');
		} else if(fixHeaderFooter.viewableArea() > heightOfSingleColumnList) {
			agentslist.removeClass('drag-list-three-col');
		}
	}

	// debounced on_resize to avoid retriggering while resizing window
	function on_resize(c,t) {
		onresize = function() {
			clearTimeout(t);
			t = setTimeout(c,100)
		};
		return c;
	}

})(jQuery);
</script>

</#escape>


