/* 사이드바 조작
   - 우클릭: 문서(수정/이름 변경), 폴더(새 문서·하위 폴더/이름 변경), 빈 공간(최상위 생성)
   - 드래그&드롭: 문서·폴더를 다른 폴더로 이동 (빈 공간에 놓으면 최상위)
   삭제는 볼트(Obsidian 등)에서 직접 처리한다. */
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
  function reload() { location.reload(); }

  /* ── 생성 ──────────────────────────────────────────────────────────── */
  function createArticle(parentPath) {
    var name = prompt('새 문서 이름 (확장자 없이)');
    if (!name) return;
    post('/api/articles', { parentPath: parentPath, name: name })
      .then(function (j) { location.href = j.url; }).catch(fail);
  }

  function createDirectory(parentPath) {
    var name = prompt('새 폴더 이름');
    if (!name) return;
    post('/api/directories', { parentPath: parentPath, name: name }).then(reload).catch(fail);
  }

  /* ── 이름 변경 ─────────────────────────────────────────────────────── */
  function renameArticle(path, current) {
    var name = prompt('문서 이름 변경', current);
    if (name == null) return;
    post('/api/articles/rename', { path: path, name: name })
      .then(function (j) { location.href = j.url; }).catch(fail);
  }

  function renameDirectory(path, current) {
    var name = prompt('폴더 이름 변경', current);
    if (name == null) return;
    post('/api/directories/rename', { path: path, name: name }).then(reload).catch(fail);
  }

  /* ── 펼치기 / 접기 ─────────────────────────────────────────────────── */
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

  /* ── 컨텍스트 메뉴 ─────────────────────────────────────────────────── */
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
    var file = e.target.closest('.tree-file');
    var dir = e.target.closest('.tree-dir');
    var items;

    if (file) {
      var fPath = file.dataset.path, fName = file.dataset.name;
      items = [
        { label: '수정', action: function () { location.href = '/edit/' + fPath; } },
        { label: '이름 변경', action: function () { renameArticle(fPath, fName); } }
      ];
    } else if (dir) {
      var dPath = dir.dataset.path, dName = dir.dataset.name;
      items = [
        { label: '새 문서', action: function () { createArticle(dPath); } },
        { label: '새 하위 폴더', action: function () { createDirectory(dPath); } },
        { label: '이름 변경', action: function () { renameDirectory(dPath, dName); } }
      ];
    } else {
      items = [
        { label: '새 문서', action: function () { createArticle(''); } },
        { label: '새 폴더', action: function () { createDirectory(''); } }
      ];
    }

    items.push({ label: '전체 펼치기', action: expandAll, separated: true });
    items.push({ label: '전체 접기', action: collapseAll });
    showMenu(e.pageX, e.pageY, items);
  });

  document.addEventListener('click', hideMenu);
  document.addEventListener('scroll', hideMenu, true);
  window.addEventListener('resize', hideMenu);

  /* ── 드래그 & 드롭 이동 ────────────────────────────────────────────── */
  var dragged = null;

  function clearDropTargets() {
    nav.querySelectorAll('.drop-target').forEach(function (el) { el.classList.remove('drop-target'); });
  }

  nav.addEventListener('dragstart', function (e) {
    var file = e.target.closest('.tree-file');
    var toggle = e.target.closest('.tree-toggle');
    if (file && e.target.closest('a')) {
      dragged = { type: 'article', path: file.dataset.path };
    } else if (toggle) {
      dragged = { type: 'directory', path: toggle.closest('.tree-dir').dataset.path };
    } else {
      return;
    }
    e.dataTransfer.effectAllowed = 'move';
    try { e.dataTransfer.setData('text/plain', dragged.path); } catch (x) {}
  });

  scroll.addEventListener('dragover', function (e) {
    if (!dragged) return;
    e.preventDefault();
    clearDropTargets();
    var dir = e.target.closest('.tree-dir');
    if (dir) dir.classList.add('drop-target');
  });

  scroll.addEventListener('drop', function (e) {
    if (!dragged) return;
    e.preventDefault();
    clearDropTargets();

    var dir = e.target.closest('.tree-dir');
    var targetPath = dir ? dir.dataset.path : '';
    var moving = dragged;
    dragged = null;

    if (moving.type === 'directory' && dir && dir.dataset.path === moving.path) return;

    var endpoint = moving.type === 'article' ? '/api/articles/move' : '/api/directories/move';
    post(endpoint, { path: moving.path, targetPath: targetPath }).then(reload).catch(fail);
  });

  nav.addEventListener('dragend', function () { dragged = null; clearDropTargets(); });
})();
