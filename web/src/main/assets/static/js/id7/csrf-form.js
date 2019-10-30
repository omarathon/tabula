/* eslint-env browser */
import $ from 'jquery';

const csrfTokenName = 'urn:websignon:csrf';

export default class CsrfForm {
  static generate() {
    const $meta = $('meta[name=_csrf]');
    let csrfTokenValue = '';
    if ($meta.length > 0 && $meta.attr('content') !== undefined) {
      csrfTokenValue = $meta.attr('content');
    }
    const $form = $('<form>').attr('method', 'POST');
    $form.append($('<input>').attr('type', 'hidden').attr('value', csrfTokenValue).attr('name', csrfTokenName));
    return $form;
  }

  static serializeWithoutCsrf(formOrInputs) {
    const $this = $(formOrInputs);
    const $elements = $this.is('form') ? $this.find(':input') : $this;
    return $elements.filter((i, e) => ($(e).attr('name') || '').indexOf(csrfTokenName) === -1).serialize();
  }
}
