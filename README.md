# AI LLM Translate Plugin for OmegaT
English | [正體中文](README_zh_TW.md) | [简体中文](README_CN.md)

A plugin that calls OpenAI, Claude, and Google Gemini APIs for translation with the following features:
* Support for multiple models (OpenAI, Claude, Gemini, DeepSeek)
* **CAT Tag Preservation & Highlighted Display**: Fixes OmegaT's native limitation where inline XML/DocBook tags (`<guibutton>`, `<placeholder .../>`, `<b0>`) are swallowed by the MT pane; renders tags with adaptive syntax highlighting while preserving pristine tags for `Ctrl+M` insertion
* Multi-configuration support with independent tabs for different models and providers
* Dual-Prompt architecture (System Prompt + User Prompt) to customize tone and instructions without compromising tag protection
* Customizable API URL (supports custom proxies and official endpoints)
* Adjustable temperature parameter
* Glossary integration
* High-performance translation caching (Caffeine / JCache)

## Usage
> Using OpenAI, Claude, or Gemini API requires an `API KEY`. This project does not provide API keys.
1. Download the jar file from releases and place it in the OmegaT plugins directory. This directory should be in the plugins folder under your OmegaT installation directory by default.
2. (Required) Open OmegaT, go to Options - Preferences - Machine Translation, select AI LLM Translate, click Configure, and enter your apikey.
3. (Required) Enter the corresponding URL in the `URL` field (for Gemini, leave empty or use `https://generativelanguage.googleapis.com`).
4. (Optional) Enter a custom system prompt in the `System Prompt` field. If not needed, leave it empty to use the built-in CAT tag-preserving prompt with automatic language detection.
5. (Optional) Enter custom additional guidelines in the `User Prompt` field. Content here is appended after the System Prompt (e.g. style, locale conventions, tone) without affecting the built-in tag preservation rules.
6. (Optional) Modify the `temperature` parameter, default is 0.
7. (Optional) Modify the `API format`. Some service providers use OpenAI's API format for Claude/Gemini models. If you don't understand this option, please don't change the default setting. It will automatically determine the API format based on the selected model. Default is set to 'default'.
8. Select the model you want to use from the dropdown list, or check the `Use Custom Model` checkbox to enter a custom model name manually. This allows you to use models not listed in the dropdown.
9. (Optional) Enable the glossary feature. When enabled, matching glossary terms for the current segment will be sent to the API along with the text.
10. (Optional) Enable caching. When enabled, previously translated results will be searched first. Cache is cleared when the project is closed.
11. Finally, check Enable, click OK and close the preferences window.

## Custom Model Support
If you need to use a model that's not listed in the dropdown, you can use the `Use Custom Model` feature:
1. Check the `Use Custom Model` checkbox at the bottom of the configuration dialog
2. When checked, the model dropdown will be replaced with a text input field
3. Enter the exact model name as required by your API provider
4. This is useful for:
   - Newly released models that haven't been added to the list yet
   - Custom fine-tuned models
   - Models from alternative API providers that use compatible formats

## CAT Tag Preservation & Highlighted Display
In OmegaT, machine-translated results with inline XML, HTML, or DocBook tags (such as `<guibutton>...</guibutton>`, `<placeholder type="inlinemediaobject" id="0"/>`, `<b0>...</b0>`) were historically hidden by OmegaT's built-in HTML display pane because Swing's renderer treats unknown tags as invisible markup.

This plugin introduces an intelligent **Tag-Aware rendering engine** (`TagAwareHTMLEditorKit`):
* **Visible & Syntax-Highlighted**: Inline tags are rendered visibly in the Machine Translation pane with beautiful, theme-adaptive syntax highlighting (vibrant blue for inline tags, royal purple for placeholder tags; automatically adjusted for light and dark themes).
* **Native `Ctrl+M` Integration**: Unlike naive HTML entity approaches, the underlying translation data in memory remains pure, unmodified text. Pressing `Ctrl+M` inserts the exact, valid XML tags into the editor, passing OmegaT's Tag Validation 100%.
* **Universal Benefit**: Once loaded, all other MT engines enabled in OmegaT (such as Google Translate or DeepL) also automatically benefit from this tag display fix.

## About Prompts
### System Prompt & User Prompt
The plugin supports two separate prompt inputs for maximum flexibility and tag safety:
1. **System Prompt**:
   - If left empty, the built-in CAT tag-preserving default prompt will be used.
   - If filled in, it will override the default prompt.
2. **User Prompt (Additional Instructions)**:
   - Content entered here is **appended** after the System Prompt under `ADDITIONAL INSTRUCTIONS:`.
   - This allows you to add custom guidelines (e.g. style, locale conventions, tone) without losing the built-in tag-preservation and format rules.

Both prompts support the following language placeholders:
* `%1$s` or `${sourceLang}`: Source language code (e.g., `en`)
* `%2$s` or `${targetLang}`: Target language code (e.g., `zh` or `zh-TW`)
* Sequential `%s` placeholders are also supported (first for source, second for target).

For example, in **User Prompt**:
```text
When %2$s is zh-TW, use Traditional Chinese terms and punctuation conventions for Taiwan.
```

### Glossary-related Prompt
The glossary section is appended directly after the translation prompt in the format: `You can refer to the following glossary when translating: {glossary}`. If there are no matching glossary terms for the current segment, this prompt will not be added.

## License
This project is licensed under GPL 3.0.

## Credits & Acknowledgments
* **Original Project & Author**: This repository is a fork of [omegat-openaitrans-plugin](https://github.com/inertia42/omegat-openaitrans-plugin) created by [Inertia (@inertia42)](https://github.com/inertia42). Sincere appreciation to Inertia for laying the initial groundwork and architecture!
* Special thanks to [omegat-niutrans-plugin](https://github.com/xflcx1991/omegat-niutrans-plugin) and [azure-translate-plugin](https://github.com/omegat-org/azure-translate-plugin) for reference implementations and foundational code.