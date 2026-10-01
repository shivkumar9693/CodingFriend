import React from 'react';
import { Link } from 'react-router-dom';
import { FileQuestion, ArrowLeft } from 'lucide-react';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="page-container not-found-page">
      <div className="not-found-card">
        <div className="not-found-icon">
          <FileQuestion size={48} />
        </div>
        <h1 className="not-found-title">404 - Page Not Found</h1>
        <p className="not-found-desc">
          The page you are looking for might have been removed, had its name changed, or is temporarily unavailable.
        </p>
        <Link to="/problems" className="btn btn-primary">
          <ArrowLeft size={16} /> Return to Problems Catalog
        </Link>
      </div>
    </div>
  );
};
