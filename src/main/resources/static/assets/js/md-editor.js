/* 문서 편집기 (EasyMDE / CodeMirror)
   - 문서 전체가 하나의 연속 편집 영역: 타이핑 / 방향키 / 되돌리기 / IME 정상
   - 미리보기는 조회 화면과 같은 서버 렌더러를 사용해 결과가 동일하다 */
(function () {
  'use strict';

  var field = document.getElementById('content');
  if (!field || typeof EasyMDE === 'undefined') return;

  var csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
  var csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

  var previewTimer = null;

  function renderPreview(markdown, target) {
    var headers = { 'Content-Type': 'application/json' };
    headers[csrfHeader] = csrfToken;
    return fetch('/api/markdown/preview', {
      method: 'POST', headers: headers, body: JSON.stringify({ markdown: markdown })
    })
      .then(function (r) { return r.json(); })
      .then(function (j) { target.innerHTML = j.html || ''; })
      .catch(function () { target.innerHTML = '<p>미리보기를 불러오지 못했습니다.</p>'; });
  }

  var editor = new EasyMDE({
    element: field,
    toolbar: false,
    status: false,
    spellChecker: false,
    autoDownloadFontAwesome: false,
    lineWrapping: true,
    autofocus: true,
    sideBySideFullscreen: false,
    placeholder: 'Markdown 으로 작성하세요',
    previewClass: ['editor-preview', 'doc-body'],
    previewRender: function (plainText, preview) {
      clearTimeout(previewTimer);
      previewTimer = setTimeout(function () { renderPreview(plainText, preview); }, 150);
      return undefined;
    }
  });

  var sideBtn = document.getElementById('md-toggle-side');
  var previewBtn = document.getElementById('md-toggle-preview');

  function syncButtons() {
    if (sideBtn) sideBtn.classList.toggle('is-active', editor.isSideBySideActive());
    if (previewBtn) previewBtn.classList.toggle('is-active', editor.isPreviewActive());
  }

  /* 활성 클래스가 setTimeout 안에서 붙으므로 레이아웃 반영 후 다시 측정한다. */
  function settleLayout() {
    requestAnimationFrame(function () {
      editor.codemirror.refresh();
      syncButtons();
      setTimeout(function () { editor.codemirror.refresh(); syncButtons(); }, 60);
    });
  }

  if (sideBtn) {
    sideBtn.addEventListener('click', function () { editor.toggleSideBySide(); settleLayout(); });
  }
  if (previewBtn) {
    previewBtn.addEventListener('click', function () { editor.togglePreview(); settleLayout(); });
  }

  var form = field.closest('form');
  if (form) {
    form.addEventListener('submit', function () { field.value = editor.value(); });
  }
})();
