import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  getErrorMessage,
  listContactDataFiles,
  uploadContactDataFile,
} from '../services/contactApi';

function UploadContactTab() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [selectedExtractFile, setSelectedExtractFile] = useState('');
  const [showExtractSelector, setShowExtractSelector] = useState(false);
  const [loadingFiles, setLoadingFiles] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState(null);
  const fileInputRef = useRef(null);

  const loadUploadedFiles = useCallback(async () => {
    setLoadingFiles(true);
    try {
      const files = await listContactDataFiles();
      setUploadedFiles(files);
    } catch (error) {
      setMessage({ type: 'error', text: getErrorMessage(error) });
    } finally {
      setLoadingFiles(false);
    }
  }, []);

  useEffect(() => {
    loadUploadedFiles();
  }, [loadUploadedFiles]);

  const handleFileChange = (event) => {
    setSelectedFile(event.target.files[0] || null);
    setMessage(null);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setMessage({ type: 'error', text: 'Please choose a text file to upload.' });
      return;
    }

    setUploading(true);
    setMessage(null);

    try {
      const result = await uploadContactDataFile(selectedFile);
      setMessage({ type: 'success', text: `File uploaded as ${result.filename}` });
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      await loadUploadedFiles();
    } catch (error) {
      setMessage({ type: 'error', text: getErrorMessage(error) });
    } finally {
      setUploading(false);
    }
  };

  const handleExtractClick = () => {
    setShowExtractSelector(true);
    setMessage(null);
  };

  const handleExtractFileChange = (event) => {
    setSelectedExtractFile(event.target.value);
  };

  return (
    <div className="tab-panel">
      <h2>Upload Contact Data</h2>
      <p className="tab-description">
        Upload a raw text file with unstructured contact details. Files are saved to the
        data/contactdata folder with a unique date-time filename.
      </p>

      {message && <div className={`alert alert-${message.type}`}>{message.text}</div>}

      <div className="upload-section">
        <label className="file-picker-label">
          Choose text file
          <input
            ref={fileInputRef}
            type="file"
            accept=".txt,text/plain"
            onChange={handleFileChange}
            disabled={uploading}
          />
        </label>

        {selectedFile && <p className="selected-file-name">Selected: {selectedFile.name}</p>}

        <button
          type="button"
          className="btn btn-primary"
          onClick={handleUpload}
          disabled={uploading || !selectedFile}
        >
          {uploading ? 'Uploading...' : 'Upload File'}
        </button>
      </div>

      <div className="uploaded-files-section">
        <h3>Uploaded Files</h3>
        {loadingFiles ? (
          <p className="status-text">Loading uploaded files...</p>
        ) : uploadedFiles.length === 0 ? (
          <p className="status-text">No files uploaded yet.</p>
        ) : (
          <ul className="uploaded-files-list">
            {uploadedFiles.map((filename) => (
              <li key={filename}>{filename}</li>
            ))}
          </ul>
        )}
      </div>

      <div className="extract-section">
        <button
          type="button"
          className="btn btn-secondary"
          onClick={handleExtractClick}
          disabled={loadingFiles || uploadedFiles.length === 0}
        >
          Extract Contact
        </button>

        {showExtractSelector && (
          <div className="extract-selector">
            <label className="selector-label">
              Select file for extraction
              <select value={selectedExtractFile} onChange={handleExtractFileChange}>
                <option value="">Choose an uploaded file</option>
                {uploadedFiles.map((filename) => (
                  <option key={filename} value={filename}>
                    {filename}
                  </option>
                ))}
              </select>
            </label>

            {selectedExtractFile && (
              <p className="status-text">Selected for extraction: {selectedExtractFile}</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default UploadContactTab;
