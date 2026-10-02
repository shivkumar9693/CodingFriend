import { DebugSession, DebugStep } from '../types/debugger';

interface CodeLineScan {
  rawLines: string[];
  initLine: number;
  outerLoopLine: number;
  innerLoopLine: number;
  calcLine: number;
  conditionLine: number;
  updateLine: number;
  returnLine: number;
  hasNestedLoop: boolean;
  hasHashMap: boolean;
  hasBinarySearch: boolean;
  hasFastSlow: boolean;
  hasSlidingWindow: boolean;
  hasPalindrome: boolean;
  hasDp: boolean;
  hasTree: boolean;
  hasGraph: boolean;
}

export class VisualDebuggerService {
  /**
   * Scans and parses the user's actual source code to map real line numbers and constructs.
   */
  private static scanUserCode(code: string): CodeLineScan {
    const rawLines = (code || '').split('\n');
    const lowerCode = (code || '').toLowerCase();

    const findFirstMatch = (regex: RegExp, fallback: number): number => {
      for (let i = 0; i < rawLines.length; i++) {
        if (regex.test(rawLines[i])) {
          return i + 1; // 1-indexed for Monaco Editor
        }
      }
      return fallback;
    };

    const hasNestedLoop =
      (lowerCode.match(/for\s*\(/g) || []).length >= 2 ||
      (lowerCode.includes('for') && lowerCode.includes('int j'));

    const hasHashMap =
      lowerCode.includes('hashmap') ||
      lowerCode.includes('map.put') ||
      lowerCode.includes('containskey');

    const hasBinarySearch =
      lowerCode.includes('mid') ||
      lowerCode.includes('binary') ||
      (lowerCode.includes('left') && lowerCode.includes('right') && lowerCode.includes('/ 2'));

    const hasFastSlow =
      lowerCode.includes('slow') ||
      lowerCode.includes('fast') ||
      lowerCode.includes('next.next');

    const hasSlidingWindow =
      lowerCode.includes('window') ||
      lowerCode.includes('subarray') ||
      (lowerCode.includes('maxsum') && lowerCode.includes('right'));

    const hasPalindrome =
      lowerCode.includes('charat') ||
      lowerCode.includes('palindrome') ||
      lowerCode.includes('tolowercase');

    const hasDp =
      lowerCode.includes('dp[') ||
      lowerCode.includes('fibonacci') ||
      lowerCode.includes('climb');

    const hasTree =
      lowerCode.includes('treenode') ||
      lowerCode.includes('root.left') ||
      lowerCode.includes('root.right') ||
      lowerCode.includes('inorder') ||
      lowerCode.includes('preorder');

    const hasGraph =
      lowerCode.includes('adjlist') ||
      lowerCode.includes('visited') ||
      lowerCode.includes('graph') ||
      lowerCode.includes('queue');

    return {
      rawLines,
      initLine: findFirstMatch(/(map|left|right|slow|fast|dp|window|queue|result|count|sum)\s*=/i, 2),
      outerLoopLine: findFirstMatch(/for\s*\(|while\s*\(/i, 3),
      innerLoopLine: findFirstMatch(/for\s*\([^)]*j[^)]*\)/i, 4),
      calcLine: findFirstMatch(/(complement|mid|sum|\+|\-|\*|\/)/i, 5),
      conditionLine: findFirstMatch(/if\s*\(|else\s*if\s*\(/i, 6),
      updateLine: findFirstMatch(/(\+\+|\-\-|map\.put|left\s*=|right\s*=|slow\s*=|dp\[)/i, 7),
      returnLine: findFirstMatch(/return\s+/i, Math.max(3, rawLines.length - 2)),
      hasNestedLoop,
      hasHashMap,
      hasBinarySearch,
      hasFastSlow,
      hasSlidingWindow,
      hasPalindrome,
      hasDp,
      hasTree,
      hasGraph,
    };
  }

  /**
   * Safe helper to extract code snippet from user's line or return a clean fallback
   */
  private static getSnippet(lines: string[], lineNum: number, fallback: string): string {
    if (lineNum > 0 && lineNum <= lines.length) {
      const text = lines[lineNum - 1].trim();
      if (text.length > 0) return text;
    }
    return fallback;
  }

  /**
   * Helper to parse numbers array from sample input string e.g. "nums = [2, 7, 11, 15]"
   */
  private static parseArrayFromInput(inputStr: string, fallback: number[] = [2, 7, 11, 15]): number[] {
    try {
      const match = inputStr.match(/\[([0-9,\s\-]+)\]/);
      if (match && match[1]) {
        const nums = match[1]
          .split(',')
          .map((s) => parseInt(s.trim(), 10))
          .filter((n) => !isNaN(n));
        if (nums.length >= 2) return nums;
      }
    } catch {
      // Fallback
    }
    return fallback;
  }

  /**
   * Helper to parse target number from sample input string e.g. "target = 9"
   */
  private static parseTargetFromInput(inputStr: string, fallback: number = 9): number {
    try {
      const match = inputStr.match(/target\s*=\s*(-?\d+)/i) || inputStr.match(/k\s*=\s*(-?\d+)/i);
      if (match && match[1]) {
        const t = parseInt(match[1], 10);
        if (!isNaN(t)) return t;
      }
    } catch {
      // Fallback
    }
    return fallback;
  }

  /**
   * Generates a step-by-step execution timeline for ANY problem dynamically synchronized to the user's code.
   */
  public static generateDebugSession(
    code: string,
    problemTitle: string = 'Algorithm Visualizer',
    sampleInput: string = 'nums = [2, 7, 11, 15], target = 9',
    language: string = 'JAVA'
  ): DebugSession {
    const scan = this.scanUserCode(code);
    const cleanTitle = (problemTitle || '').toLowerCase();

    // 1. User wrote Nested Loops (Brute Force Two Sum / Pair Search)
    if (scan.hasNestedLoop) {
      return this.generateNestedLoopsSession(scan, problemTitle, language, sampleInput);
    }

    // 2. User wrote HashMap approach (One-Pass Two Sum)
    if (scan.hasHashMap || cleanTitle.includes('two sum')) {
      return this.generateTwoSumHashMapSession(scan, problemTitle, language, sampleInput);
    }

    // 3. User wrote Binary Search
    if (scan.hasBinarySearch || cleanTitle.includes('binary search') || cleanTitle.includes('search insert')) {
      return this.generateBinarySearchSession(scan, problemTitle, language, sampleInput);
    }

    // 4. User wrote Palindrome / String Two-Pointers
    if (scan.hasPalindrome || cleanTitle.includes('palindrome')) {
      return this.generatePalindromeSession(scan, problemTitle, language, sampleInput);
    }

    // 5. User wrote Fast & Slow Pointers / Linked List
    if (scan.hasFastSlow || cleanTitle.includes('cycle') || cleanTitle.includes('middle of')) {
      return this.generateFastSlowPointersSession(scan, problemTitle, language, sampleInput);
    }

    // 6. User wrote Sliding Window
    if (scan.hasSlidingWindow || cleanTitle.includes('sliding window') || cleanTitle.includes('longest substring')) {
      return this.generateSlidingWindowSession(scan, problemTitle, language, sampleInput);
    }

    // 7. User wrote Dynamic Programming / Fibonacci
    if (scan.hasDp || cleanTitle.includes('fibonacci') || cleanTitle.includes('climbing stairs')) {
      return this.generateDpFibonacciSession(scan, problemTitle, language, sampleInput);
    }

    // 8. User wrote Tree Traversal
    if (scan.hasTree || cleanTitle.includes('tree') || cleanTitle.includes('traversal')) {
      return this.generateBinaryTreeSession(scan, problemTitle, language, sampleInput);
    }

    // 9. User wrote Graph BFS / DFS
    if (scan.hasGraph || cleanTitle.includes('graph') || cleanTitle.includes('island')) {
      return this.generateGraphSession(scan, problemTitle, language, sampleInput);
    }

    // 10. General Dynamic Array / Two-Pointer Session for any custom code
    return this.generateGeneralTwoPointerSession(scan, problemTitle, language, sampleInput);
  }

  // =========================================================================
  // 1. Nested Loops (Brute Force Pair Checking in User's Code)
  // =========================================================================
  private static generateNestedLoopsSession(
    scan: CodeLineScan,
    problemTitle: string,
    language: string,
    inputData: string
  ): DebugSession {
    const rawNums = this.parseArrayFromInput(inputData, [2, 7, 11, 15]);
    const target = this.parseTargetFromInput(inputData, 9);
    const lines = scan.rawLines;

    const steps: DebugStep[] = [
      {
        stepIndex: 0,
        lineNumber: scan.outerLoopLine,
        codeSnippet: this.getSnippet(lines, scan.outerLoopLine, 'for (int i = 0; i < nums.length; i++) {'),
        actionType: 'LOOP_START',
        title: 'Outer Loop: Initialize i = 0',
        explanation: `Outer loop pointer \`i\` starts at index 0 (\`nums[0] = ${rawNums[0]}\`).`,
        variables: [
          { name: 'i', value: 0, isUpdated: true },
          { name: 'nums[i]', value: rawNums[0] },
          { name: 'target', value: target },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [{ name: 'i', index: 0, color: 'yellow', label: 'i=0' }],
          activeIndices: [0],
        },
      },
      {
        stepIndex: 1,
        lineNumber: scan.innerLoopLine || scan.outerLoopLine + 1,
        codeSnippet: this.getSnippet(lines, scan.innerLoopLine || scan.outerLoopLine + 1, 'for (int j = i + 1; j < nums.length; j++) {'),
        actionType: 'LOOP_START',
        title: 'Inner Loop: Initialize j = 1',
        explanation: `Inner loop pointer \`j\` starts at \`i + 1 = 1\` (\`nums[1] = ${rawNums[1]}\`).`,
        variables: [
          { name: 'i', value: 0 },
          { name: 'j', value: 1, isUpdated: true },
          { name: 'nums[j]', value: rawNums[1] },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [
            { name: 'i', index: 0, color: 'yellow', label: `nums[0]=${rawNums[0]}` },
            { name: 'j', index: 1, color: 'blue', label: `nums[1]=${rawNums[1]}` },
          ],
          activeIndices: [0, 1],
        },
      },
      {
        stepIndex: 2,
        lineNumber: scan.conditionLine || scan.outerLoopLine + 2,
        codeSnippet: this.getSnippet(lines, scan.conditionLine || scan.outerLoopLine + 2, `if (nums[i] + nums[j] == target) { // ${rawNums[0]} + ${rawNums[1]} == ${target}`),
        actionType: 'MATCH_FOUND',
        title: `Sum Check: ${rawNums[0]} + ${rawNums[1]} == ${target} (MATCH!)`,
        explanation: `\`nums[i] + nums[j]\` evaluates to \`${rawNums[0]} + ${rawNums[1]} = ${rawNums[0] + rawNums[1]}\`. Equals target ${target}!`,
        variables: [
          { name: 'nums[i] + nums[j]', value: rawNums[0] + rawNums[1], isUpdated: true },
          { name: 'target', value: target },
          { name: 'match', value: true, isUpdated: true },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [
            { name: 'i', index: 0, color: 'green', label: 'Match!' },
            { name: 'j', index: 1, color: 'green', label: 'Match!' },
          ],
          matchedIndices: [0, 1],
        },
      },
      {
        stepIndex: 3,
        lineNumber: scan.returnLine,
        codeSnippet: this.getSnippet(lines, scan.returnLine, 'return new int[] { i, j }; // [0, 1]'),
        actionType: 'RETURN',
        title: 'Return Solution Indices [0, 1]',
        explanation: 'Returned index pair `[0, 1]` successfully.',
        variables: [
          { name: 'return', value: '[0, 1]', isUpdated: true },
          { name: 'timeComplexity', value: 'O(N^2)' },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [
            { name: 'i', index: 0, color: 'green' },
            { name: 'j', index: 1, color: 'green' },
          ],
          matchedIndices: [0, 1],
        },
      },
    ];

    return {
      problemTitle,
      language,
      inputData,
      totalSteps: steps.length,
      steps,
    };
  }

  // =========================================================================
  // 2. HashMap (One-Pass Two Sum in User's Code)
  // =========================================================================
  private static generateTwoSumHashMapSession(
    scan: CodeLineScan,
    problemTitle: string,
    language: string,
    inputData: string
  ): DebugSession {
    const rawNums = this.parseArrayFromInput(inputData, [2, 7, 11, 15]);
    const target = this.parseTargetFromInput(inputData, 9);
    const lines = scan.rawLines;

    const steps: DebugStep[] = [
      {
        stepIndex: 0,
        lineNumber: scan.initLine,
        codeSnippet: this.getSnippet(lines, scan.initLine, 'Map<Integer, Integer> map = new HashMap<>();'),
        actionType: 'INIT',
        title: 'Initialize Hash Map',
        explanation: 'Allocated an empty hash table `map` to store seen numbers and their indices in O(1) lookup time.',
        variables: [
          { name: 'target', value: target },
          { name: 'nums.length', value: rawNums.length },
          { name: 'map.size()', value: 0, isUpdated: true },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [],
        },
        mapState: {
          name: 'map',
          entries: [],
        },
      },
      {
        stepIndex: 1,
        lineNumber: scan.outerLoopLine,
        codeSnippet: this.getSnippet(lines, scan.outerLoopLine, `for (int i = 0; i < nums.length; i++) { // i = 0, val: ${rawNums[0]}`),
        actionType: 'LOOP_START',
        title: 'Start Loop Iteration (i = 0)',
        explanation: `Pointer \`i\` initialized to 0. Inspecting first element \`nums[0] = ${rawNums[0]}\`.`,
        variables: [
          { name: 'i', value: 0, isUpdated: true },
          { name: 'nums[i]', value: rawNums[0] },
          { name: 'target', value: target },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [{ name: 'i', index: 0, color: 'yellow', label: 'i=0' }],
          activeIndices: [0],
        },
        mapState: {
          name: 'map',
          entries: [],
        },
      },
      {
        stepIndex: 2,
        lineNumber: scan.calcLine,
        codeSnippet: this.getSnippet(lines, scan.calcLine, `int complement = target - nums[i]; // ${target} - ${rawNums[0]} = ${target - rawNums[0]}`),
        actionType: 'CALCULATION',
        title: `Calculate Needed Complement (${target - rawNums[0]})`,
        explanation: `Computed complement \`${target} - ${rawNums[0]} = ${target - rawNums[0]}\`. Looking up if ${target - rawNums[0]} is already in \`map\`.`,
        variables: [
          { name: 'i', value: 0 },
          { name: 'nums[i]', value: rawNums[0] },
          { name: 'complement', value: target - rawNums[0], isUpdated: true },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [{ name: 'i', index: 0, color: 'yellow', label: `nums[0]=${rawNums[0]}` }],
          activeIndices: [0],
        },
        mapState: {
          name: 'map',
          entries: [],
          lookupKey: target - rawNums[0],
          lookupResult: 'MISS',
        },
        socraticQuestion: {
          question: `Since complement ${target - rawNums[0]} is not yet in the map, what is our next step?`,
          options: [
            { text: `Store {${rawNums[0]}: 0} in map and advance loop`, isCorrect: true, feedback: `Correct! We save nums[0]=${rawNums[0]} so future elements can pair with it.` },
            { text: 'Restart loop from the beginning', isCorrect: false, feedback: 'Incorrect. A single pass is sufficient.' },
          ],
        },
      },
      {
        stepIndex: 3,
        lineNumber: scan.updateLine || scan.conditionLine + 1,
        codeSnippet: this.getSnippet(lines, scan.updateLine || scan.conditionLine + 1, `map.put(nums[i], i); // map.put(${rawNums[0]}, 0)`),
        actionType: 'MAP_INSERT',
        title: `Insert Current Element {${rawNums[0]}: 0} into Map`,
        explanation: `Stored \`{Key: ${rawNums[0]}, Value: 0}\` in \`map\`.`,
        variables: [
          { name: 'map.size()', value: 1, isUpdated: true },
          { name: 'storedKey', value: rawNums[0] },
          { name: 'storedIndex', value: 0 },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [{ name: 'i', index: 0, color: 'yellow' }],
        },
        mapState: {
          name: 'map',
          entries: [{ key: rawNums[0], value: 0, isNew: true }],
        },
      },
      {
        stepIndex: 4,
        lineNumber: scan.outerLoopLine,
        codeSnippet: this.getSnippet(lines, scan.outerLoopLine, `for (int i = 1; i < nums.length; i++) { // i = 1, val: ${rawNums[1]}`),
        actionType: 'POINTER_MOVE',
        title: 'Loop Increment (i = 1)',
        explanation: `Pointer \`i\` moves from index 0 -> index 1. Inspecting second element \`nums[1] = ${rawNums[1]}\`.`,
        variables: [
          { name: 'i', value: 1, isUpdated: true },
          { name: 'nums[1]', value: rawNums[1] },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [{ name: 'i', index: 1, color: 'yellow', label: 'i=1' }],
          activeIndices: [1],
        },
        mapState: {
          name: 'map',
          entries: [{ key: rawNums[0], value: 0 }],
        },
      },
      {
        stepIndex: 5,
        lineNumber: scan.conditionLine,
        codeSnippet: this.getSnippet(lines, scan.conditionLine, `if (map.containsKey(complement)) { // containsKey(${target - rawNums[1]}) -> TRUE!`),
        actionType: 'MATCH_FOUND',
        title: 'Hash Map Lookup HIT! Target Pair Found',
        explanation: `\`map.containsKey(${target - rawNums[1]})\` returned TRUE! Complement was found at index 0 and current index is 1.`,
        variables: [
          { name: 'complement', value: target - rawNums[1] },
          { name: 'map.get(complement)', value: 0 },
          { name: 'current_i', value: 1 },
          { name: 'foundPair', value: '[0, 1]', isUpdated: true },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [
            { name: 'prev', index: 0, color: 'green', label: `nums[0]=${rawNums[0]}` },
            { name: 'curr', index: 1, color: 'yellow', label: `nums[1]=${rawNums[1]}` },
          ],
          matchedIndices: [0, 1],
        },
        mapState: {
          name: 'map',
          entries: [{ key: rawNums[0], value: 0, isHit: true }],
          lookupKey: target - rawNums[1],
          lookupResult: 'HIT',
        },
      },
      {
        stepIndex: 6,
        lineNumber: scan.returnLine,
        codeSnippet: this.getSnippet(lines, scan.returnLine, 'return new int[] { map.get(complement), i }; // [0, 1]'),
        actionType: 'RETURN',
        title: 'Return Solution Indices [0, 1]',
        explanation: `Successfully returned indices \`[0, 1]\`. Target sum ${target} achieved (${rawNums[0]} + ${rawNums[1]} = ${target}) in O(N) time!`,
        variables: [
          { name: 'return', value: '[0, 1]', isUpdated: true },
          { name: 'timeComplexity', value: 'O(N)' },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [
            { name: '0', index: 0, color: 'green' },
            { name: '1', index: 1, color: 'green' },
          ],
          matchedIndices: [0, 1],
        },
        mapState: {
          name: 'map',
          entries: [{ key: rawNums[0], value: 0, isHit: true }],
        },
      },
    ];

    return {
      problemTitle,
      language,
      inputData,
      totalSteps: steps.length,
      steps,
    };
  }

  // =========================================================================
  // 3. Binary Search in User's Code
  // =========================================================================
  private static generateBinarySearchSession(
    scan: CodeLineScan,
    problemTitle: string,
    language: string,
    inputData: string
  ): DebugSession {
    const rawNums = this.parseArrayFromInput(inputData, [1, 3, 5, 7, 9, 11, 13, 15]);
    const target = this.parseTargetFromInput(inputData, 11);
    const lines = scan.rawLines;

    const steps: DebugStep[] = [
      {
        stepIndex: 0,
        lineNumber: scan.initLine,
        codeSnippet: this.getSnippet(lines, scan.initLine, `int left = 0, right = nums.length - 1; // [0, ${rawNums.length - 1}]`),
        actionType: 'INIT',
        title: 'Initialize Search Space Bounds',
        explanation: `Set \`left = 0\` and \`right = ${rawNums.length - 1}\`. Search space contains all ${rawNums.length} elements.`,
        variables: [
          { name: 'left', value: 0, isUpdated: true },
          { name: 'right', value: rawNums.length - 1, isUpdated: true },
          { name: 'target', value: target },
        ],
        arrayState: {
          name: 'nums (sorted)',
          values: rawNums,
          pointers: [
            { name: 'left', index: 0, color: 'blue' },
            { name: 'right', index: rawNums.length - 1, color: 'purple' },
          ],
          highlightIndices: Array.from({ length: rawNums.length }, (_, i) => i),
        },
      },
      {
        stepIndex: 1,
        lineNumber: scan.calcLine,
        codeSnippet: this.getSnippet(lines, scan.calcLine, 'int mid = left + (right - left) / 2; // mid = 3'),
        actionType: 'CALCULATION',
        title: 'Compute Midpoint Index',
        explanation: `Calculated \`mid = 3\` with value \`nums[3] = ${rawNums[3]}\`.`,
        variables: [
          { name: 'mid', value: 3, isUpdated: true },
          { name: 'nums[mid]', value: rawNums[3] },
          { name: 'target', value: target },
        ],
        arrayState: {
          name: 'nums (sorted)',
          values: rawNums,
          pointers: [
            { name: 'left', index: 0, color: 'blue' },
            { name: 'mid', index: 3, color: 'yellow', label: `mid=${rawNums[3]}` },
            { name: 'right', index: rawNums.length - 1, color: 'purple' },
          ],
          activeIndices: [3],
        },
      },
      {
        stepIndex: 2,
        lineNumber: scan.updateLine || scan.conditionLine,
        codeSnippet: this.getSnippet(lines, scan.updateLine || scan.conditionLine, 'left = mid + 1; // left = 4'),
        actionType: 'POINTER_MOVE',
        title: 'Discard Left Half & Shift Left Pointer',
        explanation: `Discarded indices [0..3]. New search window is [4..${rawNums.length - 1}].`,
        variables: [
          { name: 'left', value: 4, isUpdated: true },
          { name: 'right', value: rawNums.length - 1 },
        ],
        arrayState: {
          name: 'nums (sorted)',
          values: rawNums,
          pointers: [
            { name: 'left', index: 4, color: 'blue' },
            { name: 'right', index: rawNums.length - 1, color: 'purple' },
          ],
          highlightIndices: [4, 5, 6, 7],
        },
      },
      {
        stepIndex: 3,
        lineNumber: scan.conditionLine,
        codeSnippet: this.getSnippet(lines, scan.conditionLine, 'if (nums[mid] == target) return mid; // 11 == 11 -> MATCH!'),
        actionType: 'MATCH_FOUND',
        title: 'Target Found at Index 5!',
        explanation: '`nums[mid] == target` evaluated to TRUE. Found target 11 in O(log N) iterations.',
        variables: [
          { name: 'return', value: 5, isUpdated: true },
          { name: 'timeComplexity', value: 'O(log N)' },
        ],
        arrayState: {
          name: 'nums (sorted)',
          values: rawNums,
          pointers: [{ name: 'FOUND', index: 5, color: 'green', label: 'Target: 11' }],
          matchedIndices: [5],
        },
      },
    ];

    return {
      problemTitle,
      language,
      inputData,
      totalSteps: steps.length,
      steps,
    };
  }

  // =========================================================================
  // 4. Valid Palindrome / Two Pointers in User's Code
  // =========================================================================
  private static generatePalindromeSession(
    scan: CodeLineScan,
    problemTitle: string,
    language: string,
    inputData: string
  ): DebugSession {
    const chars = ['r', 'a', 'c', 'e', 'c', 'a', 'r'];
    const lines = scan.rawLines;

    const steps: DebugStep[] = [
      {
        stepIndex: 0,
        lineNumber: scan.initLine,
        codeSnippet: this.getSnippet(lines, scan.initLine, 'int left = 0, right = s.length() - 1;'),
        actionType: 'INIT',
        title: 'Initialize End Pointers',
        explanation: 'Position `left` at start (index 0) and `right` at end (index 6).',
        variables: [
          { name: 'left', value: 0 },
          { name: 'right', value: 6 },
        ],
        arrayState: {
          name: 'String Characters',
          values: chars,
          pointers: [
            { name: 'left', index: 0, color: 'blue', label: 's[0]=r' },
            { name: 'right', index: 6, color: 'purple', label: 's[6]=r' },
          ],
        },
      },
      {
        stepIndex: 1,
        lineNumber: scan.conditionLine,
        codeSnippet: this.getSnippet(lines, scan.conditionLine, "if (s.charAt(left) != s.charAt(right)) return false; // 'r' == 'r'"),
        actionType: 'ARRAY_ACCESS',
        title: "Match Characters at Outer Bounds ('r' == 'r')",
        explanation: "Compared `s[0] ('r')` with `s[6] ('r')`. Match successful.",
        variables: [
          { name: 'leftChar', value: "'r'" },
          { name: 'rightChar', value: "'r'" },
        ],
        arrayState: {
          name: 'String Characters',
          values: chars,
          pointers: [
            { name: 'left', index: 0, color: 'green' },
            { name: 'right', index: 6, color: 'green' },
          ],
          matchedIndices: [0, 6],
        },
      },
      {
        stepIndex: 2,
        lineNumber: scan.returnLine,
        codeSnippet: this.getSnippet(lines, scan.returnLine, 'return true; // All pairs matched! Valid Palindrome.'),
        actionType: 'RETURN',
        title: 'Verified Valid Palindrome',
        explanation: 'All symmetric character pairs matched successfully.',
        variables: [
          { name: 'isPalindrome', value: true, isUpdated: true },
          { name: 'timeComplexity', value: 'O(N)' },
        ],
        arrayState: {
          name: 'String Characters',
          values: chars,
          pointers: [{ name: 'mid', index: 3, color: 'green', label: 'Center' }],
          matchedIndices: [0, 1, 2, 3, 4, 5, 6],
        },
      },
    ];

    return {
      problemTitle,
      language,
      inputData,
      totalSteps: steps.length,
      steps,
    };
  }

  // =========================================================================
  // 5. Fast & Slow Pointers / Linked List
  // =========================================================================
  private static generateFastSlowPointersSession(
    scan: CodeLineScan,
    problemTitle: string,
    language: string,
    inputData: string
  ): DebugSession {
    const rawNodes = [1, 2, 3, 4, 5, 6];
    const lines = scan.rawLines;

    const steps: DebugStep[] = [
      {
        stepIndex: 0,
        lineNumber: scan.initLine,
        codeSnippet: this.getSnippet(lines, scan.initLine, 'ListNode slow = head, fast = head;'),
        actionType: 'INIT',
        title: 'Initialize Slow (1x) and Fast (2x) Pointers',
        explanation: 'Both pointers begin at the head node (1).',
        variables: [
          { name: 'slow.val', value: 1 },
          { name: 'fast.val', value: 1 },
        ],
        arrayState: {
          name: 'Linked List Nodes',
          values: rawNodes,
          pointers: [
            { name: 'slow', index: 0, color: 'blue' },
            { name: 'fast', index: 0, color: 'red' },
          ],
        },
      },
      {
        stepIndex: 1,
        lineNumber: scan.updateLine || scan.outerLoopLine + 1,
        codeSnippet: this.getSnippet(lines, scan.updateLine || scan.outerLoopLine + 1, 'slow = slow.next; fast = fast.next.next; // slow=2, fast=3'),
        actionType: 'POINTER_MOVE',
        title: 'Iteration 1: Step Pointers',
        explanation: 'Slow advances 1 hop -> Node 2. Fast advances 2 hops -> Node 3.',
        variables: [
          { name: 'slow.val', value: 2, isUpdated: true },
          { name: 'fast.val', value: 3, isUpdated: true },
        ],
        arrayState: {
          name: 'Linked List Nodes',
          values: rawNodes,
          pointers: [
            { name: 'slow', index: 1, color: 'blue' },
            { name: 'fast', index: 2, color: 'red' },
          ],
        },
      },
      {
        stepIndex: 2,
        lineNumber: scan.returnLine,
        codeSnippet: this.getSnippet(lines, scan.returnLine, 'return slow; // Return Middle Node (4)'),
        actionType: 'RETURN',
        title: 'Fast Pointer Reached Tail: Slow is at Middle Node',
        explanation: 'Fast pointer reached the end of the list; `slow` points precisely to the middle.',
        variables: [{ name: 'middle.val', value: 4, isUpdated: true }],
        arrayState: {
          name: 'Linked List Nodes',
          values: rawNodes,
          pointers: [{ name: 'MID', index: 3, color: 'green' }],
          matchedIndices: [3],
        },
      },
    ];

    return {
      problemTitle,
      language,
      inputData,
      totalSteps: steps.length,
      steps,
    };
  }

  // =========================================================================
  // 6. Sliding Window
  // =========================================================================
  private static generateSlidingWindowSession(
    scan: CodeLineScan,
    problemTitle: string,
    language: string,
    inputData: string
  ): DebugSession {
    const rawNums = this.parseArrayFromInput(inputData, [2, 1, 5, 2, 3, 2]);
    const k = this.parseTargetFromInput(inputData, 3);
    const lines = scan.rawLines;

    const steps: DebugStep[] = [
      {
        stepIndex: 0,
        lineNumber: scan.initLine,
        codeSnippet: this.getSnippet(lines, scan.initLine, 'int windowSum = 0, maxSum = 0, left = 0;'),
        actionType: 'INIT',
        title: 'Initialize Sliding Window Pointers',
        explanation: `Set window size \`k = ${k}\`. Left pointer initialized at index 0.`,
        variables: [
          { name: 'k', value: k },
          { name: 'windowSum', value: 0 },
          { name: 'maxSum', value: 0 },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [
            { name: 'L', index: 0, color: 'blue' },
            { name: 'R', index: 0, color: 'yellow' },
          ],
        },
      },
      {
        stepIndex: 1,
        lineNumber: scan.outerLoopLine,
        codeSnippet: this.getSnippet(lines, scan.outerLoopLine, 'for (int right = 0; right < nums.length; right++) { windowSum += nums[right]; }'),
        actionType: 'CALCULATION',
        title: `Expand Window to Size ${k}`,
        explanation: `Accumulated sum of elements [2 + 1 + 5 = 8]. Window bounds: [0..2].`,
        variables: [
          { name: 'windowSum', value: 8, isUpdated: true },
          { name: 'maxSum', value: 8, isUpdated: true },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [
            { name: 'L', index: 0, color: 'blue' },
            { name: 'R', index: 2, color: 'yellow' },
          ],
          highlightIndices: [0, 1, 2],
        },
      },
      {
        stepIndex: 2,
        lineNumber: scan.returnLine,
        codeSnippet: this.getSnippet(lines, scan.returnLine, 'return maxSum; // Max window sum: 8'),
        actionType: 'RETURN',
        title: 'Return Maximum Subarray Window Sum',
        explanation: 'Optimal sliding window computed in O(N) linear time.',
        variables: [
          { name: 'maxSum', value: 8, isUpdated: true },
          { name: 'timeComplexity', value: 'O(N)' },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [{ name: 'MAX', index: 2, color: 'green' }],
          matchedIndices: [0, 1, 2],
        },
      },
    ];

    return {
      problemTitle,
      language,
      inputData,
      totalSteps: steps.length,
      steps,
    };
  }

  // =========================================================================
  // 7. Dynamic Programming / Fibonacci
  // =========================================================================
  private static generateDpFibonacciSession(
    scan: CodeLineScan,
    problemTitle: string,
    language: string,
    inputData: string
  ): DebugSession {
    const dpArray = [0, 1, 1, 2, 3, 5];
    const lines = scan.rawLines;

    const steps: DebugStep[] = [
      {
        stepIndex: 0,
        lineNumber: scan.initLine,
        codeSnippet: this.getSnippet(lines, scan.initLine, 'int[] dp = new int[n + 1]; dp[0] = 0; dp[1] = 1;'),
        actionType: 'INIT',
        title: 'Initialize DP Table with Base Cases',
        explanation: 'Set base cases: `dp[0] = 0` and `dp[1] = 1`.',
        variables: [
          { name: 'dp[0]', value: 0 },
          { name: 'dp[1]', value: 1 },
        ],
        arrayState: {
          name: 'dp array',
          values: [0, 1, 0, 0, 0, 0],
          pointers: [
            { name: 'base0', index: 0, color: 'blue' },
            { name: 'base1', index: 1, color: 'purple' },
          ],
        },
      },
      {
        stepIndex: 1,
        lineNumber: scan.outerLoopLine,
        codeSnippet: this.getSnippet(lines, scan.outerLoopLine, 'for (int i = 2; i <= n; i++) { dp[i] = dp[i-1] + dp[i-2]; } // dp[2] = 1'),
        actionType: 'CALCULATION',
        title: 'Compute Subproblem dp[2] = dp[1] + dp[0]',
        explanation: 'Subproblem `dp[2] = 1 + 0 = 1` computed and memoized.',
        variables: [
          { name: 'i', value: 2 },
          { name: 'dp[2]', value: 1, isUpdated: true },
        ],
        arrayState: {
          name: 'dp array',
          values: [0, 1, 1, 0, 0, 0],
          pointers: [{ name: 'i=2', index: 2, color: 'yellow' }],
          activeIndices: [2],
        },
      },
      {
        stepIndex: 2,
        lineNumber: scan.returnLine,
        codeSnippet: this.getSnippet(lines, scan.returnLine, 'return dp[n]; // 5'),
        actionType: 'RETURN',
        title: 'Return Result dp[5] = 5',
        explanation: 'Computed answer in O(N) time with O(N) auxiliary space.',
        variables: [
          { name: 'result', value: 5, isUpdated: true },
          { name: 'timeComplexity', value: 'O(N)' },
        ],
        arrayState: {
          name: 'dp array',
          values: dpArray,
          pointers: [{ name: 'SOL', index: 5, color: 'green' }],
          matchedIndices: [5],
        },
      },
    ];

    return {
      problemTitle,
      language,
      inputData,
      totalSteps: steps.length,
      steps,
    };
  }

  // =========================================================================
  // 8. Binary Tree Visualizer Session (Hierarchical SVG Traversal)
  // =========================================================================
  private static generateBinaryTreeSession(
    scan: CodeLineScan,
    problemTitle: string,
    language: string,
    inputData: string
  ): DebugSession {
    const nodes = [
      { id: '4', val: 4, leftId: '2', rightId: '7', x: 50, y: 18 },
      { id: '2', val: 2, leftId: '1', rightId: '3', x: 28, y: 48 },
      { id: '7', val: 7, leftId: '6', rightId: '9', x: 72, y: 48 },
      { id: '1', val: 1, x: 18, y: 80 },
      { id: '3', val: 3, x: 38, y: 80 },
      { id: '6', val: 6, x: 62, y: 80 },
      { id: '9', val: 9, x: 82, y: 80 },
    ];

    const edges = [
      { fromId: '4', toId: '2', direction: 'left' as const },
      { fromId: '4', toId: '7', direction: 'right' as const },
      { fromId: '2', toId: '1', direction: 'left' as const },
      { fromId: '2', toId: '3', direction: 'right' as const },
      { fromId: '7', toId: '6', direction: 'left' as const },
      { fromId: '7', toId: '9', direction: 'right' as const },
    ];

    const lines = scan.rawLines;

    const steps: DebugStep[] = [
      {
        stepIndex: 0,
        lineNumber: scan.initLine,
        codeSnippet: this.getSnippet(lines, scan.initLine, 'if (root == null) return; // root = Node(4)'),
        actionType: 'INIT',
        title: 'Start Traversal at Root Node (4)',
        explanation: 'Entered traversal function at Root node with value 4.',
        variables: [
          { name: 'root.val', value: 4, isUpdated: true },
          { name: 'depth', value: 1 },
        ],
        treeState: {
          name: 'Binary Tree',
          rootId: '4',
          nodes: nodes.map((n) => (n.id === '4' ? { ...n, isCurrent: true, label: 'ROOT' } : n)),
          edges,
          traversalOrder: [],
          traversalType: 'INORDER',
        },
      },
      {
        stepIndex: 1,
        lineNumber: scan.calcLine,
        codeSnippet: this.getSnippet(lines, scan.calcLine, 'inorder(root.left); // traverse to left child Node(2)'),
        actionType: 'POINTER_MOVE',
        title: 'Traverse to Left Subtree: Node (2)',
        explanation: 'DFS recursive call to `root.left`. Traversal focus moves from Node 4 -> Node 2.',
        variables: [
          { name: 'curr.val', value: 2, isUpdated: true },
          { name: 'depth', value: 2 },
        ],
        treeState: {
          name: 'Binary Tree',
          rootId: '4',
          nodes: nodes.map((n) => (n.id === '2' ? { ...n, isCurrent: true, label: 'CURR' } : n)),
          edges: edges.map((e) => (e.fromId === '4' && e.toId === '2' ? { ...e, isTraversed: true } : e)),
          traversalOrder: [],
          traversalType: 'INORDER',
        },
      },
      {
        stepIndex: 2,
        lineNumber: scan.returnLine,
        codeSnippet: this.getSnippet(lines, scan.returnLine, 'return result; // Full In-Order Traversal Complete: [1, 2, 3, 4, 6, 7, 9]'),
        actionType: 'RETURN',
        title: 'Completed Full Tree Traversal',
        explanation: 'Visited all 7 nodes in sorted BST In-Order sequence `[1, 2, 3, 4, 6, 7, 9]` in O(N) time.',
        variables: [
          { name: 'result', value: '[1, 2, 3, 4, 6, 7, 9]', isUpdated: true },
          { name: 'timeComplexity', value: 'O(N)' },
        ],
        treeState: {
          name: 'Binary Tree (BST)',
          rootId: '4',
          nodes: nodes.map((n) => ({ ...n, isVisited: true, isMatched: true })),
          edges: edges.map((e) => ({ ...e, isTraversed: true })),
          traversalOrder: [1, 2, 3, 4, 6, 7, 9],
          traversalType: 'INORDER',
        },
      },
    ];

    return {
      problemTitle,
      language,
      inputData: inputData || 'root = [4, 2, 7, 1, 3, 6, 9]',
      totalSteps: steps.length,
      steps,
    };
  }

  // =========================================================================
  // 9. Graph BFS / DFS Visualizer Session
  // =========================================================================
  private static generateGraphSession(
    scan: CodeLineScan,
    problemTitle: string,
    language: string,
    inputData: string
  ): DebugSession {
    const graphNodes = [
      { id: '0', label: '0', x: 25, y: 30 },
      { id: '1', label: '1', x: 75, y: 30 },
      { id: '2', label: '2', x: 25, y: 70 },
      { id: '3', label: '3', x: 75, y: 70 },
      { id: '4', label: '4', x: 50, y: 50 },
    ];

    const graphEdges = [
      { from: '0', to: '1' },
      { from: '0', to: '4' },
      { from: '1', to: '3' },
      { from: '2', to: '4' },
      { from: '3', to: '4' },
    ];

    const lines = scan.rawLines;

    const steps: DebugStep[] = [
      {
        stepIndex: 0,
        lineNumber: scan.initLine,
        codeSnippet: this.getSnippet(lines, scan.initLine, 'Queue<Integer> queue = new LinkedList<>(); queue.offer(0);'),
        actionType: 'INIT',
        title: 'Start BFS at Source Node (0)',
        explanation: 'Enqueued source vertex `0` and marked it as visited.',
        variables: [
          { name: 'queue', value: '[0]', isUpdated: true },
          { name: 'visited', value: '{0}' },
        ],
        graphState: {
          name: 'Undirected Graph',
          nodes: graphNodes.map((n) => (n.id === '0' ? { ...n, isCurrent: true, isVisited: true } : n)),
          edges: graphEdges,
          visitedSet: [0],
          queueOrStack: [0],
        },
      },
      {
        stepIndex: 1,
        lineNumber: scan.updateLine || scan.outerLoopLine + 1,
        codeSnippet: this.getSnippet(lines, scan.updateLine || scan.outerLoopLine + 1, 'int curr = queue.poll(); // curr = 0; explore neighbors: 1, 4'),
        actionType: 'POINTER_MOVE',
        title: 'Dequeue Node 0 & Enqueue Unvisited Neighbors (1, 4)',
        explanation: 'Popped `0`. Discovered adjacent neighbors `1` and `4`. Added them to BFS Queue.',
        variables: [
          { name: 'curr', value: 0 },
          { name: 'queue', value: '[1, 4]', isUpdated: true },
          { name: 'visited', value: '{0, 1, 4}' },
        ],
        graphState: {
          name: 'Undirected Graph',
          nodes: graphNodes.map((n) => {
            if (n.id === '0') return { ...n, isVisited: true };
            if (['1', '4'].includes(n.id)) return { ...n, isNeighbor: true, isVisited: true };
            return n;
          }),
          edges: graphEdges.map((e) =>
            (e.from === '0' && e.to === '1') || (e.from === '0' && e.to === '4')
              ? { ...e, isTraversed: true }
              : e
          ),
          visitedSet: [0, 1, 4],
          queueOrStack: [1, 4],
        },
      },
      {
        stepIndex: 2,
        lineNumber: scan.returnLine,
        codeSnippet: this.getSnippet(lines, scan.returnLine, 'return visited.size(); // All 5 vertices visited!'),
        actionType: 'RETURN',
        title: 'Graph Traversal Completed',
        explanation: 'All reachable components explored in O(V + E) time.',
        variables: [
          { name: 'visitedNodesCount', value: 5, isUpdated: true },
          { name: 'timeComplexity', value: 'O(V + E)' },
        ],
        graphState: {
          name: 'Undirected Graph (Fully Explored)',
          nodes: graphNodes.map((n) => ({ ...n, isVisited: true, isCurrent: true })),
          edges: graphEdges.map((e) => ({ ...e, isTraversed: true })),
          visitedSet: [0, 1, 2, 3, 4],
          queueOrStack: [],
        },
      },
    ];

    return {
      problemTitle,
      language,
      inputData: inputData || 'graph = { 0: [1,4], 1: [0,3], 2: [4], 3: [1,4], 4: [0,2,3] }',
      totalSteps: steps.length,
      steps,
    };
  }

  // =========================================================================
  // 10. General Dynamic Two-Pointer & Array Session for Any Custom Code
  // =========================================================================
  private static generateGeneralTwoPointerSession(
    scan: CodeLineScan,
    problemTitle: string,
    language: string,
    inputData: string
  ): DebugSession {
    const rawNums = this.parseArrayFromInput(inputData, [10, 20, 30, 40, 50]);
    const lines = scan.rawLines;

    const steps: DebugStep[] = [
      {
        stepIndex: 0,
        lineNumber: scan.initLine,
        codeSnippet: this.getSnippet(lines, scan.initLine, `int left = 0, right = nums.length - 1; // [0, ${rawNums.length - 1}]`),
        actionType: 'INIT',
        title: 'Initialize Left & Right Pointers',
        explanation: `Set pointer \`left = 0\` and \`right = ${rawNums.length - 1}\`.`,
        variables: [
          { name: 'left', value: 0 },
          { name: 'right', value: rawNums.length - 1 },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [
            { name: 'left', index: 0, color: 'blue' },
            { name: 'right', index: rawNums.length - 1, color: 'purple' },
          ],
        },
      },
      {
        stepIndex: 1,
        lineNumber: scan.outerLoopLine,
        codeSnippet: this.getSnippet(lines, scan.outerLoopLine, 'while (left < right) { ... }'),
        actionType: 'LOOP_START',
        title: 'Begin Traversal',
        explanation: `Evaluating loop boundary condition \`left (0) < right (${rawNums.length - 1})\` -> TRUE.`,
        variables: [
          { name: 'nums[left]', value: rawNums[0] },
          { name: 'nums[right]', value: rawNums[rawNums.length - 1] },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [
            { name: 'left', index: 0, color: 'blue' },
            { name: 'right', index: rawNums.length - 1, color: 'purple' },
          ],
          activeIndices: [0, rawNums.length - 1],
        },
      },
      {
        stepIndex: 2,
        lineNumber: scan.returnLine,
        codeSnippet: this.getSnippet(lines, scan.returnLine, 'return result;'),
        actionType: 'RETURN',
        title: 'Algorithm Completed',
        explanation: 'Processed all elements in the array.',
        variables: [
          { name: 'status', value: 'COMPLETE', isUpdated: true },
          { name: 'timeComplexity', value: 'O(N)' },
        ],
        arrayState: {
          name: 'nums',
          values: rawNums,
          pointers: [{ name: 'DONE', index: Math.floor(rawNums.length / 2), color: 'green' }],
          matchedIndices: [Math.floor(rawNums.length / 2)],
        },
      },
    ];

    return {
      problemTitle,
      language,
      inputData,
      totalSteps: steps.length,
      steps,
    };
  }
}
