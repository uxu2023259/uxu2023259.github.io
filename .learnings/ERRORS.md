# Errors

Command failures and integration errors.

---

## [ERR-20260517-001] git_diff_not_repository

**Logged**: 2026-05-17T19:14:49
**Priority**: low
**Status**: resolved
**Area**: config

### Summary
在非 Git 仓库目录中执行 git diff 失败。

### Error
``
warning: Not a git repository. Use --no-index to compare two paths outside a working tree
``

### Context
- 尝试查看 plugin-template.html 的修改差异。
- 当前目录不是 Git 仓库，因此 git diff -- 'plugin-template.html' 无法使用。

### Suggested Fix
先确认目录是否为 Git 仓库；非 Git 仓库中改用静态检查或文件备份对比。

### Metadata
- Reproducible: yes
- Related Files: C:\Users\hyx\Desktop\hyx823894.github.io-main\plugin-template.html

### Resolution
- **Resolved**: 2026-05-17T19:14:49
- **Notes**: 已改用 C:\ripgrep\rg.exe 静态检查确认无渐变、半透明、阴影和可见边框残留。

---
## [ERR-20260517-002] browser_file_url_blocked

**Logged**: 2026-05-17T19:15:31
**Priority**: low
**Status**: resolved
**Area**: frontend

### Summary
尝试用内置浏览器打开本地 ile:// 页面预览时被安全策略拦截。

### Error
``
Browser Use cannot visit the requested page because its URL is blocked by the Browser Use URL policy.
``

### Context
- 目标页面：C:\Users\hyx\Desktop\hyx823894.github.io-main\plugin-template.html
- 目的：预览纯色无边框模板效果。

### Suggested Fix
遇到本地 ile:// 被拦截时，不继续绕过策略；改用静态检查，必要时让用户自行打开页面查看。

### Metadata
- Reproducible: unknown
- Related Files: C:\Users\hyx\Desktop\hyx823894.github.io-main\plugin-template.html

### Resolution
- **Resolved**: 2026-05-17T19:15:31
- **Notes**: 已停止浏览器预览，改用文本检查确认样式规则。

---
## [ERR-20260517-003] powershell_command_too_long

**Logged**: 2026-05-17T20:33:49
**Priority**: low
**Status**: resolved
**Area**: frontend

### Summary
一次性通过 PowerShell 执行过长的内联 Python 生成脚本失败。

### Error
``
文件名或扩展名太长。
``

### Context
- 目标：批量生成 plugins 页面和共享样式。
- 原因：命令内容过长，触发 Windows 命令长度限制。

### Suggested Fix
将生成脚本分块写入文件，或使用文件方式运行脚本，避免超长命令行。

### Metadata
- Reproducible: yes
- Related Files: C:\Users\hyx\Desktop\hyx823894.github.io-main\plugins

### Resolution
- **Resolved**: 2026-05-17T20:33:49
- **Notes**: 改为分块写入生成脚本后执行。

---
## [ERR-20260517-001] powershell_here_document_syntax

**Logged**: 2026-05-17T21:02:01.4892353+08:00
**Priority**: low
**Status**: resolved
**Area**: infra

### Summary
在 PowerShell 中误用了 Bash 风格的 python - <<'PY' here-document，导致解析失败。

### Error
`	ext
ParserError: Missing file specification after redirection operator.
`

### Context
- 尝试用内联 Python 检查 Pillow 是否可用。
- 当前 shell 是 PowerShell，不支持 Bash 的 heredoc 重定向语法。

### Suggested Fix
在 PowerShell 中使用 here-string：@'... '@ | python -，或改用 python -c。

### Metadata
- Reproducible: yes
- Related Files: 无

### Resolution
- **Resolved**: 2026-05-17T21:02:01.4892353+08:00
- **Notes**: 后续改用 PowerShell 原生 here-string 传递内联 Python。

---
## [ERR-20260517-002] image_generation_dimension_mismatch

**Logged**: 2026-05-17T21:34:57.6113601+08:00
**Priority**: low
**Status**: resolved
**Area**: frontend

### Summary
内置图片生成按提示生成了正方形 PNG，但实际尺寸为 1254x1254，而不是提示里的 256x256。

### Error
`	ext
生成结果尺寸：1254x1254
期望使用尺寸：网页图标 256x256
`

### Context
- 正在为网页插件生成 Minecraft 风格图标。
- 内置生成工具保存默认尺寸，不能依赖提示中的精确像素作为最终文件尺寸。

### Suggested Fix
将生成图作为源图，复制到项目后用本地工具统一转码/缩放为 256x256 WebP，再在页面中引用。

### Metadata
- Reproducible: yes
- Related Files: assets/images/plugin-icons/*

### Resolution
- **Resolved**: 2026-05-17T21:34:57.6113601+08:00
- **Notes**: 后续统一使用本地后处理输出最终项目图标。

---
## [ERR-20260517-003] image_gen_unavailable_after_turn_resume

**Logged**: 2026-05-17T21:40:57.7668922+08:00
**Priority**: medium
**Status**: pending
**Area**: frontend

### Summary
用户说“继续”后，当前可用工具列表中没有内置 image_gen 工具，无法继续按默认图片生成路径调用模型生成剩余图标。

### Error
`	ext
本轮工具中未提供 image_gen；只能使用 shell、浏览器、MCP 等工具。
`

### Context
- 任务：为全部插件添加 Minecraft 风格图片图标。
- 已经生成 6 张源图，剩余图标需要改用本地后处理或其他可用方案。

### Suggested Fix
继续使用已生成源图，并用本地脚本创建/补齐统一风格图标；如后续需要真实模型生成的每个独立图标，需在带有 image_gen 工具的回合继续。

### Metadata
- Reproducible: unknown
- Related Files: assets/images/plugin-icons/*

---
## [ERR-20260517-004] missing_bs4_for_local_validation

**Logged**: 2026-05-17T22:42:59.6711292+08:00
**Priority**: low
**Status**: resolved
**Area**: tests

### Summary
本地 HTML 校验尝试导入 BeautifulSoup，但当前 Python 环境未安装 s4。

### Error
`	ext
ModuleNotFoundError: No module named 'bs4'
`

### Context
- 仅需校验静态 HTML 中的图标引用。
- 为避免增加无关依赖，改用 Python 标准库和正则表达式完成检查。

### Suggested Fix
简单静态引用校验优先使用标准库；只有需要严格 HTML 解析时再安装第三方包。

### Metadata
- Reproducible: yes
- Related Files: index.html, plugins/*.html, download.html

### Resolution
- **Resolved**: 2026-05-17T22:42:59.6711292+08:00
- **Notes**: 已改用正则检查资源路径。

---
## [ERR-20260517-005] git_status_not_repository

**Logged**: 2026-05-17T22:52:08.6897936+08:00
**Priority**: low
**Status**: resolved
**Area**: infra

### Summary
尝试用 git status --short 汇总改动时，当前目录不是 Git 仓库。

### Error
`	ext
fatal: not a git repository (or any of the parent directories): .git
`

### Context
- 工作目录：C:\Users\hyx\Desktop\hyx823894.github.io-main
- 该任务可通过文件系统扫描和资源引用校验继续完成，不依赖 Git。

### Suggested Fix
若需要版本差异，请先确认仓库目录或初始化 Git；当前任务改用 C:\ripgrep\rg.exe 和文件检查汇总。

### Metadata
- Reproducible: yes
- Related Files: 无

### Resolution
- **Resolved**: 2026-05-17T22:52:08.6897936+08:00
- **Notes**: 已改用文件扫描完成验证。

---
## [ERR-20260518-001] powershell_redirection

**Logged**: 2026-05-18T00:00:00+08:00
**Priority**: low
**Status**: pending
**Area**: infra

### Summary
在 PowerShell 中误用 Bash 风格 here-doc 重定向导致命令解析失败。

### Error
```text
ParserError: Missing file specification after redirection operator.
```

### Context
- 尝试执行 `python - <<'PY'`。
- 当前 shell 是 PowerShell，应使用 `@' ... '@ | python -`。

### Suggested Fix
PowerShell 环境下运行内联 Python 时统一使用 here-string 管道格式。

### Metadata
- Reproducible: yes
- Related Files: C:\Users\hyx\Desktop\hyx823894.github.io-main

---
## [ERR-20260518-002] browser_file_url_policy

**Logged**: 2026-05-18T00:00:00+08:00
**Priority**: low
**Status**: pending
**Area**: frontend

### Summary
使用浏览器插件打开本地 file:// 页面时被浏览器安全策略阻止。

### Error
```text
Browser Use cannot visit the requested page because its URL is blocked by the Browser Use URL policy.
```

### Context
- 目标是验证本地静态 HTML 页面视觉与链接。
- 浏览器策略明确要求不要通过绕过方式访问同一页面。
- 已改用 HTML 解析、路径存在性与 UTF-8 校验作为安全替代验证。

### Suggested Fix
后续需要浏览器可视检查时，优先让用户确认可访问的本地 HTTP 预览方式；若策略阻止，不再尝试规避。

### Metadata
- Reproducible: yes
- Related Files: C:\Users\hyx\Desktop\hyx823894.github.io-main\index.html

---
## [ERR-20260518-003] tpslag_icon_script_scaling

**Logged**: 2026-05-18T00:00:00+08:00
**Priority**: low
**Status**: pending
**Area**: frontend

### Summary
重绘 TPS 压测图标时，缩放辅助函数没有处理多边形点列表，导致脚本失败。

### Error
```text
TypeError: type tuple doesn't define __round__ method
```

### Context
- 图标绘制脚本把坐标框和多边形点列表都传入同一个缩放函数。
- 多边形参数是嵌套 tuple 列表，不能直接 round。

### Suggested Fix
区分缩放坐标框和缩放多边形点列表，分别使用 box() 与 poly() 辅助函数。

### Metadata
- Reproducible: yes
- Related Files: C:\Users\hyx\Desktop\hyx823894.github.io-main\assets\images\plugin-icons\tpslag.webp

---
