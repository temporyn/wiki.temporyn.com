/* Markdown 편집기 (EasyMDE / CodeMirror)
   - 문서 전체가 하나의 연속 편집 영역: 자유로운 타이핑 / 방향키 이동 / 되돌리기 / IME 정상
   - 편집 중에도 제목·굵게·코드 등이 에디터 안에서 그대로 서식으로 보인다
   - 미리보기는 서버의 동일한 렌더러를 사용해 저장 화면과 완전히 같게 표시된다 */
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
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ markdown: markdown })
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
    autofocus: false,
    sideBySideFullscreen: false,
    placeholder: 'Markdown 으로 작성하세요',
    previewClass: ['editor-preview', 'doc-body'],
    previewRender: function (plainText, preview) {
      // 나란히 보기에서는 타이핑마다 호출되므로 디바운스한다.
      clearTimeout(previewTimer);
      previewTimer = setTimeout(function () { renderPreview(plainText, preview); }, 150);
      // 비동기로 직접 채우므로 EasyMDE 가 반환값으로 덮어쓰지 않도록 undefined 를 반환한다.
      return undefined;
    }
  });

  var sideBtn = document.getElementById('md-toggle-side');
  var previewBtn = document.getElementById('md-toggle-preview');

  function syncButtons() {
    if (sideBtn) sideBtn.classList.toggle('is-active', editor.isSideBySideActive());
    if (previewBtn) previewBtn.classList.toggle('is-active', editor.isPreviewActive());
  }

  /* EasyMDE 는 활성 클래스를 setTimeout 안에서 붙이므로,
     레이아웃이 바뀐 뒤에 CodeMirror 를 다시 측정해야 첫 클릭에 양쪽이 제대로 잡힌다. */
  function settleLayout() {
    requestAnimationFrame(function () {
      editor.codemirror.refresh();
      syncButtons();
      setTimeout(function () {
        editor.codemirror.refresh();
        syncButtons();
      }, 60);
    });
  }

  if (sideBtn) {
    sideBtn.addEventListener('click', function () {
      editor.toggleSideBySide();
      settleLayout();
    });
  }
  if (previewBtn) {
    previewBtn.addEventListener('click', function () {
      editor.togglePreview();
      settleLayout();
    });
  }

  /* 저장 시 원문 Markdown 을 그대로 전송 */
  var form = field.closest('form');
  if (form) {
    form.addEventListener('submit', function () {
      field.value = editor.value();
    });
  }
})();
