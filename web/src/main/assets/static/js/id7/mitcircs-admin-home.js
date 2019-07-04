/* eslint-env browser */
import $ from 'jquery';

class MitCircsAdminHome {
  constructor(form) {
    this.form = form;
    this.init();
  }

  init() {
    const { form } = this;
    const $form = $(form);

    $form.on('change', '.check-all', (e) => {
      const $checkAll = $(e.target);
      $form.find('input[name=submissions]').prop('checked', $checkAll.is(':checked')).last().trigger('change');
    });

    $form.on('change', 'input[name=submissions]', () => {
      const $checkboxes = $form.find('input[name=submissions]');
      $form.find('.requires-selected').prop('disabled', !$checkboxes.is(':checked'));
    });

    $form.on('change', 'select[name=panel]', (e) => {
      const $select = $(e.target);
      const formAction = $select.find(':selected').data('formaction');
      const $submit = $form.find('.mitcircs-submission-action__buttons__add-to-panel');
      $submit.prop('disabled', !formAction);
      $submit.attr('formaction', formAction);
    });
  }
}

function init() {
  $('.mitcircs-submission-actions').each((i, el) => {
    $(el).data('tabula.mitCircsAdminHome', new MitCircsAdminHome(el));
  });

  $('.table-sortable').sortableTable();
}

$(() => init());
