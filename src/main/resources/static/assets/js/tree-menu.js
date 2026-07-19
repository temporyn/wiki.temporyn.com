/* 사이드바 우클릭 메뉴
   - 폴더 위에서: 그 폴더 안에 문서/하위 폴더 생성
   - 빈 공간에서: 최상위에 문서/폴더 생성
   - 전체 펼치기 / 접기
   수정·삭제는 볼트(Obsidian 등)에서 직접 처리한다. */
(function () {
  'use strict';

  var nav = document.getElementById('nav-tree');
  var menu = document.getElementById('context-menu');
  var scroll = document.querySelector('.sidebar-scroll');
  if (!nav || !menu || !scroll) return;

  var OPEN_KEY = 'temporyn-open';
  var csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
  var csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

  function post(url, body) {
    var headers = { 'Content-Type': 'application/json' };
    headers[csrfHeader] = csrfToken;
    return fetch(url, { method: 'POST', headers: headers, body: JSON.stringify(body) })
      .then(function (res) {
        if (res.ok) return res.json();
        return res.json().catch(function () { return {}; }).then(function (j) {
          throw new Error(j.message || j.error || ('요청 실패 (' + res.status + ')'));
        });
      });
  }

  function fail(e) { alert(e.message || '오류가 발생했습니다.'); }

  function createArticle(parentPath) {
    var name = prompt('새 문서 이름 (확장자 없이)');
    if (!name) return;
    post('/api/articles', { parentPath: parentPath, name: name })
      .then(function (j) { location.href = j.url; })
      .catch(fail);
  }

  function createDirectory(parentPath) {
    var name = prompt('새 폴더 이름');
    if (!name) return;
    post('/api/directories', { parentPath: parentPath, name: name })
      .then(function () { location.reload(); })
      .catch(fail);
  }

  function saveOpen(ids) {
    try { localStorage.setItem(OPEN_KEY, JSON.stringify(ids)); } catch (e) {}
  }

  function expandAll() {
    var ids = [];
    nav.querySelectorAll('.tree-dir').forEach(function (li) {
      li.classList.add('open');
      ids.push(li.dataset.path);
    });
    saveOpen(ids);
  }

  function collapseAll() {
    nav.querySelectorAll('.tree-dir').forEach(function (li) { li.classList.remove('open'); });
    saveOpen([]);
  }

  function hideMenu() { menu.hidden = true; }

  function showMenu(x, y, items) {
    menu.innerHTML = '';
    items.forEach(function (item) {
      var li = document.createElement('li');
      li.textContent = item.label;
      if (item.separated) li.className = 'context-menu-separated';
      li.addEventListener('click', function () { hideMenu(); item.action(); });
      menu.appendChild(li);
    });
    menu.style.left = x + 'px';
    menu.style.top = y + 'px';
    menu.hidden = false;
  }

  scroll.addEventListener('contextmenu', function (e) {
    e.preventDefault();

    var dir = e.target.closest('.tree-dir');
    var parentPath = dir ? dir.dataset.path : '';
    var inFolder = !!dir;

    showMenu(e.pageX, e.pageY, [
      {
        label: inFolder ? '새 문서 (' + dir.dataset.path + ')' : '새 문서',
        action: function () { createArticle(parentPath); }
      },
      {
        label: inFolder ? '새 하위 폴더' : '새 폴더',
        action: function () { createDirectory(parentPath); }
      },
      { label: '전체 펼치기', action: expandAll, separated: true },
      { label: '전체 접기', action: collapseAll }
    ]);
  });

  document.addEventListener('click', hideMenu);
  document.addEventListener('scroll', hideMenu, true);
  window.addEventListener('resize', hideMenu);
})();
