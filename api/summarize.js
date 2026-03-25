const STOP_WORDS = new Set([
  'a', 'an', 'and', 'are', 'as', 'at', 'be', 'by', 'for', 'from', 'has', 'he', 'in', 'is',
  'it', 'its', 'of', 'on', 'that', 'the', 'to', 'was', 'were', 'will', 'with', 'this', 'these',
  'those', 'or', 'if', 'then', 'than', 'you', 'your', 'we', 'our', 'they', 'their', 'them', 'i',
  'me', 'my', 'mine', 'can', 'could', 'should', 'would', 'about', 'into', 'over', 'under', 'also',
  'not', 'no', 'do', 'does', 'did', 'done', 'have', 'had', 'having', 'such', 'so', 'but', 'because'
]);

const TOKEN_PATTERN = /[A-Za-z0-9']+/g;

function splitSentences(text) {
  if (!text || !text.trim()) {
    return [];
  }

  const normalized = text.replace(/\s+/g, ' ').trim();
  return normalized
    .split(/(?<=[.!?])\s+/)
    .map((sentence) => sentence.trim())
    .filter(Boolean);
}

function tokenize(text) {
  if (!text) {
    return [];
  }

  const tokens = text.toLowerCase().match(TOKEN_PATTERN);
  return tokens || [];
}

function summarizeText(text, maxSentences) {
  const sentences = splitSentences(text);
  if (sentences.length === 0) {
    return '';
  }

  if (sentences.length <= maxSentences) {
    return sentences.join(' ');
  }

  const allWords = tokenize(text).filter(
    (word) => !STOP_WORDS.has(word) && !/^\d+$/.test(word)
  );

  if (allWords.length === 0) {
    return sentences.slice(0, Math.min(maxSentences, sentences.length)).join(' ');
  }

  const frequency = new Map();
  for (const word of allWords) {
    frequency.set(word, (frequency.get(word) || 0) + 1);
  }

  const maxFrequency = Math.max(...frequency.values(), 1);

  const scored = sentences.map((sentence, index) => {
    const sentenceWords = tokenize(sentence).filter((word) => !STOP_WORDS.has(word));
    if (sentenceWords.length === 0) {
      return { index, score: 0 };
    }

    let score = 0;
    for (const word of sentenceWords) {
      score += (frequency.get(word) || 0) / maxFrequency;
    }

    const lengthPenalty = 1 + Math.log(sentenceWords.length + 1);
    return { index, score: score / lengthPenalty };
  });

  const selectedIndices = scored
    .sort((a, b) => b.score - a.score)
    .slice(0, maxSentences)
    .map((entry) => entry.index)
    .sort((a, b) => a - b);

  return selectedIndices.map((index) => sentences[index]).join(' ');
}

function getFormValue(reqBody, key) {
  if (!reqBody) {
    return '';
  }

  if (typeof reqBody === 'string') {
    return new URLSearchParams(reqBody).get(key) || '';
  }

  if (typeof reqBody === 'object') {
    return String(reqBody[key] || '');
  }

  return '';
}

module.exports = (req, res) => {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method Not Allowed' });
  }

  const text = getFormValue(req.body, 'text').trim();
  const sentencesRaw = getFormValue(req.body, 'sentences');
  const sentences = Number.parseInt(sentencesRaw, 10);
  const maxSentences = Number.isInteger(sentences) && sentences > 0 ? sentences : 3;

  if (!text) {
    return res.status(400).json({ error: 'Input text is required' });
  }

  const summary = summarizeText(text, maxSentences);
  return res.status(200).json({ summary });
};
