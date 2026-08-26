const BhuSanketOffline = (() => {
    const databaseName = 'bhusanket-offline';
    const storeName = 'pending-reports';
    let database;

    function openDatabase() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(databaseName, 1);
            request.onupgradeneeded = () => request.result.createObjectStore(storeName, { keyPath: 'offlineId', autoIncrement: true });
            request.onsuccess = () => { database = request.result; resolve(database); };
            request.onerror = () => reject(request.error);
        });
    }

    async function put(report) {
        const db = database || await openDatabase();
        return new Promise((resolve, reject) => {
            const request = db.transaction(storeName, 'readwrite').objectStore(storeName).add({ ...report, queuedAt: new Date().toISOString() });
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async function all() {
        const db = database || await openDatabase();
        return new Promise((resolve, reject) => {
            const request = db.transaction(storeName, 'readonly').objectStore(storeName).getAll();
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async function remove(offlineId) {
        const db = database || await openDatabase();
        return new Promise((resolve, reject) => {
            const request = db.transaction(storeName, 'readwrite').objectStore(storeName).delete(offlineId);
            request.onsuccess = resolve;
            request.onerror = () => reject(request.error);
        });
    }

    async function sync(sendReport) {
        const pending = await all();
        let synced = 0;
        for (const report of pending) {
            const { offlineId, queuedAt, ...payload } = report;
            try {
                await sendReport(payload);
                await remove(offlineId);
                synced += 1;
            } catch (error) {
                break;
            }
        }
        if (synced) document.dispatchEvent(new CustomEvent('bhusanket:reports-synced', { detail: { count: synced } }));
        return synced;
    }

    function updateStatus() {
        const feed = document.querySelector('.nav-feed');
        if (!feed) return;
        feed.innerHTML = navigator.onLine
            ? '<i class="dot green"></i> Online · sync ready'
            : '<i class="dot amber"></i> Offline · reports will queue';
        feed.classList.toggle('offline-feed', !navigator.onLine);
    }

    async function init() {
        try { await openDatabase(); } catch (error) { console.warn('Offline report storage unavailable', error); }
        if ('serviceWorker' in navigator) navigator.serviceWorker.register('./sw.js').catch(error => console.warn('Service worker unavailable', error));
        updateStatus();
        window.addEventListener('online', updateStatus);
        window.addEventListener('offline', updateStatus);
    }

    return { init, put, all, sync, updateStatus };
})();
