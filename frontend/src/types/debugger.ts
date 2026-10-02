export type DebugActionType =
  | 'INIT'
  | 'LOOP_START'
  | 'POINTER_MOVE'
  | 'CALCULATION'
  | 'MAP_CHECK'
  | 'MAP_INSERT'
  | 'ARRAY_ACCESS'
  | 'ARRAY_SWAP'
  | 'BRANCH_DECISION'
  | 'MATCH_FOUND'
  | 'RETURN';

export interface ArrayPointer {
  name: string;
  index: number;
  color?: 'yellow' | 'blue' | 'green' | 'red' | 'purple';
  label?: string;
}

export interface ArrayVisualState {
  name: string;
  values: (number | string)[];
  pointers: ArrayPointer[];
  highlightIndices?: number[];
  activeIndices?: number[];
  matchedIndices?: number[];
}

export interface MapEntry {
  key: string | number;
  value: string | number | boolean;
  isNew?: boolean;
  isHit?: boolean;
}

export interface MapVisualState {
  name: string;
  entries: MapEntry[];
  lookupKey?: string | number;
  lookupResult?: 'HIT' | 'MISS' | null;
}

export interface TreeNodeItem {
  id: string;
  val: string | number;
  leftId?: string;
  rightId?: string;
  x?: number; // relative percentage 0-100
  y?: number; // relative percentage 0-100
  isCurrent?: boolean;
  isVisited?: boolean;
  isMatched?: boolean;
  label?: string;
}

export interface TreeEdgeItem {
  fromId: string;
  toId: string;
  isTraversed?: boolean;
  direction?: 'left' | 'right';
}

export interface TreeVisualState {
  name: string;
  rootId: string;
  nodes: TreeNodeItem[];
  edges: TreeEdgeItem[];
  traversalOrder?: (string | number)[];
  traversalType?: 'INORDER' | 'PREORDER' | 'POSTORDER' | 'BFS_LEVEL_ORDER' | 'DFS';
}

export interface GraphNodeItem {
  id: string;
  label: string | number;
  isCurrent?: boolean;
  isVisited?: boolean;
  isNeighbor?: boolean;
  x: number;
  y: number;
}

export interface GraphEdgeItem {
  from: string;
  to: string;
  weight?: number;
  isTraversed?: boolean;
}

export interface GraphVisualState {
  name: string;
  nodes: GraphNodeItem[];
  edges: GraphEdgeItem[];
  visitedSet?: (string | number)[];
  queueOrStack?: (string | number)[];
}

export interface VariableWatchItem {
  name: string;
  value: string | number | boolean | null;
  type?: string;
  isUpdated?: boolean;
}

export interface DebugStep {
  stepIndex: number;
  lineNumber: number;
  codeSnippet: string;
  actionType: DebugActionType;
  title: string;
  explanation: string;
  variables: VariableWatchItem[];
  arrayState?: ArrayVisualState;
  mapState?: MapVisualState;
  treeState?: TreeVisualState;
  graphState?: GraphVisualState;
  socraticQuestion?: {
    question: string;
    options: { text: string; isCorrect: boolean; feedback: string }[];
    hint?: string;
  };
}

export interface DebugSession {
  problemTitle: string;
  language: string;
  inputData: string;
  totalSteps: number;
  steps: DebugStep[];
}
