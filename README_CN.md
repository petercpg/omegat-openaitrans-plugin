# OmegaT AI LLM 翻译插件
[English](README.md) | [正體中文](README_zh_TW.md) | 简体中文

调用 OpenAI、Claude 及 Google Gemini API 进行翻译，有如下功能
* 支持多种模型（OpenAI、Claude、Gemini、DeepSeek 等）。
* **CAT 标签保护与高亮显示**：彻底解决 OmegaT 原生机器翻译窗口吞噬/隐藏内嵌 XML、DocBook 标签（如 `<guibutton>`、`<placeholder .../>`）的顽疾，以自适应语法高亮清晰呈现，且 `Ctrl+M` 插入时完美保持原生纯净标签。
* 支持多配置 / 多实例管理（分页面板独立配置不同引擎与模型）。
* 双 Prompt 设计（System Prompt + User Prompt），在保留原生标签防护的同时自由追加风格与本地化指示。
* 支持修改 API URL（支持自建反向代理与官方端点）。
* 支持修改 `temperature` 参数。
* 支持传入术语表。
* 支持高速缓存（Caffeine / JCache）。

## 使用方法
> 使用 OpenAI、Claude 或 Gemini API 需要拥有相应的 `API KEY`，本项目不提供 `API KEY`。
1. 下载 releases 里的 jar 文件，将其放进 OmegaT 插件目录。这个目录默认应该在 OmegaT 安装目录下的 plugins 文件夹。
2. （必须）打开 OmegaT，选项——首选项——机器翻译，选中 AI LLM Translate，点击配置，然后填入 apikey
3. （必须）在 `URL` 处填入对应的 URL（若使用 Gemini，可留空或填入 `https://generativelanguage.googleapis.com`）。
4. （可选）在「System Prompt」框内填入自定义系统提示词。若不需要可保持留空，留空时会自动使用内置的 CAT 标签保护提示词，并自动识别源语言与目标语言。
5. （可选）在「User Prompt」框内填入自定义追加指令。此处的指令会附加在 System Prompt 之后（例如指定术语风格、本地化习惯等），既能自定义语气，又不会破坏内置的标签保护规则。
6. （可选）修改 `temperature`，默认设置为 0。
7. （可选）修改 `API format`，有些服务提供商会使用 OpenAI 的 API 格式为 Claude/Gemini 模型提供服务，如果不理解这个选项的含义，请不要修改默认设置，此时会自动根据选择的模型决定 API 格式，默认设置为 default。
8. 从下拉列表中选择需要调用的模型，或勾选 `使用自定义模型` 复选框来手动输入自定义模型名称。这允许您使用下拉列表中未列出的模型。
9. （可选）启用术语表功能，启用后会将术语表窗口中显示的与该段待翻译文本匹配的术语表一并发送给 API。
10. （可选）启用缓存功能，启用后会优先搜索之前翻译结果的缓存。缓存在关闭项目后清空。
11. 最后勾选启用，点确定并关闭首选项窗口。

## 自定义模型支持
如果您需要使用下拉列表中没有的模型，可以使用 `使用自定义模型` 功能：
1. 在配置对话框底部勾选 `使用自定义模型` 复选框
2. 勾选后，模型下拉列表将被替换为文本输入框
3. 输入 API 提供商要求的确切模型名称
4. 此功能适用于：
   - 尚未添加到列表中的新发布模型
   - 自定义微调模型
   - 来自使用兼容格式的其他 API 提供商的模型

## CAT 标签保护与高亮显示（Tag-Aware Rendering）
在 OmegaT 中，若待译句包含内嵌 XML、HTML 或 DocBook 标签（例如 `<guibutton>...</guibutton>`、`<placeholder type="inlinemediaobject" id="0"/>`、`<b0>...</b0>`），OmegaT 原生的机器翻译窗格会因直接将字串拼接至 HTML 中，导致 Swing 渲染器将标签视为未知 HTML 而默默隐藏，造成译者无法在窗格中看到标签位置。

本插件独家内建 **Tag-Aware 渲染引擎**（`TagAwareHTMLEditorKit`）：
* **清晰可辨的语法高亮**：在机器翻译视窗中完整显示内嵌标签，并提供清晰的自适应语法高亮（普通标签科技蓝、占位符标签优雅紫，自动适配深色与浅色主题）。
* **原生 `Ctrl+M` 插入无缝整合**：在内存中完整保留原始未转义字串，按 `Ctrl+M` 或右击插入时，注入编辑器的依然是 100% 原始纯净的 XML 标签，完美通过 OmegaT 标签验证器（Tag Validation）。
* **全局通用**：挂载后，OmegaT 中同时启用的其他机器翻译引擎（如 Google Translate、DeepL）也能一并享受到标签清晰高亮的效果。

## 关于 Prompt 的说明
### System Prompt 与 User Prompt
插件提供两个独立的 Prompt 输入框，兼顾最大靈活性與 CAT 標籤安全性：
1. **System Prompt（系統提示詞）**：
   - 若留空，會自動使用內建的 CAT 標籤保護預設提示詞。
   - 若填寫，則會取代預設提示詞。
2. **User Prompt（使用者附加指示）**：
   - 在此填寫的內容會**附加（Append）**在 System Prompt 之後（標註於 `ADDITIONAL INSTRUCTIONS:`）。
   - 讓您可以自由新增特定口吻、風格、台灣用語習慣等指示，而完全**不需要擔心破壞內建的 XML/HTML 標籤保護與分段規則**。

兩者皆支援以下語言佔位符（會自動代入當前專案的語言代碼）：
* `%1$s` 或 `${sourceLang}`：來源語言代碼（例如 `en`）
* `%2$s` 或 `${targetLang}`：目標語言代碼（例如 `zh` 或 `zh-TW`）
* 亦支援傳統順序佔位符 `%s`（第一個為來源語言，第二個為目標語言）。

例如在 **User Prompt** 中設定：
```text
When %2$s is zh-TW, use Traditional Chinese terms and punctuation conventions for Taiwan.
```

### 术语表相关的 prompt
术语表部分直接附在翻译 prompt 后，格式为 `You can refer to the following glossary when translating: {glossary}`。若该段对应术语表为空，则不会附加相关 prompt。

## 许可证
本项目采用 GPL 3.0 许可证。

## 致谢（Credits & Acknowledgments）
* **原项目与作者**：本项目 fork 自 [Inertia (@inertia42)](https://github.com/inertia42) 的开源项目 [omegat-openaitrans-plugin](https://github.com/inertia42/omegat-openaitrans-plugin)。衷心感谢原作者 Inertia 的初始开发与架构设计！
* 特别感谢 [omegat-niutrans-plugin 项目](https://github.com/xflcx1991/omegat-niutrans-plugin) 与 [azure-translate-plugin 项目](https://github.com/omegat-org/azure-translate-plugin)，本项目部分基础代码参考与借鉴自这两个项目。