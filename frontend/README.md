# Frontend - Contact Management System

React-based frontend application for the Contact Management System.

## Technology Stack

- React 18+
- Modern React Hooks
- Context API for state management
- Axios for API calls
- CSS/SCSS for styling

## Project Structure

```
frontend/
├── public/              # Static assets
├── src/
│   ├── components/      # Reusable React components
│   ├── services/        # API service layer
│   ├── utils/           # Utility functions
│   ├── hooks/           # Custom React hooks
│   ├── context/         # React Context providers
│   ├── assets/          # Images, fonts, etc.
│   ├── styles/          # Global styles, themes
│   └── App.js           # Main application component
└── package.json
```

## Getting Started

### Prerequisites
- Node.js 16+ and npm

### Installation

```bash
# Install dependencies
npm install
```

### Development

```bash
# Start development server
npm start
```

The application will run on http://localhost:3000

### Build for Production

```bash
# Create production build
npm run build
```

## Environment Variables

Create a `.env` file in the frontend directory:

```
REACT_APP_API_BASE_URL=http://localhost:8080
```

## Features

- Contact list view
- Add new contact
- Edit existing contact
- Delete contact
- Search and filter contacts
- Responsive design
