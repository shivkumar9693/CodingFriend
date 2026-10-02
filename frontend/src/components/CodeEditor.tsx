import React, { useRef, useState, useEffect, useCallback, useMemo } from 'react';
import Editor, { OnMount } from '@monaco-editor/react';
import type * as monaco from 'monaco-editor';
import { Sparkles, X, ExternalLink } from 'lucide-react';
import { LoadingSpinner } from './LoadingSpinner';
import { InlineAssistResponse } from '../types/mentor';
import { useTheme } from '../context/ThemeContext';
import { ComplexityRadarService } from '../services/complexityRadarService';

export interface CodeEditorProps {
  code: string;
  onChange: (value: string) => void;
  onCursorChange?: (line: number, column: number) => void;
  language?: string;
  readOnly?: boolean;
  height?: string | number;
  theme?: string;
  inlineNudge?: InlineAssistResponse | null;
  onDismissNudge?: () => void;
  onAskMoreMentor?: (customPrompt?: string) => void;
  showComplexityRadar?: boolean;
  debugActiveLine?: number | null;
}

export const CodeEditor: React.FC<CodeEditorProps> = ({
  code,
  onChange,
  onCursorChange,
  language = 'java',
  readOnly = false,
  height = '380px',
  theme: customTheme,
  inlineNudge,
  onDismissNudge,
  onAskMoreMentor,
  showComplexityRadar: _showComplexityRadar = true,
  debugActiveLine,
}) => {
  const { theme: appTheme } = useTheme();
  const activeMonacoTheme = customTheme || (appTheme === 'light' ? 'vs' : 'vs-dark');
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const monacoRef = useRef<typeof monaco | null>(null);
  const decorationsRef = useRef<monaco.editor.IEditorDecorationsCollection | null>(null);
  const [nudgeTop, setNudgeTop] = useState<number | null>(null);

  // Real-Time Complexity & Cognitive Detour Radar Analysis
  const radarAnalysis = useMemo(() => {
    return ComplexityRadarService.analyzeCode(code, language);
  }, [code, language]);

  const updateNudgePosition = useCallback(() => {
    if (editorRef.current && inlineNudge) {
      const line = Math.max(1, inlineNudge.targetLine);
      const lineTop = editorRef.current.getTopForLineNumber(line);
      const scrollTop = editorRef.current.getScrollTop();
      const visibleTop = lineTop - scrollTop + 24;
      setNudgeTop(Math.max(10, visibleTop));
    } else {
      setNudgeTop(null);
    }
  }, [inlineNudge]);

  useEffect(() => {
    updateNudgePosition();
  }, [inlineNudge, updateNudgePosition]);

  // Scroll to active debug line when stepping
  useEffect(() => {
    if (editorRef.current && debugActiveLine && debugActiveLine > 0) {
      editorRef.current.revealLineInCenterIfOutsideViewport(debugActiveLine);
    }
  }, [debugActiveLine]);

  const handleEditorDidMount: OnMount = (editor, monacoInstance) => {
    editorRef.current = editor;
    monacoRef.current = monacoInstance;

    editor.onDidScrollChange(() => {
      updateNudgePosition();
    });

    editor.onDidChangeCursorPosition((e) => {
      if (onCursorChange) {
        onCursorChange(e.position.lineNumber, e.position.column);
      }
      updateNudgePosition();
    });

    editor.updateOptions({
      tabSize: 4,
      insertSpaces: true,
      fontFamily: "'Fira Code', 'Cascadia Code', 'JetBrains Mono', Consolas, 'Courier New', monospace",
      fontSize: 14,
      lineHeight: 22,
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      automaticLayout: true,
      cursorBlinking: 'smooth',
      smoothScrolling: true,
      contextmenu: true,
      formatOnPaste: true,
      formatOnType: true,
      renderLineHighlight: 'all',
      bracketPairColorization: { enabled: true },
      scrollbar: {
        alwaysConsumeMouseWheel: false,
        verticalScrollbarSize: 8,
        horizontalScrollbarSize: 8,
      },
      suggest: {
        showKeywords: true,
        showSnippets: true,
      },
    });
  };

  // Update line highlights when inline nudge, radar detour, or step debugger is active
  useEffect(() => {
    if (!editorRef.current || !monacoRef.current) return;

    const monacoInstance = monacoRef.current;
    const decorations: monaco.editor.IModelDeltaDecoration[] = [];

    if (debugActiveLine && debugActiveLine > 0) {
      decorations.push({
        range: new monacoInstance.Range(debugActiveLine, 1, debugActiveLine, 1),
        options: {
          isWholeLine: true,
          className: 'monaco-debug-active-line',
          glyphMarginClassName: 'monaco-debug-glyph-arrow',
        },
      });
    }

    if (inlineNudge && inlineNudge.targetLine > 0) {
      decorations.push({
        range: new monacoInstance.Range(inlineNudge.targetLine, 1, inlineNudge.targetLine, 1),
        options: {
          isWholeLine: true,
          className: 'socrabuddy-editor-line-highlight',
        },
      });
    }

    if (radarAnalysis.flaggedLine && radarAnalysis.status === 'DETOUR_ERROR') {
      decorations.push({
        range: new monacoInstance.Range(radarAnalysis.flaggedLine, 1, radarAnalysis.flaggedLine, 1),
        options: {
          isWholeLine: true,
          className: 'radar-detour-line-highlight',
        },
      });
    }

    decorationsRef.current = editorRef.current.createDecorationsCollection(decorations);

    return () => {
      if (decorationsRef.current) {
        decorationsRef.current.clear();
      }
    };
  }, [debugActiveLine, inlineNudge, radarAnalysis]);


  return (
    <div className="monaco-editor-wrapper" data-testid="monaco-editor-wrapper">
      <Editor
        height={height}
        language={language}
        value={code}
        theme={activeMonacoTheme}
        options={{
          readOnly,
          tabSize: 4,
          insertSpaces: true,
          fontFamily: "'Fira Code', 'Cascadia Code', 'JetBrains Mono', Consolas, monospace",
          fontSize: 14,
          lineHeight: 22,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          automaticLayout: true,
          cursorBlinking: 'smooth',
          smoothScrolling: true,
          contextmenu: true,
          renderLineHighlight: 'all',
          bracketPairColorization: { enabled: true },
          wordWrap: 'on',
          scrollbar: {
            alwaysConsumeMouseWheel: false,
            verticalScrollbarSize: 8,
            horizontalScrollbarSize: 8,
          },
        }}
        onChange={(val) => onChange(val || '')}
        onMount={handleEditorDidMount}
        loading={
          <div className="editor-loading-fallback">
            <LoadingSpinner message="Loading Java Monaco Editor..." size="sm" />
          </div>
        }
      />

      {/* Inline Socratic Suggestion Box anchored inside Code Editor at target line */}
      {inlineNudge && nudgeTop !== null && (
        <div
          className="monaco-inline-socratic-box"
          style={{ top: `${nudgeTop}px` }}
          data-testid="monaco-inline-socratic-box"
        >
          <div className="inline-box-header">
            <div className="inline-box-title-group">
              <Sparkles size={13} className="sparkle-active text-yellow" />
              <span className="inline-box-title">
                SocraBuddy • Line {inlineNudge.targetLine}
              </span>
              <span className="inline-box-tag">
                {inlineNudge.nudgeType.replace(/_/g, ' ')}
              </span>
            </div>

            <button
              type="button"
              className="inline-box-close-btn"
              onClick={onDismissNudge}
              title="Dismiss suggestion (Esc)"
              aria-label="Dismiss"
            >
              <X size={12} />
            </button>
          </div>

          <div className="inline-box-body">
            <p className="inline-box-prompt">"{inlineNudge.inlineMessage}"</p>
          </div>

          <div className="inline-box-footer">
            {onAskMoreMentor && (
              <button
                type="button"
                className="btn-inline-ask-more"
                onClick={() => onAskMoreMentor()}
                title="Ask SocraBuddy more in interactive dialog"
              >
                <span>Ask SocraBuddy</span>
                <ExternalLink size={11} />
              </button>
            )}

            <button
              type="button"
              className="btn-inline-dismiss"
              onClick={onDismissNudge}
              title="Dismiss suggestion (Esc)"
            >
              <span>Dismiss (Esc)</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
