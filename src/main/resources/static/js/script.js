let db;

const dbRequest = indexedDB.open('EZ_SCPZ_DB', 1);

dbRequest.onupgradeneeded = event => {
    db = event.target.result;
    const store = db.createObjectStore('presets', { keyPath: 'id', autoIncrement: true });
    store.createIndex('username', 'username', { unique: false });
};

dbRequest.onsuccess = event => {
    db = event.target.result;
    loadPresets();
};

dbRequest.onerror = event => {
    console.error('IndexedDB error:', event.target.error);
};

// Load presets from IndexedDB
const loadPresets = () => {
    const presetList = document.getElementById('presetList');
    const presetSelect = document.getElementById('presetSelect');
    presetList.innerHTML = '';
    presetSelect.innerHTML = '<option value="">Select a preset</option>';

    const transaction = db.transaction('presets', 'readonly');
    const store = transaction.objectStore('presets');
    const request = store.getAll();

    request.onsuccess = () => {
        const presets = request.result;
        presets.forEach(preset => {
            // Populate the list
            const li = document.createElement('li');
            li.textContent = `ID: ${preset.id}, Username: ${preset.username}, Destination IP: ${preset.destinationIp}`;
            li.setAttribute('data-id', preset.id);

            // Add edit and delete buttons to the list item
            const editButton = document.createElement('button');
            editButton.textContent = 'Edit';
            editButton.addEventListener('click', () => editPreset(preset.id));
            li.appendChild(editButton);

            const deleteButton = document.createElement('button');
            deleteButton.textContent = 'Delete';
            deleteButton.addEventListener('click', () => deletePreset(preset.id));
            li.appendChild(deleteButton);

            presetList.appendChild(li);

            // Populate the dropdown
            const option = document.createElement('option');
            option.value = preset.id;
            option.textContent = `${preset.username} - ${preset.destinationIp}`;
            presetSelect.appendChild(option);
        });
    };

    request.onerror = event => {
        console.error('Error loading presets:', event.target.error);
    };
};

// Save preset data to IndexedDB on form submission
document.getElementById('presetForm').addEventListener('submit', function(e) {
    e.preventDefault();

    const id = document.getElementById('presetId').value ? parseInt(document.getElementById('presetId').value) : undefined;
    const username = document.getElementById('username').value;
    const destinationIp = document.getElementById('destinationIp').value;
    const destinationDirectory = document.getElementById('destinationDirectory').value;
    const port = parseInt(document.getElementById('port').value);

    const preset = {
        username,
        destinationIp,
        destinationDirectory,
        port
    };

    if (id !== undefined) {
        preset.id = id;
    }

    console.log('Saving preset:', preset);

    const transaction = db.transaction('presets', 'readwrite');
    const store = transaction.objectStore('presets');

    const request = id ? store.put(preset) : store.add(preset); // Auto-increment handles new keys
    request.onsuccess = () => {
        alert('Preset saved successfully.');
        loadPresets();
        document.getElementById('presetForm').reset();
    };

    request.onerror = event => {
        console.error('Error saving preset:', event.target.error);
    };
});

// Edit a preset
const editPreset = id => {
    const transaction = db.transaction('presets', 'readonly');
    const store = transaction.objectStore('presets');
    const request = store.get(id);

    request.onsuccess = () => {
        const preset = request.result;
        if (preset) {
            document.getElementById('presetId').value = preset.id;
            document.getElementById('username').value = preset.username;
            document.getElementById('destinationIp').value = preset.destinationIp;
            document.getElementById('destinationDirectory').value = preset.destinationDirectory;
            document.getElementById('port').value = preset.port;
        }
    };

    request.onerror = event => {
        console.error('Error loading preset:', event.target.error);
    };
};

// Delete a preset
const deletePreset = id => {
    const transaction = db.transaction('presets', 'readwrite');
    const store = transaction.objectStore('presets');
    const request = store.delete(id);

    request.onsuccess = () => {
        alert('Preset deleted successfully.');
        loadPresets();
    };

    request.onerror = event => {
        console.error('Error deleting preset:', event.target.error);
    };
};

// Handle file upload using a selected preset
document.getElementById('uploadForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const file = document.getElementById('file').files[0];
    const presetId = document.getElementById('presetSelect').value;
    const password = document.getElementById('uploadPassword').value;

    if (!presetId) {
        alert('Please select a preset.');
        return;
    }

    const transaction = db.transaction('presets', 'readonly');
    const store = transaction.objectStore('presets');
    const request = store.get(parseInt(presetId));

    request.onsuccess = () => {
        const preset = request.result;

        if (preset) {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('username', preset.username);
            formData.append('password', password);
            formData.append('host', preset.destinationIp);
            formData.append('port', preset.port);
            formData.append('destination', preset.destinationDirectory);

            fetch('/api/scp/upload', {
                method: 'POST',
                body: formData
            })
            .then(response => response.text())
            .then(data => {
                alert(data);
            })
            .catch(error => {
                console.error('Error uploading file:', error);
                alert('Error uploading file: ' + error.message);
            });
        } else {
            alert('Preset not found.');
        }
    };

    request.onerror = event => {
        console.error('Error loading preset for upload:', event.target.error);
    };
});

// Load presets on page load
document.addEventListener('DOMContentLoaded', () => {
    if (db) {
        loadPresets();
    } else {
        dbRequest.onsuccess = event => {
            db = event.target.result;
            loadPresets();
        };
    }
});
