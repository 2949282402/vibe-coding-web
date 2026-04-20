import DOMPurify from 'dompurify';
import { marked } from 'marked';

marked.setOptions({
  gfm: true,
  breaks: true
});

function decorateCitations(source, citationCount, anchorPrefix) {
  if (!citationCount) {
    return source;
  }

  const normalizedPrefix = String(anchorPrefix || 'rag-source').trim() || 'rag-source';

  return String(source || '').replace(/\[(\d{1,2})\]/g, (match, value) => {
    const citationIndex = Number(value);
    if (citationIndex < 1 || citationIndex > citationCount) {
      return match;
    }

    return `<a class="citation-link" href="#${normalizedPrefix}-${citationIndex}" data-citation="${citationIndex}" data-anchor-prefix="${normalizedPrefix}">[${citationIndex}]</a>`;
  });
}

export function renderMarkdown(source, options = {}) {
  const raw = marked.parse(
    decorateCitations(source || '', options.citationCount, options.anchorPrefix),
    { async: false }
  );
  return DOMPurify.sanitize(raw, {
    USE_PROFILES: { html: true }
  });
}
