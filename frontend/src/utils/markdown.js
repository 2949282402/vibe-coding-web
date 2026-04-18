import DOMPurify from 'dompurify';
import { marked } from 'marked';

marked.setOptions({
  gfm: true,
  breaks: true
});

export function renderMarkdown(source) {
  const raw = marked.parse(source || '', { async: false });
  return DOMPurify.sanitize(raw, {
    USE_PROFILES: { html: true }
  });
}
