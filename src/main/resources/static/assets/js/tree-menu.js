/* 사이드바 우클릭 메뉴 (읽기 전용 트리 조작)
   문서/폴더 생성·삭제 등 쓰기 기능은 볼트를 외부에서 관리하는 동안 제공하지 않는다. */
(function () {
  'use strict';

  var nav = document.getElementById('nav-tree');
  var menu = document.getElementById('context-menu');
  var scroll = document.querySelector('.sidebar-scroll');
  if (!nav || !menu || !scroll) return;

  var OPEN_KEY = 'temporyn-open';

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
      li.addEventListener('click', function () { hideMenu(); item.action(); });
      menu.appendChild(li);
    });
    menu.style.left = x + 'px';
    menu.style.top = y + 'px';
    menu.hidden = false;
  }

  scroll.addEventListener('contextmenu', function (e) {
    e.preventDefault();
    showMenu(e.pageX, e.pageY, [
      { label: '전체 펼치기', action: expandAll },
      { label: '전체 접기', action: collapseAll }
    ]);
  });

  document.addEventListener('click', hideMenu);
  document.addEventListener('scroll', hideMenu, true);
  window.addEventListener('resize', hideMenu);
})();
