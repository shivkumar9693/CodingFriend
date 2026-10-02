import React from 'react';
import { TreeVisualState, GraphVisualState } from '../../types/debugger';
import { GitBranch, Share2 } from 'lucide-react';

interface TreeGraphVisualizerProps {
  treeState?: TreeVisualState;
  graphState?: GraphVisualState;
}

export const TreeGraphVisualizer: React.FC<TreeGraphVisualizerProps> = ({
  treeState,
  graphState,
}) => {
  if (treeState) {
    return (
      <div className="tree-visualizer-container" data-testid="tree-visualizer-container">
        <div className="canvas-title-strip">
          <span className="canvas-title">
            <GitBranch size={14} /> Tree Traversal: {treeState.name}
          </span>
          {treeState.traversalType && (
            <span className="tree-type-badge">{treeState.traversalType}</span>
          )}
        </div>

        {/* SVG Tree Graph Canvas */}
        <div className="tree-svg-wrapper">
          <svg viewBox="0 0 500 240" className="tree-svg-canvas">
            <defs>
              <linearGradient id="edgeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#3b82f6" stopOpacity="0.8" />
                <stop offset="100%" stopColor="#eab308" stopOpacity="0.9" />
              </linearGradient>
              <filter id="nodeGlow" x="-20%" y="-20%" width="140%" height="140%">
                <feGaussianBlur stdDeviation="3" result="blur" />
                <feComposite in="SourceGraphic" in2="blur" operator="over" />
              </filter>
            </defs>

            {/* Connecting Branches / Edges */}
            {treeState.edges.map((edge, idx) => {
              const fromNode = treeState.nodes.find((n) => n.id === edge.fromId);
              const toNode = treeState.nodes.find((n) => n.id === edge.toId);
              if (!fromNode || !toNode) return null;

              const x1 = (fromNode.x ?? 50) * 5;
              const y1 = (fromNode.y ?? 20) * 2.4;
              const x2 = (toNode.x ?? 50) * 5;
              const y2 = (toNode.y ?? 20) * 2.4;

              return (
                <g key={idx}>
                  <line
                    x1={x1}
                    y1={y1}
                    x2={x2}
                    y2={y2}
                    className={`tree-branch-line ${edge.isTraversed ? 'traversed' : ''}`}
                    stroke={edge.isTraversed ? '#eab308' : 'var(--border-color)'}
                    strokeWidth={edge.isTraversed ? 3 : 2}
                    strokeDasharray={edge.isTraversed ? '4 2' : undefined}
                  />
                </g>
              );
            })}

            {/* Tree Nodes */}
            {treeState.nodes.map((node) => {
              const cx = (node.x ?? 50) * 5;
              const cy = (node.y ?? 20) * 2.4;
              const isCurr = node.isCurrent;
              const isVisited = node.isVisited;
              const isMatched = node.isMatched;

              return (
                <g
                  key={node.id}
                  className={`tree-node-group ${isCurr ? 'current' : ''} ${
                    isVisited ? 'visited' : ''
                  }`}
                >
                  {/* Outer Glow Halo for Active Node */}
                  {isCurr && (
                    <circle
                      cx={cx}
                      cy={cy}
                      r={24}
                      className="node-pulse-halo"
                      fill="none"
                      stroke="#eab308"
                      strokeWidth="2"
                    />
                  )}

                  {/* Main Node Circle */}
                  <circle
                    cx={cx}
                    cy={cy}
                    r={18}
                    className={`tree-node-circle ${
                      isMatched
                        ? 'matched'
                        : isCurr
                        ? 'current'
                        : isVisited
                        ? 'visited'
                        : 'idle'
                    }`}
                  />

                  {/* Node Value */}
                  <text
                    x={cx}
                    y={cy + 5}
                    textAnchor="middle"
                    className="tree-node-text"
                  >
                    {node.val}
                  </text>

                  {/* Pointer Label Badge */}
                  {node.label && (
                    <g transform={`translate(${cx}, ${cy - 24})`}>
                      <rect
                        x="-24"
                        y="-12"
                        width="48"
                        height="14"
                        rx="4"
                        className="node-badge-bg"
                      />
                      <text
                        x="0"
                        y="-2"
                        textAnchor="middle"
                        className="node-badge-text"
                      >
                        {node.label}
                      </text>
                    </g>
                  )}
                </g>
              );
            })}
          </svg>
        </div>

        {/* Traversal Order Strip */}
        {treeState.traversalOrder && treeState.traversalOrder.length > 0 && (
          <div className="traversal-order-strip">
            <span className="traversal-label">Visited Sequence:</span>
            <div className="traversal-nodes-row">
              {treeState.traversalOrder.map((val, idx) => (
                <div key={idx} className="traversal-node-chip">
                  <span>{String(val)}</span>
                  {idx < (treeState.traversalOrder?.length || 0) - 1 && (
                    <span className="traversal-arrow">→</span>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    );
  }

  if (graphState) {
    return (
      <div className="graph-visualizer-container" data-testid="graph-visualizer-container">
        <div className="canvas-title-strip">
          <span className="canvas-title">
            <Share2 size={14} /> Graph Adjacency Canvas: {graphState.name}
          </span>
        </div>

        {/* SVG Graph Canvas */}
        <div className="tree-svg-wrapper">
          <svg viewBox="0 0 500 240" className="tree-svg-canvas">
            {/* Graph Edges */}
            {graphState.edges.map((edge, idx) => {
              const fromNode = graphState.nodes.find((n) => n.id === edge.from);
              const toNode = graphState.nodes.find((n) => n.id === edge.to);
              if (!fromNode || !toNode) return null;

              const x1 = fromNode.x * 5;
              const y1 = fromNode.y * 2.4;
              const x2 = toNode.x * 5;
              const y2 = toNode.y * 2.4;

              return (
                <line
                  key={idx}
                  x1={x1}
                  y1={y1}
                  x2={x2}
                  y2={y2}
                  className={`tree-branch-line ${edge.isTraversed ? 'traversed' : ''}`}
                  stroke={edge.isTraversed ? '#10b981' : 'var(--border-color)'}
                  strokeWidth={edge.isTraversed ? 3 : 2}
                />
              );
            })}

            {/* Graph Nodes */}
            {graphState.nodes.map((node) => {
              const cx = node.x * 5;
              const cy = node.y * 2.4;

              return (
                <g key={node.id} className="tree-node-group">
                  {node.isCurrent && (
                    <circle
                      cx={cx}
                      cy={cy}
                      r={24}
                      className="node-pulse-halo"
                      fill="none"
                      stroke="#eab308"
                      strokeWidth="2"
                    />
                  )}
                  <circle
                    cx={cx}
                    cy={cy}
                    r={18}
                    className={`tree-node-circle ${
                      node.isCurrent
                        ? 'current'
                        : node.isVisited
                        ? 'visited'
                        : 'idle'
                    }`}
                  />
                  <text
                    x={cx}
                    y={cy + 5}
                    textAnchor="middle"
                    className="tree-node-text"
                  >
                    {node.label}
                  </text>
                </g>
              );
            })}
          </svg>
        </div>

        {/* Visited Set & Queue/Stack Deck */}
        <div className="graph-state-deck">
          {graphState.visitedSet && (
            <div className="graph-state-pill">
              <span className="state-label">Visited Set:</span>
              <span className="state-values">
                {`{ ${graphState.visitedSet.join(', ')} }`}
              </span>
            </div>
          )}
          {graphState.queueOrStack && (
            <div className="graph-state-pill">
              <span className="state-label">BFS Queue / Stack:</span>
              <span className="state-values">
                {`[ ${graphState.queueOrStack.join(', ')} ]`}
              </span>
            </div>
          )}
        </div>
      </div>
    );
  }

  return null;
};
