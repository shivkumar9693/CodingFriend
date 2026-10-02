/**
 * Cognitive Detour & Real-Time Complexity Radar Engine
 * Evaluates code in real-time for Time Complexity (O(1), O(log N), O(N), O(N^2), O(2^N)),
 * Space Complexity (O(1), O(N)), and flags cognitive detours/flaws.
 */

export type RadarStatus = 'OPTIMAL' | 'SUB_OPTIMAL' | 'DETOUR_ERROR';

export interface ComplexityRadarResult {
  status: RadarStatus; // 'OPTIMAL' (green), 'SUB_OPTIMAL' (yellow), 'DETOUR_ERROR' (red)
  timeComplexity: string; // e.g. "O(N)", "O(N²)", "O(1)", "O(2ⁿ)"
  spaceComplexity: string; // e.g. "O(1)", "O(N)"
  headline: string; // e.g. "Optimal O(N) Traversal"
  detailMessage: string; // e.g. "Nested loop detected. Consider using a hash map to reduce to O(N)."
  flaggedLine?: number;
  detourName?: string;
  loopDepth: number;
}

export class ComplexityRadarService {
  public static analyzeCode(code: string, language: string = 'JAVA'): ComplexityRadarResult {
    const trimmed = code.trim();
    if (!trimmed) {
      return {
        status: 'OPTIMAL',
        timeComplexity: 'O(1)',
        spaceComplexity: 'O(1)',
        headline: 'Awaiting Code Entry',
        detailMessage: 'Start typing your solution to see live complexity analysis.',
        loopDepth: 0,
      };
    }

    const lines = code.split('\n');

    // 1. Check for Critical Cognitive Detours & Unused Hardcoded Variables (RED)
    const detourCheck = this.findCognitiveDetours(lines, language);
    if (detourCheck) {
      return detourCheck;
    }

    // 2. Syntax / Bracket Imbalance or Incomplete Condition check (RED)
    const syntaxCheck = this.checkBasicSyntaxAnomalies(code, language);
    if (syntaxCheck) {
      return syntaxCheck;
    }

    // 3. Loop Nesting Depth Analysis (Time Complexity)
    const loopAnalysis = this.analyzeLoopDepth(lines, language);

    // 4. Space Complexity Detection (auxiliary data structures)
    const spaceComplexity = this.analyzeSpaceComplexity(code);

    if (loopAnalysis.depth >= 2) {
      return {
        status: 'SUB_OPTIMAL',
        timeComplexity: loopAnalysis.depth === 2 ? 'O(N²)' : `O(N^${loopAnalysis.depth})`,
        spaceComplexity,
        headline: `Sub-Optimal ${loopAnalysis.depth === 2 ? 'O(N²)' : 'Polynomial'} Complexity`,
        detailMessage: 'Nested iteration detected. Can a Hash Map or Two-Pointer approach eliminate the inner loop?',
        flaggedLine: loopAnalysis.flaggedLine,
        loopDepth: loopAnalysis.depth,
      };
    }

    // 5. Recursion without memoization detection (Exponential O(2^N) / Sub-optimal)
    const recursiveCheck = this.detectRecursiveBranching(code);
    if (recursiveCheck) {
      return recursiveCheck;
    }

    // 6. Optimal O(N) or O(1) Solution (GREEN)
    const hasLinearLoop = loopAnalysis.depth === 1;
    return {
      status: 'OPTIMAL',
      timeComplexity: hasLinearLoop ? 'O(N)' : 'O(1)',
      spaceComplexity,
      headline: hasLinearLoop ? 'Optimal O(N) Traversal' : 'Optimal O(1) Constant Logic',
      detailMessage: hasLinearLoop
        ? 'Clean linear-time pass. Memory usage and data flow are balanced.'
        : 'Constant-time logical operations detected without runaway iteration.',
      loopDepth: loopAnalysis.depth,
    };
  }

  private static findCognitiveDetours(lines: string[], _language: string): ComplexityRadarResult | null {
    // Pattern: Variable declaration like "int ab = 2;", "let temp = ...", "x = 99"
    const varDeclRegex = /(?:int|double|long|String|var|let|const)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*([^;]+);/g;

    for (let i = 0; i < lines.length; i++) {
      const lineText = lines[i];
      // Skip loop counters like for (int i = 0...)
      if (/^\s*(for|while|if|switch)\b/.test(lineText)) continue;

      let match;
      while ((match = varDeclRegex.exec(lineText)) !== null) {
        const varName = match[1];
        // Ignore standard algorithmic accumulator names
        const standardNames = ['result', 'res', 'ans', 'sum', 'count', 'max', 'min', 'map', 'set', 'seen', 'head', 'curr', 'left', 'right', 'complement', 'comp', 'idx', 'target', 'nums', 'arr'];
        if (standardNames.includes(varName.toLowerCase())) continue;

        // Check occurrences across entire code
        const varUsageRegex = new RegExp(`\\b${varName}\\b`, 'g');
        const totalMatches = (lines.join('\n').match(varUsageRegex) || []).length;

        // If variable is declared but only appears once (never read or returned)
        if (totalMatches <= 1) {
          return {
            status: 'DETOUR_ERROR',
            timeComplexity: '⚠️ Detour',
            spaceComplexity: 'O(1)',
            headline: 'Cognitive Detour Flagged',
            detailMessage: `Variable '${varName}' on Line ${i + 1} is declared but never referenced in your return path. Is this variable necessary?`,
            flaggedLine: i + 1,
            detourName: varName,
            loopDepth: 0,
          };
        }
      }
    }

    return null;
  }

  private static checkBasicSyntaxAnomalies(code: string, _language: string): ComplexityRadarResult | null {
    // Check unmatched braces
    let braceCount = 0;
    for (const ch of code) {
      if (ch === '{') braceCount++;
      if (ch === '}') braceCount--;
    }

    if (braceCount > 0) {
      return {
        status: 'DETOUR_ERROR',
        timeComplexity: 'Syntax Anomaly',
        spaceComplexity: 'O(1)',
        headline: 'Unclosed Code Block',
        detailMessage: `Missing ${braceCount} closing brace '}'. Structure your method body properly before submitting.`,
        loopDepth: 0,
      };
    }

    // Check infinite loop risk: while(true) without break
    if (/while\s*\(\s*(true|1)\s*\)/.test(code) && !code.includes('break') && !code.includes('return')) {
      return {
        status: 'DETOUR_ERROR',
        timeComplexity: 'O(∞) Infinite Loop Risk',
        spaceComplexity: 'O(1)',
        headline: 'Potential Infinite Loop (TLE Risk)',
        detailMessage: 'Loop condition while(true) has no visible break or return exit path.',
        loopDepth: 1,
      };
    }

    return null;
  }

  private static analyzeLoopDepth(lines: string[], _language: string): { depth: number; flaggedLine?: number } {
    let currentDepth = 0;
    let maxDepth = 0;
    let deepestLine: number | undefined;

    // Track loop boundaries
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      const hasLoop = /\b(for|while)\s*\(/.test(line) || /^\s*for\s+[a-zA-Z0-9_]+\s+in\s+/.test(line);

      if (hasLoop) {
        currentDepth++;
        if (currentDepth > maxDepth) {
          maxDepth = currentDepth;
          deepestLine = i + 1;
        }
      }

      // Decrement on closing braces if they close a loop block
      const closeBraces = (line.match(/\}/g) || []).length;
      if (closeBraces > 0 && currentDepth > 0) {
        currentDepth = Math.max(0, currentDepth - closeBraces);
      }
    }

    return { depth: maxDepth, flaggedLine: deepestLine };
  }

  private static analyzeSpaceComplexity(code: string): string {
    const hasHeapCollection =
      /\b(new\s+(HashMap|HashSet|ArrayList|LinkedList|TreeMap|TreeSet|PriorityQueue)|make\s*\(\s*map|new\s+Map|new\s+Set|\[\])\b/.test(code) ||
      /\b(dict|set|list)\s*\(/.test(code) ||
      /seen\s*=\s*\{/.test(code);

    return hasHeapCollection ? 'O(N)' : 'O(1)';
  }

  private static detectRecursiveBranching(code: string): ComplexityRadarResult | null {
    // Detect multiple self-calls in same method without memoization table
    const twoSumSelfCall = (code.match(/\btwoSum\s*\(/g) || []).length;
    const solveSelfCall = (code.match(/\bsolve\s*\(/g) || []).length;

    if ((twoSumSelfCall > 2 || solveSelfCall > 2) && !code.includes('memo') && !code.includes('dp')) {
      return {
        status: 'SUB_OPTIMAL',
        timeComplexity: 'O(2ⁿ) Exponential Risk',
        spaceComplexity: 'O(N) Call Stack',
        headline: 'Unmemoized Recursive Branching',
        detailMessage: 'Multiple recursive calls detected without memoization table. Consider applying dynamic programming or caching.',
        loopDepth: 2,
      };
    }

    return null;
  }
}
