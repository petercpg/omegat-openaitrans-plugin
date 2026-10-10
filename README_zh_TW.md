# OmegaT AI LLM 機器翻譯外掛
[English](README.md) | 正體中文 | [简体中文](README_CN.md)

本外掛串接 OpenAI、Anthropic Claude、Google Gemini 與 DeepSeek 等大型語言模型 API 進行機器翻譯，具備以下核心特色：
* 支援多種主流模型（OpenAI、Claude、Gemini、DeepSeek 等）
* **CAT 標籤保護與強調顯示**：徹底解決 OmegaT 原生機器翻譯視窗吞噬/隱藏內嵌 XML、DocBook 標籤（如 `<guibutton>`、`<placeholder .../>`、`<b0>`）的陳年問題；以語法強調清晰呈現，且 `Ctrl+M` 插入時完美保留無損的原始標籤
* 支援多組態 / 多執行個體管理（分頁獨立設定不同翻譯引擎與模型）
* 雙 Prompt 架構（System Prompt + User Prompt），在保留原生標籤防護規則的同時，自由追加口吻、風格與在地化指示
* 支援自訂 API 端點 URL（支援自建反向代理與官方端點）
* 支援調整 `temperature`（溫度）參數
* 整合專案詞彙表（Glossary），智慧注入比對相符的詞彙
* 高效能翻譯快取（Caffeine / JCache）

## 使用方法
> 使用 OpenAI、Claude 或 Gemini API 需備有對應的 `API Key`，本專案不提供 API 金鑰。

1. 從 Releases 下載編譯好的 jar 檔案，放入 OmegaT 的 plugins 目錄（預設通常在 OmegaT 安裝目錄或使用者設定檔下的 `plugins` 資料夾）。
2. （必要）啟動 OmegaT，依序開啟「選項」➔「偏好設定」➔「機器翻譯」，勾選「AI LLM Translate」，點選「設定」並輸入您的 API 金鑰。
3. （必要）在「API 端點」填入對應的 URL（若使用 Gemini，可留空或填入 `https://generativelanguage.googleapis.com`）。
4. （選填）在「System Prompt」框內填入自訂系統提示詞。若不需要可保持留空，留空時會自動使用內建的 CAT 標籤保護提示詞，並自動辨識來源語言與目標語言。
5. （選填）在「User Prompt」框內填入自訂追加指示。此處內容會附加在 System Prompt 之後（例如指定「請使用台灣正體中文慣用語與全形標點」等風格指示），既能彈性自訂語氣，又不會破壞內建的標籤保護規則。
6. （選填）調整 `temperature` 參數（預設為 0，適合精確翻譯）。
7. （選填）修改「API 格式」。部分第三方服務提供者會以 OpenAI 的相容格式提供 Claude/Gemini 模型；若不清楚此選項的意義，請保持「default」預設值即可，外掛會自動依據所選模型決定 API 格式。
8. 從下拉選單中選擇欲呼叫的模型，或勾選「使用自訂模型」以手動輸入模型名稱（適用於未收錄在清單中的最新模型或微調模型）。
9. （選填）勾選「啟用詞彙表」，啟用後系統會自動將目前區段中比對相符的專案詞彙一併傳遞給 AI 作為翻譯參考。
10. （選填）勾選「啟用快取功能」，啟用後會優先搜尋先前的翻譯紀錄以節省額度（快取會在關閉專案時清空）。
11. 最後勾選啟用，點擊確定並關閉設定視窗。

## 自訂模型支援
若需要使用下拉選單中未列出的模型，可利用「使用自訂模型」功能：
1. 在設定對話框底部勾選「使用自訂模型」核取方塊。
2. 勾選後，模型下拉選單會切換為文字輸入框。
3. 輸入 API 服務提供者要求的確切模型名稱代碼。
4. 適用情境：
   - 剛推出且尚未收錄於外掛清單的新模型
   - 自訂微調（Fine-tuned）模型
   - 採用相容 API 格式的第三方 AI 服務模型

## CAT 標籤保護與強調顯示（Tag-Aware Rendering）
在 OmegaT 中，若待譯字串包含內嵌 XML、HTML 或 DocBook 標籤（例如 `<guibutton>...</guibutton>`、`<placeholder type="inlinemediaobject" id="0"/>`、`<b0>...</b0>`），OmegaT 原生的機器翻譯窗格會因直接將字串串接至 HTML 中，導致 Swing 渲染引擎將標籤誤認為未知 HTML 而默默隱藏，使得譯者無法在預覽中看清標籤位置。

本外掛內建 **Tag-Aware 渲染引擎**（`TagAwareHTMLEditorKit`）：
* **清晰可辨的語法強調**：在機器翻譯面板中完整還原所有內嵌標籤，並套用美觀的語法強調（一般標籤為科技藍、預留位置標籤為優雅紫，自動依淺色或深色主題切換明暗對比）。
* **原生 `Ctrl+M` 插入無縫整合**：不同於粗糙的 HTML 跳脫（escape）做法，本外掛在記憶體中保留原始字串。按下 `Ctrl+M` 或右鍵插入時，寫入編輯器的依然是純文字 XML 標籤，完美通過 OmegaT 標籤驗證（Tag Validation）。
* **全域通用受惠**：載入外掛後，OmegaT 中同時啟用的其他翻譯引擎（如 Google Translate、DeepL）也能一併享受到標籤可見與強調的好處。

## 提示詞（Prompt）說明
### System Prompt 與 User Prompt
本外掛提供兩個獨立的 Prompt 輸入欄位，兼顧最大彈性與 CAT 標籤防護：
1. **System Prompt**：
   - 若留空，會自動採用內建的提示詞。
   - 若填寫，則會覆蓋內建提示詞。
2. **User Prompt**：
   - 填寫於此的內容會**附加（Append）**於 System Prompt 之後（標註在 `ADDITIONAL INSTRUCTIONS:` 區塊）。
   - 讓您能自由追加特定術語、風格、台灣正體中文慣用語等指示，而**無須擔心破壞內建的標籤保護規則與排版格式**。

兩個欄位均支援以下語言代碼預留位置（會自動代入目前專案的語言設定）：
* `%1$s` 或 `${sourceLang}`：來源語言代碼（例如 `en`）
* `%2$s` 或 `${targetLang}`：目標語言代碼（例如 `zh` 或 `zh-TW`）
* 亦相容傳統順序替換代碼 `%s`（第一個為來源語言，第二個為目標語言）。

例如在 **User Prompt** 中設定：
```text
When %2$s is zh-TW, use Traditional Chinese terms and punctuation conventions for Taiwan.
```

### 詞彙表相關 Prompt
專案啟用詞彙表功能後，若當下翻譯的字串有對應詞彙，會自動附加在翻譯 Prompt 末尾，格式為：`You can refer to the following glossary when translating: {glossary}`。若目前字串沒有相符的詞彙，則不會追加此段 Prompt。

## 授權條款（License）
本專案遵循 GPL 3.0 授權條款。

## 致謝（Credits & Acknowledgments）
* **原專案與作者**：本專案為 [Inertia (@inertia42)](https://github.com/inertia42) 的開源專案 [omegat-openaitrans-plugin](https://github.com/inertia42/omegat-openaitrans-plugin) 之 Fork 版本。衷心感謝原作者 Inertia 的開創性研發與架構設計！
* 特別感謝 [omegat-niutrans-plugin 專案](https://github.com/xflcx1991/omegat-niutrans-plugin) 與 [azure-translate-plugin 專案](https://github.com/omegat-org/azure-translate-plugin)，本專案部分程式碼參考這兩個專案的實作。
