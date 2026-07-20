const TABLE_ACTIONS = [
  { label: 'Insert column left', title: 'Insert a column to the left', run: (c) => c.addColumnBefore() },
  { label: 'Insert column right', title: 'Insert a column to the right', run: (c) => c.addColumnAfter() },
  { label: 'Delete column', title: 'Delete the current column', run: (c) => c.deleteColumn() },
  { separator: true },
  { label: 'Insert row above', title: 'Insert a row above', run: (c) => c.addRowBefore() },
  { label: 'Insert row below', title: 'Insert a row below', run: (c) => c.addRowAfter() },
  { label: 'Delete row', title: 'Delete the current row', run: (c) => c.deleteRow() },
  { separator: true },
  { label: 'Header row', title: 'Toggle header row', run: (c) => c.toggleHeaderRow() },
  { label: 'Merge/split cells', title: 'Merge or split selected cells', run: (c) => c.mergeOrSplit() },
  { separator: true },
  { label: 'Delete table', title: 'Delete the entire table', className: 'is-danger', run: (c) => c.deleteTable() },
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
