const selectAll = document.getElementById('selectAll');
const checkboxes = document.querySelectorAll('.item-checkbox');


selectAll.addEventListener('change', () => {
  checkboxes.forEach(cb => cb.checked = selectAll.checked);
});


checkboxes.forEach(cb => {
  cb.addEventListener('change', () => {
    const allChecked = Array.from(checkboxes).every(c => c.checked);
    selectAll.checked = allChecked;
  });
});
