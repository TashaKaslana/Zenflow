# Agent Overview

This document provides an overview of the agent’s behavior, context usage, and documentation generation policy.

---

## 🧩 Context and External Libraries

- The agent should always uses **`context7`** mcp for accessing/working with **external libraries** and retrieving **the latest information** when required.  
- `context7` ensures that the agent stays current with external data sources instead of relying solely on cached or internal data.

---

## ⚙️ Testing and Job Execution

- After each job is completed, the agent **automatically runs tests** to verify the correctness and stability of the outcome.  
- This continuous testing approach helps maintain reliability and consistency in all executions.

---

## 📝 Documentation Policy

- The agent **only writes documentation** to **inform or provide an overview** of a job once it is done.  
- It **does not automatically generate documentation** after each job unless the **user explicitly requests it**.  
- This keeps unnecessary output to a minimum while still allowing transparency and traceability when desired.

---

## ✅ Summary

| Aspect | Description |
|--------|--------------|
| External Data | Uses `context7` for latest info |
| Post-Job Actions | Automatically runs tests after completion |
| Documentation | Only generated when explicitly requested by user |