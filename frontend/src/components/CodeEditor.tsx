import React, { useRef } from 'react';
import Editor, { OnMount } from '@monaco-editor/react';
import type * as monaco from 'monaco-editor';
import { LoadingSpinner } from './LoadingSpinner';

export interface CodeEditorProps {
  code: string;
  onChange: (value: string) => void;
  language?: string;
  readOnly?: boolean;
  height?: string | number;
}

export const CodeEditor: React.FC<CodeEditorProps> = ({
  code,
  onChange,
  language = 'java',
  readOnly = false,
  height = '380px',
}) => {
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);

  const handleEditorDidMount: OnMount = (editor, _monaco) => {
    editorRef.current = editor;
    // Format on load if valid
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

  return (
    <div className="monaco-editor-wrapper" data-testid="monaco-editor-wrapper">
      <Editor
        height={height}
        language={language}
        value={code}
        theme="vs-dark"
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
    </div>
  );
};
