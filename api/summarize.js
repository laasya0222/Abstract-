const STOP_WORDS = new Set([
  'a', 'an', 'and', 'are', 'as', 'at', 'be', 'by', 'for', 'from', 'has', 'he', 'in', 'is',
  'it', 'its', 'of', 'on', 'that', 'the', 'to', 'was', 'were', 'will', 'with', 'this', 'these',
  'those', 'or', 'if', 'then', 'than', 'you', 'your', 'we', 'our', 'they', 'their', 'them', 'i',
  'me', 'my', 'mine', 'can', 'could', 'should', 'would', 'about', 'into', 'over', 'under', 'also',
  'not', 'no', 'do', 'does', 'did', 'done', 'have', 'had', 'having', 'such', 'so', 'but', 'because'
]);

function splitSentences(text) {
  if (!text || !text.trim()) {
    return [];
  }

  const normalized = text.replace(/\s+/g, ' ').trim();
  const base = normalized
    .split(/(?<=[.!?])\s+/)
    .map((sentence) => sentence.trim())
    .filter(Boolean);

  if (base.length > 1) {
    return base;
  }

  const clauses = normalized
    .split(/(?<=[,;:])\s+/)
    .map((clause) => clause.trim())
    .filter(Boolean);

  if (clauses.length > 1) {
    return clauses;
  }

  return [normalized];
}

function tokenize(text) {
  if (!text) {
    return [];
  }
  const tokens = text.toLowerCase().match(/[A-Za-z0-9']+/g);
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

  const allWords = tokenize(text).filter((word) => !STOP_WORDS.has(word) && !/^\d+$/.test(word));
  if (allWords.length === 0) {
    return sentences.slice(0, maxSentences).join(' ');
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

  const selected = scored
    .sort((a, b) => b.score - a.score)
    .slice(0, maxSentences)
    .map((entry) => entry.index)
    .sort((a, b) => a - b);

  return selected.map((index) => sentences[index]).join(' ');
}

module.exports = (req, res) => {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method Not Allowed' });
  }

  const text = String(req.body?.text || '').trim();
  const sentencesRaw = Number.parseInt(req.body?.sentences, 10);
  const sentences = Number.isInteger(sentencesRaw) && sentencesRaw > 0 ? sentencesRaw : 3;

  if (!text) {
    return res.status(400).json({ error: 'Input text is required' });
  }

  return res.status(200).json({ summary: summarizeText(text, sentences) });
};
