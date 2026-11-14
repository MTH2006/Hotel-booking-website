/**
 * ==============================================================================
 * HESTORA AI HOTEL ASSISTANT - REACT CHATBOT WIDGET
 * ==============================================================================
 */

(function () {
  'use strict';

  // 1. Session & API Configuration
  const API_BASE = (window.location.protocol.startsWith('http') && window.location.port !== '8080')
    ? '/api/ai'
    : `${window.location.protocol}//${window.location.hostname || 'localhost'}:8080/api/ai`;

  function getOrCreateSessionId() {
    let sid = localStorage.getItem('hestora_ai_session_id');
    if (!sid) {
      sid = 'session_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
      localStorage.setItem('hestora_ai_session_id', sid);
    }
    return sid;
  }

  function getAuthToken() {
    try {
      const u = JSON.parse(localStorage.getItem('hotel_user') || 'null');
      return u && u.token ? u.token : null;
    } catch {
      return null;
    }
  }

  function getCurrentUser() {
    try {
      return JSON.parse(localStorage.getItem('hotel_user') || 'null');
    } catch {
      return null;
    }
  }

  // 2. React Chatbot Component Implementation
  function HotelAIChatbot() {
    const React = window.React;
    const useState = React.useState;
    const useEffect = React.useEffect;
    const useRef = React.useRef;

    const [isOpen, setIsOpen] = useState(false);
    const [sessionId, setSessionId] = useState(getOrCreateSessionId);
    const [messages, setMessages] = useState([
      {
        id: 'welcome_0',
        sender: 'assistant',
        text: 'Xin chào quý khách! Em là **Trợ lý AI HESTORA**. Em có thể hỗ trợ quý khách tìm kiếm phòng trống, xem giá niêm yết, giải đáp chính sách nhận/trả phòng, hủy phòng hoặc tra cứu đơn đặt phòng của quý khách.\n\nQuý khách cần em hỗ trợ điều gì hôm nay ạ?',
        citations: [],
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      }
    ]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [suggestions, setSuggestions] = useState([
      'Tìm phòng cho 2 người tại Đà Nẵng',
      'Giờ nhận và trả phòng là mấy giờ?',
      'Chính sách hủy phòng thế nào?',
      'Khách sạn có buffet sáng miễn phí không?',
      'Tôi muốn kiểm tra đơn đặt phòng của mình'
    ]);
    const [showHandoffModal, setShowHandoffModal] = useState(false);
    const [handoffForm, setHandoffForm] = useState({ name: '', email: '', phone: '', note: '' });
    const [handoffSuccess, setHandoffSuccess] = useState(false);

    const messagesEndRef = useRef(null);
    const chatWindowRef = useRef(null);

    useEffect(() => {
      const handleOpen = () => setIsOpen(true);
      const handleClose = () => setIsOpen(false);
      const handleToggle = () => setIsOpen(prev => !prev);
      window.addEventListener('open-hestora-ai', handleOpen);
      window.addEventListener('close-hestora-ai', handleClose);
      window.addEventListener('toggle-hestora-ai', handleToggle);
      window.openHestoraAIChatbot = handleOpen;
      window.closeHestoraAIChatbot = handleClose;
      window.toggleHestoraAIChatbot = handleToggle;
      return () => {
        window.removeEventListener('open-hestora-ai', handleOpen);
        window.removeEventListener('close-hestora-ai', handleClose);
        window.removeEventListener('toggle-hestora-ai', handleToggle);
      };
    }, []);

    // Click outside to close and ESC key listener
    useEffect(() => {
      if (!isOpen) return;

      const handleClickOutside = (e) => {
        // If clicked inside the chatbot window or its modals, do not close
        if (chatWindowRef.current && chatWindowRef.current.contains(e.target)) {
          return;
        }
        // If clicked on opening triggers, let their handler manage it
        if (e.target.closest('#navSupport') || e.target.closest('.hestora-ai-trigger') || e.target.closest('#featureSupportItem')) {
          return;
        }
        setIsOpen(false);
      };

      const handleKeyDown = (e) => {
        if (e.key === 'Escape' || e.keyCode === 27) {
          setIsOpen(false);
        }
      };

      const timer = setTimeout(() => {
        document.addEventListener('click', handleClickOutside);
        document.addEventListener('keydown', handleKeyDown);
      }, 0);

      return () => {
        clearTimeout(timer);
        document.removeEventListener('click', handleClickOutside);
        document.removeEventListener('keydown', handleKeyDown);
      };
    }, [isOpen]);

    const scrollToBottom = () => {
      if (messagesEndRef.current) {
        messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
      }
    };

    useEffect(() => {
      scrollToBottom();
    }, [messages, loading, isOpen]);

    // Load initial suggestions from server if available
    useEffect(() => {
      fetch(`${API_BASE}/suggestions`)
        .then(res => res.json())
        .then(data => {
          if (Array.isArray(data) && data.length > 0) {
            setSuggestions(data);
          }
        })
        .catch(() => {});
    }, []);

    // Send Message Handler
    const handleSend = async (messageText) => {
      const text = (messageText || input).trim();
      if (!text || loading) return;

      const userMsg = {
        id: 'msg_' + Date.now(),
        sender: 'user',
        text: text,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };

      setMessages(prev => [...prev, userMsg]);
      setInput('');
      setLoading(true);

      const token = getAuthToken();
      const headers = {
        'Content-Type': 'application/json; charset=utf-8'
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      try {
        const response = await fetch(`${API_BASE}/chat`, {
          method: 'POST',
          headers: headers,
          body: JSON.stringify({
            sessionId: sessionId,
            message: text
          })
        });

        if (!response.ok) {
          throw new Error('Máy chủ phản hồi lỗi: ' + response.status);
        }

        const data = await response.json();

        const aiMsg = {
          id: 'msg_ai_' + Date.now(),
          sender: 'assistant',
          text: data.reply || 'Dạ, em đã nhận được thông tin.',
          citations: data.citations || [],
          toolResult: data.toolResult,
          handoffRecommended: data.handoffRecommended,
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        };

        setMessages(prev => [...prev, aiMsg]);
        if (data.suggestions && data.suggestions.length > 0) {
          setSuggestions(data.suggestions);
        }
      } catch (err) {
        console.error('Chat error:', err);
        setMessages(prev => [
          ...prev,
          {
            id: 'err_' + Date.now(),
            sender: 'assistant',
            text: 'Dạ, hiện tại kết nối đến máy chủ AI đang bị gián đoạn. Quý khách vui lòng thử lại sau ít giây hoặc liên hệ trực tiếp hotline **1900 8888** để được hỗ trợ tức thì ạ.',
            citations: [],
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
          }
        ]);
      } finally {
        setLoading(false);
      }
    };

    const handleKeyDown = (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSend();
      }
    };

    const handleReset = () => {
      const newSid = 'session_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
      localStorage.setItem('hestora_ai_session_id', newSid);
      setSessionId(newSid);
      setMessages([
        {
          id: 'welcome_' + Date.now(),
          sender: 'assistant',
          text: 'Dạ, em đã tạo một cuộc hội thoại mới. Em có thể hỗ trợ gì cho kỳ nghỉ của quý khách tại HESTORA ạ?',
          citations: [],
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
    };

    const handleHandoffSubmit = async (e) => {
      e.preventDefault();
      try {
        await fetch(`${API_BASE}/handoff`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json; charset=utf-8' },
          body: JSON.stringify({
            sessionId: sessionId,
            customerName: handoffForm.name,
            customerEmail: handoffForm.email,
            customerPhone: handoffForm.phone,
            questionSummary: handoffForm.note || 'Khách yêu cầu hỗ trợ trực tiếp'
          })
        });
        setHandoffSuccess(true);
        setTimeout(() => {
          setShowHandoffModal(false);
          setHandoffSuccess(false);
          setMessages(prev => [
            ...prev,
            {
              id: 'handoff_ok_' + Date.now(),
              sender: 'assistant',
              text: 'Dạ, thông tin yêu cầu của quý khách đã được chuyển tới bộ phận Chăm sóc khách hàng. Nhân viên lễ tân sẽ liên hệ trong ít phút ạ!',
              citations: [],
              timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
            }
          ]);
        }, 1500);
      } catch (err) {
        alert('Không thể gửi yêu cầu hỗ trợ lúc này. Vui lòng thử lại.');
      }
    };

    // Render formatted markdown helper
    const formatText = (text) => {
      if (!text) return '';
      // Simple and safe bold / bullet points formatting
      const paragraphs = text.split('\n\n');
      return paragraphs.map((p, idx) => {
        let formatted = p.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        formatted = formatted.replace(/\*(.*?)\*/g, '<em>$1</em>');
        formatted = formatted.replace(/\n• /g, '<br>• ');
        formatted = formatted.replace(/\n(\d+)\.\s/g, '<br>$1. ');
        return React.createElement('p', {
          key: idx,
          dangerouslySetInnerHTML: { __html: formatted }
        });
      });
    };

    return React.createElement(
      React.Fragment,
      null,
      // 1. Floating Trigger Button
      React.createElement(
        'button',
        {
          className: 'hestora-ai-trigger' + (isOpen ? ' open' : ''),
          onClick: () => setIsOpen(!isOpen),
          'aria-label': 'Trợ lý AI HESTORA'
        },
        React.createElement('div', { className: 'trigger-badge' }),
        React.createElement('div', { className: 'hestora-ai-tooltip' }, 'Trò chuyện cùng AI HESTORA'),
        isOpen
          ? React.createElement(
              'svg',
              { className: 'trigger-icon', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: '2.5', strokeLinecap: 'round', strokeLinejoin: 'round' },
              React.createElement('line', { x1: '18', y1: '6', x2: '6', y2: '18' }),
              React.createElement('line', { x1: '6', y1: '6', x2: '18', y2: '18' })
            )
          : React.createElement(
              'svg',
              { className: 'trigger-icon', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: '2', strokeLinecap: 'round', strokeLinejoin: 'round' },
              React.createElement('path', { d: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z' }),
              React.createElement('circle', { cx: '9', cy: '10', r: '1', fill: 'currentColor' }),
              React.createElement('circle', { cx: '15', cy: '10', r: '1', fill: 'currentColor' })
            )
      ),

      // 2. Chat Window Modal
      React.createElement(
        'div',
        { ref: chatWindowRef, className: 'hestora-ai-window' + (isOpen ? ' open' : '') },

        // Header
        React.createElement(
          'div',
          { className: 'ai-header' },
          React.createElement(
            'div',
            { className: 'ai-header-left' },
            React.createElement('div', { className: 'ai-avatar' }, 'AI'),
            React.createElement(
              'div',
              { className: 'ai-title-block' },
              React.createElement('h3', null, 'Trợ lý HESTORA AI'),
              React.createElement(
                'p',
                null,
                React.createElement('span', { className: 'ai-status-dot' }),
                'Trực tuyến 24/7 • RAG Engine'
              )
            )
          ),
          React.createElement(
            'div',
            { className: 'ai-header-actions' },
            React.createElement(
              'button',
              { className: 'ai-header-btn', title: 'Cuộc trò chuyện mới', onClick: handleReset },
              React.createElement(
                'svg',
                { width: '16', height: '16', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: '2', strokeLinecap: 'round', strokeLinejoin: 'round' },
                React.createElement('polyline', { points: '1 4 1 10 7 10' }),
                React.createElement('path', { d: 'M3.51 15a9 9 0 1 0 2.13-9.36L1 10' })
              )
            ),
            React.createElement(
              'button',
              { className: 'ai-header-btn', title: 'Thu nhỏ', onClick: () => setIsOpen(false) },
              React.createElement(
                'svg',
                { width: '16', height: '16', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: '2.5', strokeLinecap: 'round', strokeLinejoin: 'round' },
                React.createElement('line', { x1: '18', y1: '6', x2: '6', y2: '18' }),
                React.createElement('line', { x1: '6', y1: '6', x2: '18', y2: '18' })
              )
            )
          )
        ),

        // Messages Body
        React.createElement(
          'div',
          { className: 'ai-messages-body' },
          messages.map(msg =>
            React.createElement(
              'div',
              { key: msg.id, className: 'ai-message ' + msg.sender },
              React.createElement(
                'div',
                { className: 'ai-bubble' },
                formatText(msg.text),
                // Citations
                msg.citations && msg.citations.length > 0
                  ? React.createElement(
                      'div',
                      { className: 'ai-citations' },
                      msg.citations.map((c, i) =>
                        React.createElement('span', { key: i, className: 'ai-citation-pill' }, '📚 ' + c)
                      )
                    )
                  : null,
                // Handoff CTA
                msg.handoffRecommended
                  ? React.createElement(
                      'div',
                      { style: { marginTop: '10px' } },
                      React.createElement(
                        'button',
                        {
                          className: 'ai-suggestion-chip',
                          style: { background: '#ea580c', color: '#fff', borderColor: '#ea580c' },
                          onClick: () => setShowHandoffModal(true)
                        },
                        '📞 Yêu cầu nhân viên lễ tân gọi lại'
                      )
                    )
                  : null
              )
            )
          ),
          // Loading typing indicator
          loading
            ? React.createElement(
                'div',
                { className: 'ai-typing' },
                React.createElement('div', { className: 'ai-dot' }),
                React.createElement('div', { className: 'ai-dot' }),
                React.createElement('div', { className: 'ai-dot' })
              )
            : null,
          React.createElement('div', { ref: messagesEndRef })
        ),

        // Quick Suggestion Chips
        suggestions && suggestions.length > 0
          ? React.createElement(
              'div',
              { className: 'ai-suggestions-container' },
              suggestions.map((s, idx) =>
                React.createElement(
                  'button',
                  {
                    key: idx,
                    className: 'ai-suggestion-chip',
                    onClick: () => handleSend(s)
                  },
                  s
                )
              )
            )
          : null,

        // Input Area
        React.createElement(
          'div',
          { className: 'ai-input-area' },
          React.createElement('input', {
            type: 'text',
            className: 'ai-input',
            placeholder: 'Hỏi về phòng, giá, chính sách...',
            value: input,
            onChange: e => setInput(e.target.value),
            onKeyDown: handleKeyDown,
            disabled: loading
          }),
          React.createElement(
            'button',
            {
              className: 'ai-send-btn',
              onClick: () => handleSend(),
              disabled: loading || !input.trim(),
              title: 'Gửi'
            },
            React.createElement(
              'svg',
              { width: '18', height: '18', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: '2', strokeLinecap: 'round', strokeLinejoin: 'round' },
              React.createElement('line', { x1: '22', y1: '2', x2: '11', y2: '13' }),
              React.createElement('polygon', { points: '22 2 15 22 11 13 2 9 22 2' })
            )
          )
        ),

        // Handoff Modal
        showHandoffModal
          ? React.createElement(
              'div',
              { className: 'ai-handoff-modal' },
              React.createElement(
                'div',
                { className: 'ai-handoff-card' },
                handoffSuccess
                  ? React.createElement('div', { style: { textAlign: 'center', padding: '20px 0', color: '#166534' } }, '✓ Đã chuyển thông tin đến lễ tân thành công!')
                  : React.createElement(
                      'form',
                      { onSubmit: handleHandoffSubmit },
                      React.createElement('h4', null, 'Yêu cầu nhân viên hỗ trợ'),
                      React.createElement('p', null, 'Vui lòng để lại thông tin để nhân viên lễ tân liên hệ với quý khách trong ít phút.'),
                      React.createElement(
                        'div',
                        { className: 'ai-handoff-field' },
                        React.createElement('label', null, 'Họ và tên'),
                        React.createElement('input', {
                          type: 'text',
                          required: true,
                          value: handoffForm.name,
                          placeholder: 'Nguyễn Văn A',
                          onChange: e => setHandoffForm({ ...handoffForm, name: e.target.value })
                        })
                      ),
                      React.createElement(
                        'div',
                        { className: 'ai-handoff-field' },
                        React.createElement('label', null, 'Số điện thoại'),
                        React.createElement('input', {
                          type: 'tel',
                          required: true,
                          value: handoffForm.phone,
                          placeholder: '0901234567',
                          onChange: e => setHandoffForm({ ...handoffForm, phone: e.target.value })
                        })
                      ),
                      React.createElement(
                        'div',
                        { className: 'ai-handoff-field' },
                        React.createElement('label', null, 'Email liên hệ'),
                        React.createElement('input', {
                          type: 'email',
                          value: handoffForm.email,
                          placeholder: 'email@domain.com',
                          onChange: e => setHandoffForm({ ...handoffForm, email: e.target.value })
                        })
                      ),
                      React.createElement(
                        'div',
                        { className: 'ai-handoff-actions' },
                        React.createElement(
                          'button',
                          {
                            type: 'button',
                            className: 'ai-btn-secondary',
                            onClick: () => setShowHandoffModal(false)
                          },
                          'Hủy'
                        ),
                        React.createElement(
                          'button',
                          { type: 'submit', className: 'ai-btn-primary' },
                          'Gửi yêu cầu'
                        )
                      )
                    )
              )
            )
          : null
      )
    );
  }

  // 3. Mount React Component into Webpage
  function initChatbot() {
    let rootEl = document.getElementById('hestora-ai-chatbot-root');
    if (!rootEl) {
      rootEl = document.createElement('div');
      rootEl.id = 'hestora-ai-chatbot-root';
      document.body.appendChild(rootEl);
    }

    if (window.ReactDOM && window.ReactDOM.createRoot && window.React) {
      const root = window.ReactDOM.createRoot(rootEl);
      root.render(window.React.createElement(HotelAIChatbot));
    } else if (window.ReactDOM && window.ReactDOM.render && window.React) {
      window.ReactDOM.render(window.React.createElement(HotelAIChatbot), rootEl);
    } else {
      console.warn('React or ReactDOM not found on window. Will retry after delay.');
      setTimeout(initChatbot, 300);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initChatbot);
  } else {
    initChatbot();
  }
})();

