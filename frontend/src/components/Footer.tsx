import React from 'react';
import { Code2, Heart } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="footer-root">
      <div className="footer-container">
        <div className="footer-brand">
          <div className="footer-logo">
            <Code2 size={20} className="footer-icon" />
            <span className="footer-title">CodingFriend</span>
          </div>
          <p className="footer-tagline">
            Master Java algorithms with SocraBuddy Socratic hints.
          </p>
        </div>
        <div className="footer-bottom">
          <p className="footer-copyright">
            &copy; {new Date().getFullYear()} CodingFriend. Powered by SocraBuddy AI. Built with{' '}
            <Heart size={14} className="heart-icon" /> for passionate developers.
          </p>
        </div>
      </div>
    </footer>
  );
};
