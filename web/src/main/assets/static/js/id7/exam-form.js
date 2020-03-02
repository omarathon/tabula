/* eslint-env browser */
import $ from 'jquery';
import _ from 'lodash-es';

class ExamForm {
  constructor(form) {
    this.form = form;
    this.init();
  }

  init() {
    const { form } = this;
    const $form = $(form);

    $form.find('.component-filters').on('change', 'input[type=checkbox]', () => {
      const showNonExams = $('input[name=showNonExam]').prop('checked');
      const showNotInUse = $('input[name=showNotInUse]').prop('checked');
      const $maybeHidden = $('.non-exam,.not-in-use');
      const $toShow = $maybeHidden
        .filter((i, el) => showNonExams || !$(el).hasClass('non-exam'))
        .filter((i, el) => showNotInUse || !$(el).hasClass('not-in-use'));
      const $toHide = $maybeHidden.not($toShow);
      $toShow.show();
      $toHide.hide();
    });
    $form.find('.component-filters input[type=checkbox]').trigger('change');

    $form.find('input[name=occurrences]').on('change', this.updateOccurrences.bind(this)).first().trigger('change');
    $form.find('input[name=assessmentComponents]').on('change', this.updateEnrolledStudents.bind(this)).first().trigger('change');
  }

  selectedOccurrences() {
    return $(this.form).serializeArray().filter(field => field.name === 'occurrences').map(field => field.value);
  }

  updateEnrolledStudents() {
    const { form } = this;
    const $form = $(form);
    const $container = $form.find('.enrolled-students');
    const $total = $container.find('.student-total');
    const $duplicateAlert = $container.find('.alert.duplicates');
    const $noStudentsAlert = $container.find('.alert.no-students');
    const $studentTable = $container.find('table.students');
    const $studentTableBody = $studentTable.find('tbody');
    $studentTableBody.empty();

    const occurrences = this.selectedOccurrences();
    const students = $('.assessment-component')
      .filter((i, component) => $(component).find('input[name=assessmentComponents]').is(':checked'))
      .map((i, component) => occurrences.map(occurrence => $(component).data('students')[occurrence]).flat()).get();

    const noDupes = _.uniqBy(students, 'universityId');
    noDupes.forEach((student) => {
      const $row = $('<tr></tr>');
      $row.append($(`<td>${student.firstName}</td>`));
      $row.append($(`<td>${student.lastName}</td>`));
      $row.append($(`<td>${student.universityId}</td>`));
      $row.append($(`<td>${student.usercode}</td>`));
      $row.append($(`<td>${student.seatNumber}</td>`));
      $studentTableBody.append($row);
    });

    $duplicateAlert.toggle(noDupes.length !== students.length);

    if (students.length === 0) {
      $studentTable.hide();
      $noStudentsAlert.show();
      $total.hide();
    } else {
      $studentTable.show();
      $noStudentsAlert.hide();
      $total.show().find('.count').html(noDupes.length);
    }
  }

  updateOccurrences() {
    const { form } = this;
    const $form = $(form);
    const occurrences = this.selectedOccurrences();
    $('.assessment-component', $form).each((i, component) => {
      const $component = $(component);
      const students = occurrences.map(occurrence => $component.data('students')[occurrence]).flat();
      $component.find('td.students').html(students.length);
    });

    this.updateEnrolledStudents();
  }
}

function init() {
  $('.exam-form').each((i, el) => {
    $(el).data('tabula.ExamForm', new ExamForm(el));
  });
  $('.exam-form.fix-area').fixHeaderFooter();
}

$(() => init());
