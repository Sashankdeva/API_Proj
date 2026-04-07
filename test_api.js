// HTTP test of the actual register route
const http = require('http');

const payload = JSON.stringify({
    username: 'httptest_user',
    email: 'httptest@test.com',
    password: 'testpass123'
});

const options = {
    hostname: 'localhost',
    port: 5500,
    path: '/api/auth/register',
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload)
    }
};

const req = http.request(options, (res) => {
    let body = '';
    res.on('data', chunk => body += chunk);
    res.on('end', () => {
        console.log('Status:', res.statusCode);
        console.log('Response:', body);
    });
});

req.on('error', (e) => console.error('Request failed:', e.message));
req.write(payload);
req.end();
