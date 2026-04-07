import React, { useState, useRef, useEffect } from 'react';
import { aiChat } from '../services/api';

const AiChat = () => {
  const [open, setOpen]       = useState(false);
  const [messages, setMessages] = useState([
    { role: 'ai', text: "Hi! I'm your AI course advisor 👋 Ask me anything like:\n• \"I want to learn Python from scratch\"\n• \"What should I study after React?\"\n• \"Best free ML courses for beginners\"" }
  ]);
  const [input, setInput]     = useState('');
  const [loading, setLoading] = useState(false);
  const [courses, setCourses] = useState([]);
  const bottomRef = useRef(null);

  useEffect(() => {
    if (open && bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, open]);

  const sendMessage = async (e) => {
    e?.preventDefault();
    const text = input.trim();
    if (!text || loading) return;

    setInput('');
    setMessages(prev => [...prev, { role: 'user', text }]);
    setLoading(true);
    setCourses([]);

    try {
      const data = await aiChat(text);
      setMessages(prev => [...prev, { role: 'ai', text: data.reply || 'Here are some courses for you!' }]);
      if (data.courses && data.courses.length > 0) {
        setCourses(data.courses);
      }
    } catch (err) {
      setMessages(prev => [...prev, {
        role: 'ai',
        text: '⚠️ Backend not connected. Start the Spring Boot server to enable AI search.',
        isError: true
      }]);
    } finally {
      setLoading(false);
    }
  };

  const handleSuggestion = (s) => {
    setInput(s);
    setTimeout(() => sendMessage(), 50);
  };

  const SUGGESTIONS = [
    'Best Python course for beginners',
    'Machine learning roadmap 2024',
    'Learn Java Spring Boot',
    'Free web dev courses',
  ];

  return (
    <>
      {/* Floating button */}
      <button
        id="ai-chat-toggle"
        className="ai-fab"
        onClick={() => setOpen(o => !o)}
        aria-label="AI Course Advisor"
      >
        {open ? '✕' : '🤖'}
        {!open && <span className="ai-fab-label">AI Advisor</span>}
      </button>

      {/* Chat Panel */}
      {open && (
        <div className="ai-panel" id="ai-chat-panel">
          {/* Header */}
          <div className="ai-panel-header">
            <div className="ai-panel-title">
              <span className="ai-panel-icon">🤖</span>
              <div>
                <div className="ai-panel-name">AI Course Advisor</div>
                <div className="ai-panel-status">● Powered by Gemini</div>
              </div>
            </div>
            <button className="ai-panel-close" onClick={() => setOpen(false)}>✕</button>
          </div>

          {/* Messages */}
          <div className="ai-messages">
            {messages.map((msg, i) => (
              <div key={i} className={`ai-msg ai-msg-${msg.role} ${msg.isError ? 'ai-msg-error' : ''}`}>
                {msg.role === 'ai' && <span className="ai-msg-avatar">🤖</span>}
                <div className="ai-msg-bubble">
                  {msg.text.split('\n').map((line, j) => (
                    <span key={j}>{line}{j < msg.text.split('\n').length - 1 && <br />}</span>
                  ))}
                </div>
              </div>
            ))}

            {/* Loading */}
            {loading && (
              <div className="ai-msg ai-msg-ai">
                <span className="ai-msg-avatar">🤖</span>
                <div className="ai-msg-bubble ai-msg-thinking">
                  <span className="ai-dot" /><span className="ai-dot" /><span className="ai-dot" />
                </div>
              </div>
            )}

            {/* Course Results */}
            {courses.length > 0 && !loading && (
              <div className="ai-courses-results">
                <div className="ai-courses-label">📚 Courses found:</div>
                <div className="ai-courses-scroll">
                  {courses.map((c, i) => (
                    <div key={c.id || i} className="ai-mini-card">
                      {c.thumbnail ? (
                        <img src={c.thumbnail} alt={c.title} className="ai-mini-thumb" referrerPolicy="no-referrer" onError={e => e.target.style.display='none'} />
                      ) : (
                        <div className="ai-mini-thumb-placeholder">🎓</div>
                      )}
                      <div className="ai-mini-info">
                        <div className="ai-mini-platform">{c.platform}</div>
                        <div className="ai-mini-title">{c.title}</div>
                        {c.rating && <div className="ai-mini-rating">⭐ {c.rating}</div>}
                      </div>
                      <a href={c.url} target="_blank" rel="noopener noreferrer" className="ai-mini-btn">→</a>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Quick suggestions (only if first message) */}
            {messages.length === 1 && !loading && (
              <div className="ai-suggestions">
                {SUGGESTIONS.map(s => (
                  <button key={s} className="ai-suggestion-chip" onClick={() => handleSuggestion(s)}>
                    {s}
                  </button>
                ))}
              </div>
            )}

            <div ref={bottomRef} />
          </div>

          {/* Input */}
          <form className="ai-input-row" onSubmit={sendMessage}>
            <input
              id="ai-chat-input"
              className="ai-input"
              type="text"
              value={input}
              onChange={e => setInput(e.target.value)}
              placeholder="Ask anything about courses..."
              disabled={loading}
              autoComplete="off"
            />
            <button type="submit" className="ai-send-btn" disabled={loading || !input.trim()} id="ai-send-btn">
              {loading ? '…' : '↑'}
            </button>
          </form>
        </div>
      )}
    </>
  );
};

export default AiChat;
