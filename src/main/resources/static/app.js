const summarizeBtn = document.getElementById('summarizeBtn');
const textInput = document.getElementById('text');
const sentenceInput = document.getElementById('sentences');
const output = document.getElementById('output');

summarizeBtn.addEventListener('click', async () => {
  const text = textInput.value.trim();
  const sentences = sentenceInput.value;

  if (!text) {
    output.textContent = 'Please enter text first.';
    return;
  }

  summarizeBtn.disabled = true;
  output.textContent = 'Summarizing...';

  try {
    const body = new URLSearchParams({ text, sentences });
    const response = await fetch('/api/summarize', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body,
    });

    const data = await response.json();
    if (!response.ok) {
      output.textContent = data.error || 'Failed to summarize.';
      return;
    }

    output.textContent = data.summary || '[No summary generated]';
  } catch (error) {
    output.textContent = 'Error: Unable to reach server.';
  } finally {
    summarizeBtn.disabled = false;
  }
});
