/* 사이드바 관리: 우클릭 컨텍스트 메뉴 / 드래그&드롭 이동 / 빈공간 메뉴 / 전체 펼치기·접기 */
(function () {
  'use strict';

  var nav = document.getElementById('nav-tree');
  var menu = document.getElementById('context-menu');
  var scroll = document.querySelector('.sidebar-scroll');
  if (!nav || !menu) return;

  var csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
  var csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

  function post(url, body) {
    var opts = { method: 'POST', headers: {} };
    opts.headers[csrfHeader] = csrfToken;
    if (body !== undefined) {
      opts.headers['Content-Type'] = 'application/json';
      opts.body = JSON.stringify(body);
    }
    return fetch(url, opts).then(function (res) {
      if (res.ok) return res;
      return res.json().catch(function () { return {}; }).then(function (j) {
        throw new Error(j.message || j.error || ('요청 실패 (' + res.status + ')'));
      });
    });
  }

  function reload() { location.reload(); }
  function fail(e) { alert(e.message || '오류가 발생했습니다.'); }

  /* ── 컨텍스트 메뉴 ─────────────────────────────────────────────── */
  function showMenu(x, y, items) {
    menu.innerHTML = '';
    items.forEach(function (it) {
      var li = document.createElement('li');
      li.textContent = it.label;
      li.addEventListener('click', function () { hideMenu(); it.action(); });
      menu.appendChild(li);
    });
    menu.style.left = x + 'px';
    menu.style.top = y + 'px';
    menu.hidden = false;
  }
  function hideMenu() { menu.hidden = true; }
  document.addEventListener('click', hideMenu);
  document.addEventListener('scroll', hideMenu, true);

  function emptyItems() {
    return [
      { label: '새 문서', action: function () { location.href = '/articles/new'; } },
      { label: '새 폴더', action: function () { createDir(null); } },
      { label: '전체 펼치기', action: expandAll },
      { label: '전체 접기', action: collapseAll }
    ];
  }

  nav.addEventListener('contextmenu', function (e) {
    e.preventDefault();
    var fileLi = e.target.closest('.tree-file');
    var dirLi = e.target.closest('.tree-dir');
    if (fileLi) {
      var aid = fileLi.dataset.id, aname = fileLi.dataset.name;
      showMenu(e.pageX, e.pageY, [
        { label: '수정', action: function () { location.href = '/articles/' + aid + '/edit'; } },
        { label: '이름 변경', action: function () { renameArticle(aid, aname); } },
        { label: '삭제', action: function () { deleteArticle(aid); } }
      ]);
    } else if (dirLi) {
      var did = dirLi.dataset.id, dname = dirLi.dataset.name;
      showMenu(e.pageX, e.pageY, [
        { label: '새 문서', action: function () { location.href = '/articles/new?directoryId=' + did; } },
        { label: '새 하위 폴더', action: function () { createDir(did); } },
        { label: '이름 변경', action: function () { renameDir(did, dname); } },
        { label: '삭제', action: function () { deleteDir(did); } }
      ]);
    } else {
      showMenu(e.pageX, e.pageY, emptyItems());
    }
  });

  /* 빈 공간 좌클릭 → 메뉴 */
  if (scroll) {
    scroll.addEventListener('click', function (e) {
      if (e.target.closest('.tree-toggle, .tree-file, a, button, input')) return;
      showMenu(e.pageX, e.pageY, emptyItems());
      e.stopPropagation();
    });
  }

  /* ── 작업들 ────────────────────────────────────────────────────── */
  function createDir(parentId) {
    var name = prompt('새 폴더 이름');
    if (!name) return;
    post('/api/directories', { name: name, parentId: parentId }).then(reload).catch(fail);
  }
  function renameDir(id, current) {
    var name = prompt('폴더 이름 변경', current);
    if (name == null) return;
    post('/api/directories/' + id + '/rename', { name: name }).then(reload).catch(fail);
  }
  function deleteDir(id) {
    if (!confirm('이 폴더를 삭제할까요?')) return;
    post('/api/directories/' + id + '/delete').then(function () { location.href = '/'; }).catch(fail);
  }
  function renameArticle(id, current) {
    var name = prompt('제목 변경', current);
    if (name == null) return;
    post('/api/articles/' + id + '/rename', { name: name }).then(reload).catch(fail);
  }
  function deleteArticle(id) {
    if (!confirm('이 문서를 삭제할까요?')) return;
    post('/api/articles/' + id + '/delete').then(function () { location.href = '/'; }).catch(fail);
  }

  /* ── 전체 펼치기 / 접기 ────────────────────────────────────────── */
  function saveOpen(ids) {
    try { localStorage.setItem('temporyn-open', JSON.stringify(ids)); } catch (e) {}
  }
  function expandAll() {
    var ids = [];
    nav.querySelectorAll('.tree-dir').forEach(function (li) { li.classList.add('open'); ids.push(li.dataset.path); });
    saveOpen(ids);
  }
  function collapseAll() {
    nav.querySelectorAll('.tree-dir').forEach(function (li) { li.classList.remove('open'); });
    saveOpen([]);
  }

  /* ── 드래그 & 드롭 이동 ────────────────────────────────────────── */
  var dragged = null;

  nav.addEventListener('dragstart', function (e) {
    var fileLi = e.target.closest('.tree-file');
    var toggle = e.target.closest('.tree-toggle');
    if (fileLi && e.target.closest('a')) {
      dragged = { type: 'file', id: fileLi.dataset.id };
    } else if (toggle) {
      dragged = { type: 'dir', id: toggle.closest('.tree-dir').dataset.id };
    } else {
      return;
    }
    e.dataTransfer.effectAllowed = 'move';
    try { e.dataTransfer.setData('text/plain', dragged.id); } catch (x) {}
  });

  nav.addEventListener('dragover', function (e) {
    if (!dragged) return;
    var dir = e.target.closest('.tree-dir');
    if (dir) { e.preventDefault(); clearDrop(); dir.classList.add('drop-target'); }
    else if (dragged.type === 'dir') { e.preventDefault(); clearDrop(); }
  });

  nav.addEventListener('dragleave', function (e) {
    var dir = e.target.closest('.tree-dir');
    if (dir) dir.classList.remove('drop-target');
  });

  nav.addEventListener('drop', function (e) {
    if (!dragged) return;
    e.preventDefault();
    clearDrop();
    var dir = e.target.closest('.tree-dir');
    var targetId = dir ? dir.dataset.id : null;
    var moving = dragged;
    dragged = null;
    if (moving.type === 'dir') {
      if (dir && dir.dataset.id === moving.id) return;
      post('/api/directories/' + moving.id + '/move', { parentId: targetId }).then(reload).catch(fail);
    } else {
      if (!dir) return;
      post('/api/articles/' + moving.id + '/move', { parentId: targetId }).then(reload).catch(fail);
    }
  });

  nav.addEventListener('dragend', function () { dragged = null; clearDrop(); });

  function clearDrop() {
    nav.querySelectorAll('.drop-target').forEach(function (d) { d.classList.remove('drop-target'); });
  }
})();
