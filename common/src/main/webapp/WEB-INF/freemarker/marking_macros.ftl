<#escape x as x?html>
  <#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
  <#import "*/modal_macros.ftl" as modal />

  <#macro autoGradeOnline gradePath gradeLabel markPath markingId generateUrl>
    <#local labelText>${gradeLabel} <@fmt.help_popover id="auto-grade-${markingId}-help" content="The grades available depends on the mark entered and the SITS mark scheme in use" /></#local>
    <@bs3form.labelled_form_group path=gradePath labelText=labelText>
      <div class="input-group">
        <@f.input path="${gradePath}" cssClass="form-control auto-grade" id="auto-grade-${markingId}" />
        <select name="${gradePath}" class="form-control" disabled style="display:none;"></select>
      </div>
    </@bs3form.labelled_form_group>

    <@autoGradeOnlineScripts markPath markingId generateUrl />
  </#macro>

  <#macro autoGradeOnlineScripts markPath markingId generateUrl>
    <script nonce="${nonce()}">
      jQuery(function ($) {
        var $gradeInput = $('#auto-grade-${markingId}').hide()
          , $markInput = $gradeInput.closest('form').find('input[name="${markPath}"]')
          , $select = $gradeInput.closest('div').find('select').on('click', function () {
          $(this).closest('.control-group').removeClass('info');
        })
          , currentRequest = null
          , data = {'studentMarks': {}}
          , doRequest = function () {
          if (currentRequest != null) {
            currentRequest.abort();
          }
          data['studentMarks']['${markingId}'] = $markInput.val();
          if ($select.is(':visible') || $gradeInput.val().length > 0) {
            data['selected'] = {};
            data['selected']['${markingId}'] = ($select.is(':visible')) ? $select.val() : $gradeInput.val();
          }
          currentRequest = $.ajax('${generateUrl}', {
            'type': 'POST',
            'data': data,
            success: function (data) {
              $select.html(data);
              if ($select.find('option').length > 1) {
                $gradeInput.hide().prop('disabled', true);
                $select.prop('disabled', false).show()
                  .closest('.control-group').addClass('info');
                $('#auto-grade-${markingId}-help').show();
              } else {
                $gradeInput.show().prop('disabled', false);
                $select.prop('disabled', true).hide();
                $('#auto-grade-${markingId}-help').hide();
              }
            }, error: function (xhr, errorText) {
              if (errorText != "abort") {
                $gradeInput.show().prop('disabled', false);
                $('#auto-grade-${markingId}-help').hide();
              }
            }
          });
        };
        $markInput.on('keyup', doRequest);
        doRequest();
      });
    </script>
  </#macro>

<#-- used in exams -->
<#macro marksForm assignment templateUrl formUrl commandName cancelUrl generateUrl seatNumberMap="" showAddButton=true>
  <div id="batch-feedback-form">
    <h1>Submit marks for ${assignment.name}</h1>
    <ul id="marks-tabs" class="nav nav-tabs">
      <li class="active"><a href="#upload">Upload</a></li>
      <li class="webform-tab"><a href="#webform">Web Form</a></li>
    </ul>
    <div class="tab-content">
      <div class="tab-pane active" id="upload">
        <p>
          You can upload marks in a spreadsheet, which must be saved as an .xlsx file (ie created in Microsoft Office 2007 or later).
          The spreadsheet should have at least two column headings: <b>University ID</b> and <b>Mark</b>.
          You can use this <a href="${templateUrl}">generated spreadsheet</a> as a template.
          Note that you can upload just marks, or marks and grades.
        </p>
        <@f.form method="post" enctype="multipart/form-data" action="${formUrl}" modelAttribute="${commandName}">
          <input name="isfile" value="true" type="hidden" />
          <h3>Select file</h3>
          <@bs3form.filewidget
          basename="file"
          labelText="Files"
          types=[]
          multiple=true
          />
          <div class="submit-buttons">
            <button class="btn btn-primary btn-lg">Upload</button>
          </div>
        </@f.form>
      </div>
      <div class="tab-pane" id="webform">
        <#if showAddButton>
        <p>
          Click the add button below to enter marks for a student.
        </p>

        <div class="row-markup hide">
          <div class="mark-row form-group">
            <#if seatNumberMap?has_content>
              <div class="col-md-2"></div>
            </#if>
            <div class="col-md-2">
              <input class="universityId form-control" name="universityId" type="text" />
            </div>
            <#if studentMarkerMap?has_content>
              <div class="col-md-3"></div>
            </#if>
            <div class="col-md-2">
              <div class="input-group">
                <input name="actualMark" type="text" class="form-control" />
                <span class="input-group-addon">%</span>
              </div>
              <div class="col-md-2">
                <input class="grade form-control" name="actualGrade" type="text" />
                <#if isGradeValidation>
                  <select name="actualGrade" class="form-control" disabled style="display:none;"></select>
                </#if>
              </div>
            </div>
          </div>
          </#if>
          <@f.form id="marks-web-form" method="post" enctype="multipart/form-data" action="${formUrl}" modelAttribute="${commandName}">
            <div class="fix-area">
              <input name="isfile" value="false" type="hidden" />
              <div class="marksUpload">
                <div class="mark-header form-group clearfix">
                  <#if seatNumberMap?has_content>
                    <div class="col-md-2">Seat order</div>
                  </#if>
                  <div class="col-md-2">University ID</div>
                  <#if studentMarkerMap?has_content>
                    <div class="col-md-3">Marker</div>
                  </#if>
                  <div class="col-md-2">Marks</div>
                  <div class="col-md-2">
                    Grade <#if isGradeValidation><@fmt.help_popover id="auto-grade-help" content="The grade is automatically calculated from the SITS mark scheme" /></#if></div>
                </div>
                <#if marksToDisplay??>
                  <#list marksToDisplay as markItem>
                    <div class="mark-row form-group clearfix">
                      <#if seatNumberMap?has_content>
                        <#if mapGet(seatNumberMap, markItem.user.userId)??>
                          <div class="col-md-2">${mapGet(seatNumberMap, markItem.user.userId)}</div>
                        <#else>
                          <div class="col-md-2"></div>
                        </#if>
                      </#if>
                      <div class="col-md-2">
                        <input class="universityId form-control" value="${markItem.universityId}" name="marks[${markItem_index}].universityId" type="text"
                               readonly="readonly" />
                      </div>
                      <#if studentMarkerMap?has_content>
                        <#if mapGet(studentMarkerMap, markItem.universityId)??>
                          <div class="col-md-3">${mapGet(studentMarkerMap, markItem.universityId)}</div>
                        <#else>
                          <div class="col-md-3"></div>
                        </#if>
                      </#if>
                      <div class="col-md-2">
                        <div class="input-group">
                          <input class="form-control" name="marks[${markItem_index}].actualMark" class="mark"
                                 value="<#if markItem.actualMark??>${markItem.actualMark}</#if>" type="text" />
                          <span class="input-group-addon">%</span>
                        </div>
                      </div>
                      <div class="col-md-2">
                        <input name="marks[${markItem_index}].actualGrade" class="grade form-control"
                               value="<#if markItem.actualGrade??>${markItem.actualGrade}</#if>" type="text" />
                        <#if isGradeValidation>
                          <select name="marks[${markItem_index}].actualGrade" class="form-control" disabled style="display:none;"></select>
                        </#if>
                      </div>
                    </div>
                  </#list>
                </#if>
              </div>
              <#if showAddButton>
                <br />
                <button class="add-additional-marks btn btn-default">Add</button>
              </#if>
              <div class="fix-footer">
                <input type="submit" class="btn btn-primary" value="Save">
                or <a href="${cancelUrl}" class="btn btn-default">Cancel</a>
              </div>
            </div>
          </@f.form>
        </div>
      </div>
    </div>

    <script nonce="${nonce()}">
      jQuery(function ($) {
        $('.fix-area').fixHeaderFooter();
        // Fire a resize to get the fixed button in the right place
        $('.webform-tab').on('shown.bs.tab', function () {
          $(window).trigger('resize');
        });

        if (${isGradeValidation?string('true','false')}) {
          var currentRequest = null, doIndividualRequest = function () {
            if (currentRequest != null) {
              currentRequest.abort();
            }
            var data = {'studentMarks': {}, 'selected': {}}
              , $this = $(this)
              , $markRow = $this.closest('.form-group')
              , $gradeInput = $markRow.find('input.grade')
              , $select = $markRow.find('select')
              , universityId = $markRow.find('input.universityId').val();
            data['studentMarks'][universityId] = $this.val();
            if ($select.is(':visible') || $gradeInput.val().length > 0) {
              data['selected'] = {};
              data['selected'][universityId] = ($select.is(':visible')) ? $select.val() : $gradeInput.val();
            }
            currentRequest = $.ajax('${generateUrl}', {
              'type': 'POST',
              'data': data,
              success: function (data) {
                $select.html(data);
                if ($select.find('option').length > 1 || $this.val() == "") {
                  $gradeInput.hide().prop('disabled', true);
                  $select.prop('disabled', false).show()
                } else {
                  $gradeInput.show().prop('disabled', false);
                  $select.prop('disabled', true).hide();
                }
              }, error: function (xhr, errorText) {
                if (errorText != "abort") {
                  $gradeInput.show().prop('disabled', false);
                }
              }
            });
          };

          $('.marksUpload').on('keyup', 'input[name*="actualMark"]', doIndividualRequest).on('tableFormNewRow', function () {
            // Make sure all the selects have the correct name
            $('.marksUpload .mark-row select').each(function () {
              $(this).attr('name', $(this).closest('div').find('input').prop('name'));
            });
          });

          var currentData = {'studentMarks': {}, 'selected': {}};
          var $markRows = $('.marksUpload .mark-row').each(function () {
            var $markRow = $(this)
              , universityId = $markRow.find('input.universityId').val()
              , $select = $markRow.find('select')
              , $gradeInput = $markRow.find('input.grade');
            currentData['studentMarks'][universityId] = $markRow.find('input[name*="actualMark"]').val();
            currentData['selected'][universityId] = ($select.is(':visible')) ? $select.val() : $gradeInput.val();
            $gradeInput.hide();
          });
          $.ajax('${generateUrl}/multiple', {
            'type': 'POST',
            'data': currentData,
            success: function (data) {
              var $selects = $(data);
              $markRows.each(function () {
                var $markRow = $(this)
                  , universityId = $markRow.find('input.universityId').val()
                  , $thisSelect = $selects.find('select').filter(function () {
                  return $(this).data('universityid') == universityId;
                });
                if ($thisSelect.length > 0 && ($thisSelect.find('option').length > 1 || $markRow.find('input.mark').val() == "")) {
                  $markRow.find('input.grade').hide().prop('disabled', true);
                  $markRow.find('select').html($thisSelect.html()).prop('disabled', false).show();
                } else {
                  $markRow.find('input.grade').show().prop('disabled', false);
                  $markRow.find('select').prop('disabled', true).hide();
                }

              });
            },
            error: function(err) {
              // TAB-7304 add error handling
            },
          });
        }
      });
    </script>
    </#macro>

    <#macro feedbackGradeValidation isGradeValidation gradeValidation>
      <#local gradeValidationClass><#compress>
        <#if isGradeValidation>
          <#if gradeValidation.invalid?has_content || gradeValidation.zero?has_content>danger<#elseif gradeValidation.populated?has_content>info</#if>
        <#else>
          <#if gradeValidation.populated?has_content || gradeValidation.invalid?has_content>danger</#if>
        </#if>
      </#compress></#local>

    <#if gradeValidation.populated?has_content || gradeValidation.invalid?has_content || gradeValidation.zero?has_content || gradeValidation.notOnScheme?has_content>
    <#if isGradeValidation>
      <div class="grade-validation alert alert-${gradeValidationClass}" style="display:none;">
        <#if gradeValidation.notOnScheme?has_content >
          <#local notOnSchemeTotal = gradeValidation.notOnScheme?keys?size />
            <p>
              <a href="#grade-validation-not-on-scheme-modal" data-toggle="modal"><@fmt.p notOnSchemeTotal "student" /></a>
              <#if notOnSchemeTotal==1>
                has feedback that cannot be uploaded because the student is manually-added and therefore not present on a linked assessment component.
              <#else>
                have feedback that cannot be uploaded because the students are manually-added and therefore not present on a linked assessment component.
              </#if>
            </p>
        </#if>
        <#if gradeValidation.invalid?has_content>
          <#local total = gradeValidation.invalid?keys?size />
          <p>
            <a href="#grade-validation-invalid-modal" data-toggle="modal"><@fmt.p total "student" /></a>
            <#if total==1>
              has feedback with a grade that is invalid. It will not be uploaded.
            <#else>
              have feedback with grades that are invalid. They will not be uploaded.
            </#if>
          </p>
        </#if>
        <#if gradeValidation.zero?has_content>
          <#local total = gradeValidation.zero?keys?size />
          <p>
            <a href="#grade-validation-zero-modal" data-toggle="modal"><@fmt.p total "student" /></a>
            <#if total==1>
              has feedback with a mark of zero and no grade. Zero marks are not populated with a default grade and it will not be uploaded.
            <#else>
              have feedback with marks of zero and no grades. Zero marks are not populated with a default grade and they will not be uploaded.
            </#if>
          </p>
        </#if>
        <#if gradeValidation.populated?has_content>
          <#local total = gradeValidation.populated?keys?size />
          <p>
            <a href="#grade-validation-populated-modal" data-toggle="modal"><@fmt.p total "student" /></a>
            <#if total==1>
              has feedback with a grade that is empty. It will be populated with a default grade.
            <#else>
              have feedback with grades that are empty. They will be populated with a default grade.
            </#if>
          </p>
        </#if>
      </div>
      <div id="grade-validation-invalid-modal" class="modal fade">
        <@modal.wrapper>
          <@modal.header>
            <h3 class="modal-title">Students with invalid grades</h3>
          </@modal.header>
          <@modal.body>
            <table class="table table-condensed table-bordered table-striped table-hover">
              <thead>
              <tr>
                <th>University ID</th>
                <th>Mark</th>
                <th>Grade</th>
                <th>Valid grades</th>
              </tr>
              </thead>
              <tbody>
              <#list gradeValidation.invalid?keys as feedback>
                <tr>
                  <td>${feedback.studentIdentifier}</td>
                  <td>${(feedback.latestMark)!}</td>
                  <td>${(feedback.latestGrade)!}</td>
                  <td>${mapGet(gradeValidation.invalid, feedback)}</td>
                </tr>
              </#list>
              </tbody>
            </table>
          </@modal.body>
        </@modal.wrapper>
      </div>
      <div id="grade-validation-zero-modal" class="modal fade">
        <@modal.wrapper>
          <@modal.header>
            <h3 class="modal-title">Students with zero marks and empty grades</h3>
          </@modal.header>
          <@modal.body>
            <table class="table table-condensed table-bordered table-striped table-hover">
              <thead>
              <tr>
                <th>University ID</th>
                <th>Mark</th>
                <th>Grade</th>
              </tr>
              </thead>
              <tbody>
              <#list gradeValidation.zero?keys as feedback>
                <tr>
                  <td>${feedback.studentIdentifier}</td>
                  <td>${(feedback.latestMark)!}</td>
                  <td>${(feedback.latestGrade)!}</td>
                </tr>
              </#list>
              </tbody>
            </table>
          </@modal.body>
        </@modal.wrapper>
      </div>
      <div id="grade-validation-populated-modal" class="modal fade">
        <@modal.wrapper>
          <@modal.header>
            <h3 class="modal-title">Students with empty grades</h3>
          </@modal.header>
          <@modal.body>
            <table class="table table-condensed table-bordered table-striped table-hover">
              <thead>
              <tr>
                <th>University ID</th>
                <th>Mark</th>
                <th>Populated grade</th>
              </tr>
              </thead>
              <tbody>
              <#list gradeValidation.populated?keys as feedback>
                <tr>
                  <td>${feedback.studentIdentifier}</td>
                  <td>${(feedback.latestMark)!}</td>
                  <td>${mapGet(gradeValidation.populated, feedback)}</td>
                </tr>
              </#list>
              </tbody>
            </table>
          </@modal.body>
        </@modal.wrapper>
      </div>
    <#else>
      <div class="grade-validation alert alert-${gradeValidationClass}" style="display:none;">
        <#if gradeValidation.notOnScheme?has_content >
          <p>
            <#local notOnSchemeTotal = gradeValidation.notOnScheme?keys?size />
            <a href="#grade-validation-not-on-scheme-modal" data-toggle="modal"><@fmt.p notOnSchemeTotal "student" /></a>
            <#if notOnSchemeTotal==1>
              has feedback that cannot be uploaded because the student is manually-added and therefore not present on a linked assessment component.
            <#else>
              have feedback that cannot be uploaded because the students are manually-added and therefore not present on a linked assessment component.
            </#if>
          </p>
        </#if>
        <#local total = gradeValidation.populated?keys?size + gradeValidation.invalid?keys?size />
        <p>
          <a href="#grade-validation-modal" data-toggle="modal"><@fmt.p total "student" /></a>
          <#if total==1>
            has feedback with a grade that is empty or invalid. It will not be uploaded.
          <#else>
            have feedback with grades that are empty or invalid. They will not be uploaded.
          </#if>
        </p>
      </div>
      <div id="grade-validation-modal" class="modal fade">
        <@modal.wrapper>
          <@modal.header>
            <h3 class="modal-title">Students with empty or invalid grades</h3>
          </@modal.header>
          <@modal.body>
            <table class="table table-condensed table-bordered table-striped table-hover">
              <thead>
              <tr>
                <th>University ID</th>
                <th>Mark</th>
                <th>Grade</th>
                <th>Valid grades</th>
              </tr>
              </thead>
              <tbody>
              <#list gradeValidation.populated?keys as feedback>
                <tr>
                  <td>${feedback.studentIdentifier}</td>
                  <td>${(feedback.latestMark)!}</td>
                  <td></td>
                  <td></td>
                </tr>
              </#list>
              <#list gradeValidation.invalid?keys as feedback>
                <tr>
                  <td>${feedback.studentIdentifier}</td>
                  <td>${(feedback.latestMark)!}</td>
                  <td>${(feedback.latestGrade)!}</td>
                  <td>${mapGet(gradeValidation.invalid, feedback)}</td>
                </tr>
              </#list>
              </tbody>
            </table>
          </@modal.body>
        </@modal.wrapper>
      </div>
    </#if>
    <div id="grade-validation-not-on-scheme-modal" class="modal fade">
      <@modal.wrapper>
          <@modal.header>
            <h3 class="modal-title">Manually-added students</h3>
          </@modal.header>
          <@modal.body>
            <table class="table table-condensed table-bordered table-striped table-hover">
              <thead>
              <tr>
                <th>University ID</th>
                <th>Mark</th>
                <th>Grade</th>
              </tr>
              </thead>
              <tbody>
              <#list gradeValidation.notOnScheme?keys as feedback>
                <tr>
                  <td>${feedback.studentIdentifier}</td>
                  <td>${(feedback.latestMark)!}</td>
                  <td>${(feedback.latestGrade)!}</td>
                </tr>
              </#list>
              </tbody>
            </table>
          </@modal.body>
      </@modal.wrapper>
    </div>
    </#if>
      <script nonce="${nonce()}">
        jQuery(function ($) {
          $('#sendToSits').on('change', function () {
            var $validationDiv = $('.grade-validation');
            if ($(this).is(':checked') && ($validationDiv.hasClass('alert-info') || $validationDiv.hasClass('alert-danger'))) {
              $validationDiv.show();
            } else {
              $validationDiv.hide();
            }
          }).trigger('change');
        });
      </script>
    </#macro>

    <#macro uploadToSits withValidation assignment verb isGradeValidation=false gradeValidation="">
      <div class="alert alert-info">
        <@bs3form.form_group path="sendToSits">
          <@bs3form.checkbox path="sendToSits">
            <@f.checkbox path="sendToSits" id="sendToSits" /> Upload ${verb} marks and grades to SITS
          </@bs3form.checkbox>
        </@bs3form.form_group>

        <#if assignment.module.adminDepartment.canUploadMarksToSitsForYear(assignment.academicYear, assignment.module)>
          <div>
            <p>They display in the SITS SAT screen as actual marks and grades.</p>
          </div>
        <#else>
          <div class="alert alert-warning">
            <p>
              Mark upload is closed
              for ${assignment.module.adminDepartment.name} <#if assignment.module.degreeType??> (${assignment.module.degreeType.toString})</#if>
              for the academic year ${assignment.academicYear.toString}.
            </p>
            <p>
              If you still have marks and grades to upload, please contact the Exams Office <a id="email-support-link" href="mailto:aoexams@warwick.ac.uk">aoexams@warwick.ac.uk</a>.
            </p>
            <p>
              Select the checkbox to queue marks and grades for upload to SITS. As soon as mark upload re-opens for this department, the marks and grades will
              automatically upload. They display in the SITS SAT screen as actual marks and grades.
            </p>
          </div>
        </#if>
      </div>

      <#if withValidation>
        <@feedbackGradeValidation isGradeValidation gradeValidation />
      </#if>
    </#macro>

    <#function extractId user>
      <#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
        <#return user.userId />
      <#else>
        <#return user.warwickId! />
      </#if>
    </#function>

    <#macro markField assignment command>
    <@bs3form.labelled_form_group path="mark" labelText="Mark">
    <#if assignment.useMarkPoints>
    <@f.select path="mark" cssClass="form-control">
      <@f.option value="" label="" />
      <#local lastMarkClassName = "" />
    <#list assignment.availableMarkPoints?reverse as markPoint>
    <#if lastMarkClassName != markPoint.markClass.name>
      <option disabled="disabled">${markPoint.markClass.name}</option>
      <#local lastMarkClassName = markPoint.markClass.name />
    </#if>
      <@f.option value=markPoint.mark label="${markPoint.mark} (${markPoint.name})" />
    </#list>
    </@f.select>
    <#else>
      <div class="input-group">
        <@f.input type="number" path="mark" cssClass="form-control" />
        <div class="input-group-addon">%</div>
      </div>
    </#if>
    </@bs3form.labelled_form_group>

    <#if assignment.useMarkPoints>
    <@bs3form.labelled_form_group labelText="Mark descriptor">
    <@bs3form.static>
    <#list assignment.availableMarkingDescriptors as markingDescriptor>
      <div class="hidden" data-mark-points="<#list markingDescriptor.markPoints as markPoint>${markPoint.mark};</#list>">
        ${markingDescriptor.text}
      </div>
    </#list>
    </@bs3form.static>
    </@bs3form.labelled_form_group>

      <script nonce="${nonce()}">
        jQuery(function ($) {
          var $form = $('form[studentid=${command.student.userId}]');

          $form.find('select[name=mark]').on('change', function () {
            var mark = $(this).val();

            $form.find('[data-mark-points]').addClass('hidden').each(function () {
              if (mark !== '' && $(this).data('markPoints').indexOf(mark + ';') !== -1) {
                $(this).removeClass('hidden');
              }
            });
          }).trigger('change');
        });
      </script>
    </#if>
    </#macro>
</#escape>