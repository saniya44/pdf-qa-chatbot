import { useState, useCallback, useRef, useEffect } from "react";

const API = "http://localhost:8080/api";

// ── Icons ────────────────────────────────────────────────────
const UploadIcon = () => (
  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
    <polyline points="17 8 12 3 7 8"/>
    <line x1="12" y1="3" x2="12" y2="15"/>
  </svg>
);

const SendIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <line x1="22" y1="2" x2="11" y2="13"/>
    <polygon points="22 2 15 22 11 13 2 9 22 2"/>
  </svg>
);

const PdfIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
    <polyline points="14 2 14 8 20 8"/>
    <line x1="16" y1="13" x2="8" y2="13"/>
    <line x1="16" y1="17" x2="8" y2="17"/>
    <polyline points="10 9 9 9 8 9"/>
  </svg>
);

// ── Main App ─────────────────────────────────────────────────
export default function App() {
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [messages, setMessages] = useState([{
    role: "assistant",
    content: "👋 Hello! Upload a PDF above, then ask me anything about it.",
    timestamp: new Date()
  }]);
  const [question, setQuestion] = useState("");
  const [uploading, setUploading] = useState(false);
  const [asking, setAsking] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const messagesEndRef = useRef(null);
  const fileInputRef = useRef(null);

  // Auto-scroll to latest message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // ── Upload Handler ─────────────────────────────────────────
  const handleUpload = useCallback(async (file) => {
    if (!file || file.type !== "application/pdf") {
      alert("Please upload a valid PDF file.");
      return;
    }

    setUploading(true);
    const formData = new FormData();
    formData.append("file", file);

    try {
      const res = await fetch(`${API}/pdf/upload`, {
        method: "POST",
        body: formData
      });
      const data = await res.json();

      if (data.success) {
        setUploadedFiles(prev => [...prev, {
          name: data.fileName,
          chunks: data.totalChunks,
          uploadedAt: new Date()
        }]);
        setMessages(prev => [...prev, {
          role: "assistant",
          content: `✅ **${data.fileName}** has been indexed! (${data.totalChunks} chunks)\n\nYou can now ask me questions about this document.`,
          timestamp: new Date()
        }]);
      } else {
        alert("Upload failed: " + data.message);
      }
    } catch (e) {
      alert("Error: " + e.message);
    } finally {
      setUploading(false);
    }
  }, []);

  const onDrop = useCallback((e) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleUpload(file);
  }, [handleUpload]);

  // ── Ask Handler ────────────────────────────────────────────
  const handleAsk = async () => {
    if (!question.trim() || asking) return;

    const q = question.trim();
    setQuestion("");
    setMessages(prev => [...prev,
      { role: "user", content: q, timestamp: new Date() }
    ]);
    setAsking(true);

    try {
      const res = await fetch(`${API}/chat/ask`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question: q })
      });
      const data = await res.json();

      setMessages(prev => [...prev, {
        role: "assistant",
        content: data.answer || data.error,
        timestamp: new Date(),
        processingTimeMs: data.processingTimeMs,
        sourceCount: data.sourceChunks?.length
      }]);
    } catch (e) {
      setMessages(prev => [...prev, {
        role: "assistant",
        content: "❌ Error: " + e.message,
        timestamp: new Date()
      }]);
    } finally {
      setAsking(false);
    }
  };

  // ── Render ─────────────────────────────────────────────────
  return (
    <div style={{
      display: "flex", height: "100vh", fontFamily: "'Inter', system-ui, sans-serif",
      background: "#0f0f13", color: "#e8e8f0"
    }}>

      {/* ── Sidebar ── */}
      <div style={{
        width: 280, background: "#16161e", borderRight: "1px solid #2a2a38",
        display: "flex", flexDirection: "column", padding: 20, gap: 16
      }}>
        <div>
          <h1 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "#a78bfa" }}>
            📄 PDF Chatbot
          </h1>
          <p style={{ margin: "4px 0 0", fontSize: 12, color: "#666" }}>
            Spring Boot + LangChain4j + RAG
          </p>
        </div>

        {/* Drop Zone */}
        <div
          onClick={() => fileInputRef.current?.click()}
          onDrop={onDrop}
          onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          style={{
            border: `2px dashed ${dragOver ? "#a78bfa" : "#2a2a38"}`,
            borderRadius: 12, padding: "24px 16px", textAlign: "center",
            cursor: uploading ? "not-allowed" : "pointer",
            background: dragOver ? "#1e1b30" : "transparent",
            transition: "all 0.2s", color: dragOver ? "#a78bfa" : "#555"
          }}
        >
          {uploading ? (
            <div>
              <div style={{ fontSize: 24 }}>⏳</div>
              <div style={{ fontSize: 13, marginTop: 8 }}>Processing PDF...</div>
            </div>
          ) : (
            <>
              <UploadIcon />
              <div style={{ fontSize: 13, marginTop: 8, fontWeight: 500 }}>
                Drop PDF here
              </div>
              <div style={{ fontSize: 11, marginTop: 4, color: "#444" }}>
                or click to browse
              </div>
            </>
          )}
        </div>

        <input
          ref={fileInputRef}
          type="file"
          accept="application/pdf"
          style={{ display: "none" }}
          onChange={(e) => handleUpload(e.target.files[0])}
        />

        {/* Uploaded Files */}
        {uploadedFiles.length > 0 && (
          <div>
            <div style={{ fontSize: 11, color: "#555", fontWeight: 600,
              textTransform: "uppercase", letterSpacing: 1, marginBottom: 8 }}>
              Indexed PDFs ({uploadedFiles.length})
            </div>
            {uploadedFiles.map((f, i) => (
              <div key={i} style={{
                display: "flex", alignItems: "flex-start", gap: 8,
                padding: "8px 10px", borderRadius: 8, background: "#1e1b30",
                marginBottom: 6
              }}>
                <span style={{ color: "#a78bfa", marginTop: 1 }}><PdfIcon /></span>
                <div>
                  <div style={{ fontSize: 12, fontWeight: 500, color: "#d4d0e8",
                    wordBreak: "break-all" }}>{f.name}</div>
                  <div style={{ fontSize: 11, color: "#555", marginTop: 2 }}>
                    {f.chunks} chunks indexed
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Tech Stack info */}
        <div style={{ marginTop: "auto", fontSize: 11, color: "#333", lineHeight: 1.8 }}>
          <div>🔧 Spring Boot 3.2</div>
          <div>🦜 LangChain4j 0.35</div>
          <div>🧠 RAG Architecture</div>
          <div>💾 ChromaDB / Memory</div>
          <div>🤖 OpenAI / Ollama</div>
        </div>
      </div>

      {/* ── Chat Panel ── */}
      <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>

        {/* Messages */}
        <div style={{ flex: 1, overflowY: "auto", padding: "24px 32px", display: "flex",
          flexDirection: "column", gap: 16 }}>
          {messages.map((msg, i) => (
            <div key={i} style={{
              display: "flex",
              justifyContent: msg.role === "user" ? "flex-end" : "flex-start"
            }}>
              <div style={{
                maxWidth: "75%",
                background: msg.role === "user"
                  ? "linear-gradient(135deg, #7c3aed, #a78bfa)"
                  : "#1e1e2a",
                border: msg.role === "user" ? "none" : "1px solid #2a2a38",
                borderRadius: msg.role === "user"
                  ? "18px 18px 4px 18px"
                  : "18px 18px 18px 4px",
                padding: "12px 16px",
              }}>
                <div style={{ fontSize: 14, lineHeight: 1.6, whiteSpace: "pre-wrap" }}>
                  {msg.content}
                </div>
                {msg.processingTimeMs && (
                  <div style={{ fontSize: 10, color: "#555", marginTop: 6 }}>
                    ⚡ {msg.processingTimeMs}ms · {msg.sourceCount} source chunks
                  </div>
                )}
              </div>
            </div>
          ))}

          {asking && (
            <div style={{ display: "flex", justifyContent: "flex-start" }}>
              <div style={{
                background: "#1e1e2a", border: "1px solid #2a2a38",
                borderRadius: "18px 18px 18px 4px", padding: "12px 16px"
              }}>
                <span style={{ fontSize: 20 }}>⏳</span>
                <span style={{ fontSize: 13, color: "#666", marginLeft: 8 }}>
                  Searching PDF and generating answer...
                </span>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Input Bar */}
        <div style={{
          padding: "16px 32px", borderTop: "1px solid #1e1e2a",
          display: "flex", gap: 12, background: "#16161e"
        }}>
          <input
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && !e.shiftKey && handleAsk()}
            placeholder={uploadedFiles.length > 0
              ? "Ask a question about your PDFs..."
              : "Upload a PDF first, then ask questions..."}
            style={{
              flex: 1, background: "#1e1e2a", border: "1px solid #2a2a38",
              borderRadius: 12, padding: "12px 16px", color: "#e8e8f0",
              fontSize: 14, outline: "none",
              opacity: uploadedFiles.length === 0 ? 0.5 : 1
            }}
            disabled={uploading || asking || uploadedFiles.length === 0}
          />
          <button
            onClick={handleAsk}
            disabled={!question.trim() || asking || uploading || uploadedFiles.length === 0}
            style={{
              background: "linear-gradient(135deg, #7c3aed, #a78bfa)",
              border: "none", borderRadius: 12, padding: "12px 20px",
              cursor: "pointer", color: "white", display: "flex",
              alignItems: "center", gap: 6, fontSize: 14, fontWeight: 600,
              opacity: (!question.trim() || asking || uploadedFiles.length === 0) ? 0.5 : 1,
              transition: "opacity 0.2s"
            }}
          >
            <SendIcon />
            Ask
          </button>
        </div>
      </div>
    </div>
  );
}
