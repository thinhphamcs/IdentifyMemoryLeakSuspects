(function () {
    'use strict';

    const form = document.getElementById('analyze-form');
    const submitBtn = document.getElementById('submit-btn');
    const statusPanel = document.getElementById('status-panel');
    const jobIdEl = document.getElementById('job-id');
    const statusBadge = document.getElementById('status-badge');
    const errorBlock = document.getElementById('error-block');
    const errorMessage = document.getElementById('error-message');

    const TERMINAL = new Set(['COMPLETE', 'FAILED']);
    let pollTimer = null;

    form.addEventListener('submit', async function (e) {
        e.preventDefault();
        const javacorePath = document.getElementById('javacorePath').value.trim();
        const heapDumpPath = document.getElementById('heapDumpPath').value.trim();

        submitBtn.disabled = true;
        clearInterval(pollTimer);
        hideStatus();

        try {
            const params = new URLSearchParams({ javacorePath, heapDumpPath });
            const res = await fetch('/api/jobs?' + params.toString(), { method: 'POST' });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                alert('Failed to start job: ' + (err.error || res.status));
                submitBtn.disabled = false;
                return;
            }
            const { jobId } = await res.json();
            showStatus(jobId, 'PENDING');
            pollTimer = setInterval(() => poll(jobId), 5000);
            poll(jobId);
        } catch (err) {
            alert('Network error: ' + err.message);
            submitBtn.disabled = false;
        }
    });

    async function poll(jobId) {
        try {
            const res = await fetch('/api/jobs/' + encodeURIComponent(jobId) + '/status');
            if (!res.ok) return;
            const data = await res.json();
            showStatus(jobId, data.status, data.errorMessage);
            if (TERMINAL.has(data.status)) {
                clearInterval(pollTimer);
                submitBtn.disabled = false;
            }
        } catch (_) {
            // network blip — keep polling
        }
    }

    function showStatus(jobId, status, errMsg) {
        jobIdEl.textContent = jobId;
        statusBadge.textContent = status;
        statusBadge.className = 'badge badge-' + status;

        if (status === 'FAILED' && errMsg) {
            errorMessage.textContent = errMsg;
            errorBlock.classList.remove('hidden');
        } else {
            errorBlock.classList.add('hidden');
        }

        statusPanel.classList.remove('hidden');
    }

    function hideStatus() {
        statusPanel.classList.add('hidden');
        errorBlock.classList.add('hidden');
    }
})();
