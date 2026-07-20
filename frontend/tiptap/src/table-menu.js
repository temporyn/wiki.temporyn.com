const TABLE_ACTIONS = [
  { label: '왼쪽 열 추가', title: '왼쪽에 열 추가', run: (c) => c.addColumnBefore() },
  { label: '오른쪽 열 추가', title: '오른쪽에 열 추가', run: (c) => c.addColumnAfter() },
  { label: '열 삭제', title: '현재 열 삭제', run: (c) => c.deleteColumn() },
  { separator: true },
  { label: '위 행 추가', title: '위에 행 추가', run: (c) => c.addRowBefore() },
  { label: '아래 행 추가', title: '아래에 행 추가', run: (c) => c.addRowAfter() },
  { label: '행 삭제', title: '현재 행 삭제', run: (c) => c.deleteRow() },
  { separator: true },
  { label: '헤더 행', title: '헤더 행 토글', run: (c) => c.toggleHeaderRow() },
  { label: '셀 병합/분할', title: '선택 셀 병합 또는 분할', run: (c) => c.mergeOrSplit() },
  { separator: true },
  { label: '표 삭제', title: '표 전체 삭제', className: 'is-danger', run: (c) => c.deleteTable() },
];

export function buildTableMenu(getEditor) {
  const element = document.createElement('div');
  element.className = 'tiptap-table-menu';

  TABLE_ACTIONS.forEach((action) => {
    if (action.separator) {
      const divider = document.createElement('span');
      divider.className = 'tiptap-table-sep';
      element.appendChild(divider);
      return;
    }
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'tiptap-table-btn' + (action.className ? ' ' + action.className : '');
    button.title = action.title;
    button.textContent = action.label;
    button.addEventListener('mousedown', (event) => {
      event.preventDefault();
      const editor = getEditor();
      if (!editor) return;
      action.run(editor.chain().focus()).run();
    });
    element.appendChild(button);
  });

  return { element };
}
