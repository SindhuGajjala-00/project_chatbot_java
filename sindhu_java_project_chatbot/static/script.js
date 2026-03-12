// script.js
const chatEl = document.getElementById('chat');
const qEl = document.getElementById('q');
const sendBtn = document.getElementById('send');

function addBubble(text, who) {
  const el = document.createElement('div');
  el.className = 'bubble ' + (who === 'user' ? 'user' : 'bot');
  el.innerText = text;
  chatEl.appendChild(el);
  chatEl.scrollTop = chatEl.scrollHeight;
}

function addTyping() {
  const el = document.createElement('div');
  el.className = 'bubble bot typing';
  el.id = 'typing';
  el.innerText = 'Thinking...';
  chatEl.appendChild(el);
  chatEl.scrollTop = chatEl.scrollHeight;
}

function removeTyping() {
  const t = document.getElementById('typing');
  if (t) t.remove();
}

async function send() {
  const text = qEl.value.trim();
  if (!text) return;
  addBubble(text, 'user');
  qEl.value = '';
  addTyping();

  try {
    const resp = await fetch('/chat', { method: 'POST', body: text });
    const json = await resp.json();
    removeTyping();
    if (json && json.answer) {
      addBubble(json.answer, 'bot');
      // small session info
      if (json.topic) {
        const meta = document.createElement('div');
        meta.className = 'meta';
        meta.innerText = 'Topic: ' + json.topic;
        chatEl.appendChild(meta);
      }
    } else {
      addBubble('Sorry, I could not get an answer.', 'bot');
    }
  } catch (e) {
    removeTyping();
    addBubble('Network error: ' + e.message, 'bot');
  }
}

sendBtn.addEventListener('click', send);
qEl.addEventListener('keydown', (e) => { if (e.key === 'Enter') send(); });
