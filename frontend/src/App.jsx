import { useState } from 'react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export default function App() {
  const [text, setText] = useState('');
  const [sentences, setSentences] = useState(3);
  const [output, setOutput] = useState('Your summary will appear here.');
  const [loading, setLoading] = useState(false);

  const onSummarize = async () => {
    const cleanText = text.trim();
    if (!cleanText) {
      setOutput('Please enter text first.');
      return;
    }

    setLoading(true);
    setOutput('Summarizing...');

    try {
      const response = await fetch(`${API_BASE_URL}/api/summarize`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: cleanText, sentences: Number(sentences) || 3 }),
      });

      const data = await response.json();
      if (!response.ok) {
        setOutput(data.error || 'Failed to summarize.');
        return;
      }

      setOutput(data.summary || '[No summary generated]');
    } catch {
      setOutput('Error: Unable to reach backend server.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="wrap">
      <h1>Text Summarizer</h1>
      <p className="sub">Paste your text and get a short summary instantly.</p>

      <label htmlFor="text">Input Text</label>
      <textarea
        id="text"
        placeholder="Paste long text here..."
        value={text}
        onChange={(event) => setText(event.target.value)}
      />

      <div className="controls">
        <label htmlFor="sentences">Summary Sentences</label>
        <input
          id="sentences"
          type="number"
          min="1"
          max="10"
          value={sentences}
          onChange={(event) => setSentences(event.target.value)}
        />
        <button type="button" onClick={onSummarize} disabled={loading}>
          Summarize
        </button>
      </div>

      <h2>Summary</h2>
      <pre>{output}</pre>
    </main>
  );
}
