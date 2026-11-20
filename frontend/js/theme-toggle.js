(() => {
  const button = document.getElementById('themeToggle');
  if (!button) return;

  let theme = 'light';
  try {
    theme = localStorage.getItem('hestora_theme') === 'dark' ? 'dark' : 'light';
  } catch { /* Keep the default theme when storage is unavailable. */ }

  const applyTheme = (value) => {
    document.documentElement.dataset.theme = value;
    button.setAttribute('aria-pressed', String(value === 'dark'));
    button.setAttribute('aria-label', value === 'dark' ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối');
  };

  applyTheme(theme);
  button.addEventListener('click', () => {
    theme = theme === 'dark' ? 'light' : 'dark';
    applyTheme(theme);
    try { localStorage.setItem('hestora_theme', theme); } catch { /* Theme still changes for this page. */ }
  });
})();
