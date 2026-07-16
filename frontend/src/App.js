import React, { useState } from 'react';
import './styles/App.css';
import CreateContactTab from './components/CreateContactTab';
import UpdateContactTab from './components/UpdateContactTab';
import SearchContactTab from './components/SearchContactTab';
import ViewContactTab from './components/ViewContactTab';
import DeleteContactTab from './components/DeleteContactTab';
import UploadContactTab from './components/UploadContactTab';

const TABS = [
  { id: 'view', label: 'View' },
  { id: 'create', label: 'Create' },
  { id: 'update', label: 'Update' },
  { id: 'search', label: 'Search' },
  { id: 'delete', label: 'Delete' },
  { id: 'upload', label: 'Upload' },
];

function App() {
  const [activeTab, setActiveTab] = useState('view');

  const renderTab = () => {
    switch (activeTab) {
      case 'create':
        return <CreateContactTab />;
      case 'update':
        return <UpdateContactTab />;
      case 'search':
        return <SearchContactTab />;
      case 'delete':
        return <DeleteContactTab />;
      case 'upload':
        return <UploadContactTab />;
      default:
        return <ViewContactTab />;
    }
  };

  return (
    <div className="App">
      <header className="app-header">
        <h1>Contact Management System</h1>
        <p>Manage contacts with create, update, and delete operations</p>
      </header>

      <main className="app-main">
        <nav className="tab-nav">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              type="button"
              className={`tab-button ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </nav>

        <section className="tab-content">{renderTab()}</section>
      </main>
    </div>
  );
}

export default App;
