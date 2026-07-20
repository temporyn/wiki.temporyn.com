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

  /* 현재 보고 있는(또는 편집 중인) 문서 경로 */
  var currentPath = document.body.dataset.currentPath || '';
  var currentMode = document.body.dataset.currentMode === 'edit' ? 'edit' : 'view';

  /* 경로에 공백·한글이 있어도 안전하도록 세그먼트별로 인코딩한다. */
  function pathUrl(path) {
    return '/' + currentMode + '/' + path.split('/').map(encodeURIComponent).join('/');
  }

  /* 이름 변경·이동으로 현재 문서의 경로가 바뀌면 새 경로로 이동하고, 아니면 트리만 갱신한다. */
  function followArticle(oldPath, newPath) {
    if (currentPath === oldPath) location.href = pathUrl(newPath);
    else reload();
  }

  function followDirectory(oldPath, newPath) {
    if (currentPath === oldPath || currentPath.indexOf(oldPath + '/') === 0) {
      location.href = pathUrl(newPath + currentPath.substring(oldPath.length));
    } else {
      reload();
    }
  }

  /* ── 생성 ──────────────────────────────────────────────────────────── */
  function createArticle(parentPath) {
    var name = prompt('새 문서 이름 (확장자 없이)');
    if (!name) return;
    post('/api/articles', { parentPath: parentPath, name: name })
      .then(function (j) { location.href = '/view/' + j.path.split('/').map(encodeURIComponent).join('/'); })
      .catch(fail);
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
      .then(function (j) { followArticle(path, j.path); }).catch(fail);
  }

  function renameDirectory(path, current) {
    var name = prompt('폴더 이름 변경', current);
    if (name == null) return;
    post('/api/directories/rename', { path: path, name: name })
      .then(function (j) { followDirectory(path, j.path); }).catch(fail);
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

    // Show first to measure, then keep the menu inside the viewport.
    menu.style.left = '0';
    menu.style.top = '0';
    menu.hidden = false;

    var margin = 8;
    var rect = menu.getBoundingClientRect();
    var left = x;
    var top = y;
    if (left + rect.width > window.innerWidth - margin) {
      left = Math.max(margin, x - rect.width);
    }
    if (top + rect.height > window.innerHeight - margin) {
      top = Math.max(margin, y - rect.height);
    }
    menu.style.left = left + 'px';
    menu.style.top = top + 'px';
  }

  scroll.addEventListener('contextmenu', function (e) {
    e.preventDefault();
    var file = e.target.closest('.tree-file');
    var dir = e.target.closest('.tree-dir');
    var items;

    if (file) {
      var fPath = file.dataset.path, fName = file.dataset.name;
      items = [
        {
          label: '수정',
          action: function () {
            location.href = '/edit/' + fPath.split('/').map(encodeURIComponent).join('/');
          }
        },
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
      /* 빈 공간에서만 트리 전체 조작을 제공한다. */
      items = [
        { label: '새 문서', action: function () { createArticle(''); } },
        { label: '새 폴더', action: function () { createDirectory(''); } },
        { label: '전체 펼치기', action: expandAll, separated: true },
        { label: '전체 접기', action: collapseAll }
      ];
    }

    showMenu(e.clientX, e.clientY, items);
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

    if (moving.type === 'article') {
      post('/api/articles/move', { path: moving.path, targetPath: targetPath })
        .then(function (j) { followArticle(moving.path, j.path); }).catch(fail);
    } else {
      post('/api/directories/move', { path: moving.path, targetPath: targetPath })
        .then(function (j) { followDirectory(moving.path, j.path); }).catch(fail);
    }
  });

  nav.addEventListener('dragend', function () { dragged = null; clearDropTargets(); });
})();
