/* eslint-env browser */
import $ from 'jquery';
import moment from 'moment-timezone';

class MitCircsForm {
  constructor(form) {
    this.form = form;
    this.init();
  }

  init() {
    const { form } = this;
    const $form = $(form);

    // Issue type fields
    $form
      .find(':input[name="issueTypes"]')
      .on('input change', () => {
        const checked = $(':input[name="issueTypes"]:checked').closest('.checkbox');
        const $dl = $('<dl></dl>');
        checked.each((i, issueType) => {
          const $issueType = $(issueType);
          $dl.append($('<dt></dt>').text($issueType.find('label').text().trim()));
          $dl.append($('<dd></dd>').text($issueType.data('evidenceguidance')));
        });
        const $hintContainer = $('.mitcircs-form__fields__section__evidence-upload .mitcircs-form__fields__section__hint');
        $hintContainer.find('dl').remove();
        $hintContainer.append($dl);
      })
      .trigger('change');

    $form
      .find(':input[name="issueTypes"][value="Other"]')
      .on('input change', e => $form.find(':input[name="issueTypeDetails"]').prop('disabled', !$(e.target).is(':checked')));

    // End date or ongoing
    $form
      .find('input[name="noEndDate"]')
      .on('input change', (e) => {
        // This will get fired twice as there are two radio buttons, so filter it to only
        // be the currently selected one
        const $radio = $(e.target);
        if ($radio.is(':checked')) {
          $form.find('input[name="endDate"]').prop('disabled', $radio.val() === 'true');
        }
      })
      .trigger('change');

    // Contacted fields
    $form
      .find('input[name="contacted"]')
      .on('input change', () => {
        const val = $form.find('input[name="contacted"]:checked').val();
        if (val) {
          $form.find('.mitcircs-form__fields__contact-subfield--yes').toggle(val === 'true');
          $form.find('.mitcircs-form__fields__contact-subfield--no').toggle(val !== 'true');
        }
      })
      .trigger('change');

    $form
      .find(':input[name="contacted"][value="Other"]')
      .on('input change', e => $form.find(':input[name="contactOther"]').prop('disabled', !$(e.target).is(':checked')))
      .trigger('change');

    // Removing attachments
    $form
      .on('click', '.remove-attachment', (e) => {
        e.preventDefault();

        const $target = $(e.target);
        const $attachmentContainer = $target.closest('li.attachment');

        $target.closest('.modal')
          .on('hidden.bs.modal', () => $attachmentContainer.remove());
      });

    this.initAssessmentsTable();
    $form.on('submit', this.processAssessmentsTableFormFieldsForSubmission.bind(this));
  }

  initAssessmentsTable() {
    const { form } = this;
    const $form = $(form);

    this.$assessmentsTable = $form.find('.mitcircs-form__fields__section__assessments-table');
    this.lastStartDate = undefined;
    this.lastEndDate = undefined;

    const $dateFields = $form.find('input[name="noEndDate"], input[name="endDate"], input[name="startDate"]');
    $dateFields.on('input change', this.updateAssessments.bind(this));
    this.updateAssessments();

    const $tfoot = this.$assessmentsTable.children('tfoot');
    $tfoot.find('button').on('click', () => {
      $tfoot.find('td').removeClass('has-error');

      const $selectedModule = $tfoot.find('#new-assessment-module option:selected');
      if ($selectedModule.length === 0 || !$selectedModule.text()) {
        $tfoot.find('#new-assessment-module').attr('aria-invalid', 'true').closest('td').addClass('has-error');
        return;
      }

      const $titleField = $tfoot.find('#new-assessment-name');
      if ($titleField.val().trim().length === 0) {
        $titleField.attr('aria-invalid', 'true').closest('td').addClass('has-error');
        return;
      }

      const module = { code: $selectedModule.attr('value').toUpperCase(), name: $selectedModule.data('name') };
      const academicYear = $selectedModule.closest('optgroup').attr('label');

      let deadline = $tfoot.find('#new-assessment-deadline').val();
      if (deadline.length > 0) {
        deadline = moment(deadline, 'DD-MMM-YYYY').format('YYYY-MM-DD');
      } else {
        deadline = undefined;
      }

      const assessmentComponent = {
        name: $titleField.val().trim(),
        inUse: true,
        selected: true,
        assessmentType: this.$assessmentsTable.find('tbody#assessment-table-exams').hasClass('active') ? 'E' : 'A',
      };
      const upstreamAssessmentGroups = [];
      const tabulaAssignments = [];

      this.updateAssessmentsTable([{
        module,
        academicYear,
        name: assessmentComponent.name,
        deadline,
        assessmentComponent,
        upstreamAssessmentGroups,
        tabulaAssignments,
      }], false);

      $tfoot.find(':input').not(':button, :submit, :reset, :hidden, :checkbox, :radio').val('');
      $tfoot.find(':checkbox, :radio').prop('checked', false);
    });
  }

  updateAssessments() {
    const { form } = this;
    const $form = $(form);

    const startDate = $form.find('input[name="startDate"]').val();
    let endDate = $form.find('input[name="endDate"]').val();
    const noEndDate = $form.find('input[name="noEndDate"]').is(':checked');

    if (noEndDate) endDate = undefined;

    if (startDate === this.lastStartDate && endDate === this.lastEndDate) return;
    this.lastStartDate = startDate;
    this.lastEndDate = endDate;

    if (startDate && (endDate || noEndDate)) {
      $.post(
        this.$assessmentsTable.data('endpoint'),
        { startDate, endDate },
        this.updateAssessmentsTable.bind(this),
      );
    } else {
      this.updateAssessmentsTable([]);
    }
  }

  static parseAcademicYear(academicYear) {
    const m = academicYear.match(/^\d{2}(\d{2})$/);
    if (m) {
      const startYear = parseInt(m[1], 10);
      const endYear = (startYear + 1) % 100;

      const padded = i => (i < 10 ? `0${i}` : i.toString());

      return `${padded(startYear)}/${padded(endYear)}`;
    }

    return m;
  }

  updateAssessmentsTable(results, removeExisting = true) {
    const { $assessmentsTable } = this;
    const $tbody = $assessmentsTable.children('tbody');

    if (removeExisting) {
      // Go through the existing rows first and remove any that are unchecked and not
      // included in results
      const $rows = $tbody.children('tr');
      $rows.each((i, tr) => {
        const $tr = $(tr);
        const isChecked = $tr.children('td').first().find('input[type="checkbox"]').is(':checked');

        const moduleCode = $tr.find('input[name$="moduleCode"]').val();
        if (!moduleCode) return; // A custom added row, not removed

        const sequence = $tr.find('input[name$="sequence"]').val();
        const academicYear = MitCircsForm.parseAcademicYear($tr.find('input[name$="academicYear"]').val());

        const matching = results.find((item) => {
          const matchingUag = item.upstreamAssessmentGroups
            .find(uag => uag.academicYear === academicYear);

          return item.assessmentComponent.sequence === sequence
            && matchingUag !== undefined;
        });

        if (matching === undefined) {
          if (!isChecked) {
            $tr.remove();
          }
        } else {
          if (!isChecked) {
            // Update the name
            $tr.find('.mitcircs-form__fields__section__assessments-table__name')
              .text(matching.name)
              .prepend($('<input />').attr({
                type: 'hidden',
                name: 'name',
                value: matching.name,
              }));

            // Update the deadline
            if (matching.deadline) {
              $tr.find('input[name$="deadline"]').attr('value', moment(matching.deadline, 'YYYY-MM-DD').format('DD-MMM-YYYY'));
            }
          }

          results.splice(results.indexOf(matching), 1);
        }
      });
    }

    // Add new rows for the remaining items
    results.forEach((item) => {
      const {
        module,
        academicYear,
        name,
        deadline,
        assessmentComponent,
        // upstreamAssessmentGroups,
        // tabulaAssignments,
      } = item;

      const $tr = $('<tr />');

      const $checkboxCell = $('<td />').addClass('mitcircs-form__fields__section__assessments-table__checkbox');
      $tr.append($checkboxCell);
      if (assessmentComponent.moduleCode && assessmentComponent.sequence) {
        $checkboxCell
          .append($('<input />').attr({
            type: 'hidden',
            name: 'moduleCode',
            value: assessmentComponent.moduleCode,
          }))
          .append($('<input />').attr({
            type: 'hidden',
            name: 'sequence',
            value: assessmentComponent.sequence,
          }))
          .append($('<input />').attr({
            type: 'hidden',
            name: 'assessmentType',
            value: assessmentComponent.assessmentType,
          }))
          .append($('<input />').attr({
            type: 'hidden',
            name: 'academicYear',
            value: academicYear,
          }));
      } else {
        $checkboxCell
          .append($('<input />').attr({
            type: 'hidden',
            name: 'moduleCode',
            value: module.code,
          }))
          .append($('<input />').attr({
            type: 'hidden',
            name: 'assessmentType',
            value: assessmentComponent.assessmentType,
          }))
          .append($('<input />').attr({
            type: 'hidden',
            name: 'academicYear',
            value: academicYear,
          }));
      }
      const $checkbox = $('<input />').attr('type', 'checkbox').prop('checked', !!assessmentComponent.selected);
      $checkboxCell.append($checkbox);

      const $moduleCell = $('<td />').addClass('mitcircs-form__fields__section__assessments-table__module');
      $tr.append($moduleCell);
      $moduleCell
        .append($('<span />').addClass('mod-code').text(module.code.toUpperCase()))
        .append(' ')
        .append($('<span />').addClass('mod-name').text(`${module.name} (${academicYear})`));

      const $nameCell = $('<td />').addClass('mitcircs-form__fields__section__assessments-table__name');
      $tr.append($nameCell);
      $nameCell.text(name)
        .prepend($('<input />').attr({
          type: 'hidden',
          name: 'name',
          value: name,
        }));

      const $deadlineCell = $('<td />').addClass('mitcircs-form__fields__section__assessments-table__deadline');
      $tr.append($deadlineCell);

      const $datePicker = $('<input />').attr({
        name: 'deadline',
        type: 'text',
        value: deadline ? moment(deadline, 'YYYY-MM-DD').format('DD-MMM-YYYY') : '',
      }).addClass('form-control input-sm date-picker');
      $deadlineCell.append(
        $('<div />').addClass('input-group')
          .append($datePicker)
          .append($('<span />').addClass('input-group-addon').append($('<i />').addClass('fa fa-calendar'))),
      );

      if (assessmentComponent.assessmentType === 'E') {
        $tbody.filter('#assessment-table-exams').append($tr);
      } else {
        $tbody.filter('#assessment-table-assignments').append($tr);
      }

      $checkbox.on('input change', () => $datePicker.prop('disabled', !$checkbox.is(':checked'))).trigger('change');

      $deadlineCell.find('input.date-picker').tabulaDatePicker();
    });
  }

  processAssessmentsTableFormFieldsForSubmission() {
    const { $assessmentsTable } = this;
    const $rows = $assessmentsTable.children('tbody').children('tr');

    let count = 0;
    $rows.each((i, tr) => {
      const $tr = $(tr);
      const isChecked = $tr.children('td').first().find('input[type="checkbox"]').is(':checked');
      if (!isChecked) {
        // Remove the names from all the inputs so they don't get submitted
        $tr.find(':input').removeAttr('name').prop('disabled', true);
        return;
      }

      $tr.find('input[name$="moduleCode"]').attr('name', `affectedAssessments[${count}].moduleCode`);
      $tr.find('input[name$="sequence"]').attr('name', `affectedAssessments[${count}].sequence`);
      $tr.find('input[name$="assessmentType"]').attr('name', `affectedAssessments[${count}].assessmentType`);
      $tr.find('input[name$="academicYear"]').attr('name', `affectedAssessments[${count}].academicYear`);
      $tr.find('input[name$="name"]').attr('name', `affectedAssessments[${count}].name`);
      $tr.find('input[name$="deadline"]').attr('name', `affectedAssessments[${count}].deadline`);

      count += 1;
    });
  }
}

function init() {
  $('#mitigatingCircumstancesForm').each((i, el) => {
    $(el).data('tabula.mitigatingCircumstancesForm', new MitCircsForm(el));
  });
}

$(() => init());
