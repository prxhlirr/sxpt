export function injectRecorderScript(html: string, scriptPath: string): string {
  const scriptTag = `<script src="${scriptPath}"></script>`;

  if (html.includes(scriptTag)) {
    return html;
  }

  return html.replace(/<\/body>/i, `${scriptTag}</body>`) === html
    ? `${html}${scriptTag}`
    : html.replace(/<\/body>/i, `${scriptTag}</body>`);
}
