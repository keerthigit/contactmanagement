import React, { useState } from 'react';
import './styles/App.css';
import CreateContactTab from './components/CreateContactTab';
import UpdateContactTab from './components/UpdateContactTab';
import DeleteContactTab from './components/DeleteContactTab';

const TABS = [
  { id: 'create', label: 'Create' },
  { id: 'update', label: 'Update' },
  { id: 'delete', label: 'Delete' },
];

function App() {
  const [activeTab, setActiveTab] = useState('create');

  const renderTab = () => {
    switch (activeTab) {
      case 'update':
        return <UpdateContactTab />;
      case 'delete':
        return <DeleteContactTab />;
      default:
        return <CreateContactTab />;
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
