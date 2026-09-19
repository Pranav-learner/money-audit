# 09 — AI, ML & Intelligence Engines

## Purpose
Document the machine learning, optical character recognition (OCR), rule engines, and conversational AI assistant modules.

## Scope
`com/Pranav/finance_tracker/financialintelligence/`, `com/Pranav/finance_tracker/aiassistant/`, and `com/Pranav/finance_tracker/receipt/`.

## Current Status
- **Status**: Initialized (Pre-Level 0)
- **Deep Inspection**: Scheduled for Level 9

---

## 1. Intelligence Subsystems [OBSERVED]

### A. Receipt OCR Pipeline
- **Engine**: Tesseract OCR via Tess4J (5.11.0).
- **Function**: Extracts text from receipt images to pre-populate expense details.

### B. Financial Intelligence Rule Engine (`financialintelligence`)
- **Risk Detection Engine**: Evaluates spending spikes, debt thresholds, overdue settlements, subscription creep.
- **Financial Health Score**: Weighted scoring across budget adherence (30%), savings (30%), debt management (25%), spending stability (15%).
- **Automated Recommendation Engine**: Priority-orchestrated rules for emergency fund creation, savings targets, and debt payoff.
- **Financial Forecasting**: 6-month trailing predictive horizon for goal planning and cash flow projection.

### C. Conversational AI Assistant (`aiassistant`)
- **Architecture**: Tool-orchestrated conversational agent.
- **Tools**: 11 internal Java tools (`BudgetTool`, `ExpenseTool`, `SavingsTool`, `RiskTool`, etc.) fetching ground truth from SQL database.
- **Provider**: `TemplateLlmProvider` (deterministic, fact-grounded, zero hallucination; swappable to OpenAI/Anthropic/Gemini).

---

## 2. Placeholders for Level 9
- [ ] *Detailed execution trace of OCR image preprocessing and regex extraction.*
- [ ] *Mathematical formulas used in Health Score and Risk Engines.*
- [ ] *Prompt engineering templates and intent routing mechanisms in AI Assistant.*
- [ ] *Roadmap for potential ML fine-tuning or generative model integration.*
