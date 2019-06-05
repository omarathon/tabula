/* eslint-env browser */
import $ from 'jquery';

class MitCircsOutcomesForm {

  constructor(form) {
    this.form = form;
    this.init();
  }

  init() {
    const {form} = this;
    const $form = $(form);

    $form
      .find(':input[name="outcomeGrading"]')
      .on('input change', () => {
        const $checked = $(':input[name="outcomeGrading"]:checked');
        $('.mitcircs-outcomes-form__rejection-reasons').collapse($checked.val() === "Rejected" ? 'show' : 'hide');
      })
      .trigger('change');
  }

}

function init() {
  $('.mitcircs-outcomes-form').each((i, el) => {
    $(el).data('tabula.mitCircsOutcomesForm', new MitCircsOutcomesForm(el));
  });
}

$(() => init());