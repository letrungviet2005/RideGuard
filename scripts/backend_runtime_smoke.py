"""Real HTTP + local in-memory SMTP smoke test. Requires a dedicated *_test DB and CLI admin.

Uses only Python standard library. Never prints credentials, HTTP bodies or reset messages.
Starts/stops only its own Java process and loopback SMTP sink; does not manage PostgreSQL.
"""
import email
import json
import os
from pathlib import Path
import queue
import re
import secrets
import socket
import socketserver
import subprocess
import threading
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone


def run():
    env = os.environ.copy()
    if not re.fullmatch(r"jdbc:postgresql://[^/]+/[A-Za-z0-9_]+_test", env.get("DB_URL", "")):
        raise RuntimeError("A dedicated database ending in _test is required")
    admin_email = env.pop("RG_ADMIN_EMAIL")
    admin_password = env.pop("RG_ADMIN_PASSWORD")
    for key in ("DB_USER", "DB_PASSWORD", "JWT_SIGNING_KEY", "CURSOR_SIGNING_KEY", "JAVA_HOME"):
        if not env.get(key):
            raise RuntimeError("Missing required test environment variable: " + key)
    mailbox = queue.Queue()

    class SMTP(socketserver.StreamRequestHandler):
        def handle(self):
            self.wfile.write(b"220 localhost RideGuard test sink\r\n")
            while True:
                line = self.rfile.readline(65537)
                if not line:
                    return
                command = line.split(b" ", 1)[0].strip().upper()
                if command in (b"EHLO", b"HELO"):
                    self.wfile.write(b"250-localhost\r\n250 SIZE 65536\r\n")
                elif command == b"DATA":
                    self.wfile.write(b"354 End with dot\r\n")
                    data = bytearray()
                    while True:
                        part = self.rfile.readline(65537)
                        if not part or part == b".\r\n":
                            break
                        data.extend(part[1:] if part.startswith(b"..") else part)
                        if len(data) > 65536:
                            return
                    mailbox.put(bytes(data))
                    self.wfile.write(b"250 Accepted in test memory\r\n")
                elif command == b"QUIT":
                    self.wfile.write(b"221 Bye\r\n")
                    return
                else:
                    self.wfile.write(b"250 OK\r\n")

    sink = socketserver.ThreadingTCPServer(("127.0.0.1", 0), SMTP)
    sink.daemon_threads = True
    threading.Thread(target=sink.serve_forever, daemon=True).start()
    with socket.socket() as port_socket:
        port_socket.bind(("127.0.0.1", 0))
        port = port_socket.getsockname()[1]
    env.update(PORT=str(port), SMTP_HOST="127.0.0.1", SMTP_PORT=str(sink.server_address[1]),
               SMTP_AUTH="false", SMTP_STARTTLS="false", DEBUG="false",
               ADMIN_ALLOWED_ORIGINS="https://admin.example.invalid", COOKIE_SECURE="true",
               RATE_LIMIT_PER_MINUTE="10000", SPRING_PROFILES_ACTIVE="")
    base = f"http://127.0.0.1:{port}"
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    results = []
    process = None
    log = None
    local = Path('.local')
    local.mkdir(exist_ok=True)

    def check(condition, label):
        if not condition:
            raise AssertionError(label)
        results.append({"assertion": label, "result": "PASS"})

    def call(method, path, body=None, token=None, headers=None, expected=200):
        h = dict(headers or {})
        if token:
            h['Authorization'] = 'Bearer ' + token
        data = None if body is None else json.dumps(body).encode()
        if data is not None:
            h['Content-Type'] = 'application/json'
        request = urllib.request.Request(base + path, data=data, headers=h, method=method)
        try:
            response = opener.open(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            status, rh, raw = response.status, response.headers, response.read()
        if status != expected:
            raise AssertionError(f"{method} {path.split('?')[0]} expected {expected}, got {status}")
        check(bool(rh.get('X-Request-ID')), 'request_id header')
        results.append({"method": method, "path": path.split('?')[0], "status": status})
        return json.loads(raw) if raw else None, rh

    mobile = {'X-Client-Type': 'mobile'}
    web = {'X-Client-Type': 'admin_web', 'Origin': 'https://admin.example.invalid', 'X-CSRF-Protection': '1'}

    def start():
        nonlocal process, log
        log = (local / 'backend-runtime.log').open('ab')
        process = subprocess.Popen([str(Path(env['JAVA_HOME']) / 'bin/java.exe'), '-jar',
                                    'apps/backend/target/backend-0.1.0-SNAPSHOT.jar'],
                                   env=env, stdout=log, stderr=subprocess.STDOUT)
        for _ in range(120):
            if process.poll() is not None:
                raise RuntimeError('Backend exited during startup; inspect sanitized server log')
            try:
                with opener.open(base + '/actuator/health/readiness', timeout=1) as r:
                    if r.status == 200:
                        return
            except (OSError, urllib.error.URLError):
                pass
            time.sleep(.5)
        raise RuntimeError('Backend readiness timeout')

    def stop():
        nonlocal process, log
        if process and process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=10)
        if log:
            log.close()

    try:
        start()
        for p in ('/actuator/health', '/actuator/health/liveness', '/actuator/health/readiness'):
            b, _ = call('GET', p)
            check(b['status'] == 'UP' and 'details' not in b, 'minimal healthy probe')
        prefix = '/api/v1'
        user_email = str(uuid.uuid4()) + '@test.invalid'
        password = secrets.token_urlsafe(32)
        user, _ = call('POST', prefix + '/auth/register', dict(email=user_email, full_name='Runtime User', password=password), expected=201)

        def login(address, pwd, headers=mobile, expected=200):
            return call('POST', prefix + '/auth/login', dict(email=address, password=pwd), headers=headers, expected=expected)

        u, _ = login(user_email, password)
        a, _ = login(admin_email, admin_password)
        ut, at = u['access_token'], a['access_token']
        call('GET', prefix + '/users/me', token=ut)
        call('PATCH', prefix + '/users/me', {'full_name': 'Runtime Updated'}, token=ut)
        sessions, _ = call('GET', prefix + '/users/me/sessions', token=ut)
        check(len(sessions['items']) == 1, 'one logical session')
        call('GET', prefix + '/admin/users', token=ut, expected=403)
        call('GET', prefix + '/admin/users', token=at)
        serial = 'RG-' + str(uuid.uuid4()).upper()
        device, _ = call('POST', prefix + '/admin/devices', dict(serial_number=serial, model='REST smoke'), token=at, expected=201)
        did = device['id']
        call('GET', prefix + '/admin/devices', token=at)
        call('PATCH', prefix + '/admin/devices/' + did + '/status', {'status': 'ACTIVE'}, token=at)
        key = {'Idempotency-Key': str(uuid.uuid4())}
        bound, _ = call('POST', prefix + '/devices/bind', {'serial_number': serial}, token=ut, headers=key, expected=201)
        replay, _ = call('POST', prefix + '/devices/bind', {'serial_number': serial}, token=ut, headers=key)
        check(bound == replay, 'idempotent bind snapshot')
        bid = bound['binding_id']
        dp = prefix + '/devices/' + did
        call('GET', prefix + '/devices', token=ut)
        call('PATCH', dp, {'nickname': 'Test mirror'}, token=ut)
        config, _ = call('GET', dp + '/config', token=ut)
        check(config['desired_version'] == 1 and config['reported_applied_version'] is None, 'initial config')
        config, _ = call('PATCH', dp + '/config', {'expected_version': 1, 'settings': {'vibration_enabled': False}}, token=ut)
        call('PATCH', dp + '/config', {'expected_version': 1, 'settings': {'vibration_enabled': True}}, token=ut, expected=409)
        for version in (2, 1):
            ack, _ = call('POST', dp + '/config/ack', {'binding_id': bid, 'reported_applied_version': version}, token=ut)
            check(ack['reported_applied_version'] == 2, 'monotonic REST ACK (simulated client)')
        now = datetime.now(timezone.utc) - timedelta(seconds=2)
        report = dict(binding_id=bid, observed_at=now.isoformat().replace('+00:00', 'Z'), firmware_version=None,
                      radar_status='UNKNOWN', imu_status='UNKNOWN', display_status='UNKNOWN', vibration_status='UNKNOWN', battery_percent=None)
        b, _ = call('POST', dp + '/reports', report, token=ut)
        check(b['accepted'], 'new REST report accepted')
        report['observed_at'] = (now-timedelta(seconds=1)).isoformat().replace('+00:00', 'Z')
        b, _ = call('POST', dp + '/reports', report, token=ut)
        check(not b['accepted'], 'stale report rejected')
        b, _ = call('GET', dp, token=ut)
        check(datetime.fromisoformat(b['latest_report']['observed_at'].replace('Z', '+00:00')) == now, 'stale report did not replace latest')
        fb, _ = call('POST', prefix + '/feedbacks', {'device_id': did, 'category': 'SUGGESTION', 'content': 'REST smoke only'}, token=ut,
                     headers={'Idempotency-Key': str(uuid.uuid4())}, expected=201)
        fp = prefix + '/feedbacks/' + fb['id']
        call('GET', prefix + '/feedbacks', token=ut)
        call('GET', prefix + '/admin/feedbacks', token=at)
        call('PATCH', prefix + '/admin/feedbacks/' + fb['id'], {'status': 'RESOLVED', 'admin_response': 'Smoke reply'}, token=at)
        call('DELETE', dp + '/binding?expected_binding_id=' + bid, token=ut, expected=204)
        for path in (dp, dp + '/config'):
            call('GET', path, token=ut, expected=404)
        call('POST', dp + '/config/ack', {'binding_id': bid, 'reported_applied_version': 2}, token=ut, expected=404)
        call('POST', dp + '/reports', report, token=ut, expected=404)
        call('GET', fp, token=ut)
        call('GET', prefix + '/device-bindings', token=ut)
        bound2, _ = call('POST', prefix + '/devices/bind', {'serial_number': serial}, token=ut,
                         headers={'Idempotency-Key': str(uuid.uuid4())}, expected=201)
        call('DELETE', dp + '/binding?expected_binding_id=' + bid, token=ut, expected=204)
        b, _ = call('GET', dp, token=ut)
        check(b['binding_id'] == bound2['binding_id'], 'stale unbind preserves new binding')
        w, wh = login(admin_email, admin_password, web)
        check('refresh_token' not in w, 'web response hides refresh token')
        cookie = wh['Set-Cookie']
        check('HttpOnly' in cookie and 'Secure' in cookie and 'SameSite=Lax' in cookie, 'secure cookie attributes')
        cookie = cookie.split(';', 1)[0]
        call('POST', prefix + '/auth/refresh', {}, headers={**web, 'Cookie': cookie, 'Origin': 'https://evil.invalid'}, expected=403)
        no_csrf = {k: v for k, v in web.items() if k != 'X-CSRF-Protection'}
        call('POST', prefix + '/auth/refresh', {}, headers={**no_csrf, 'Cookie': cookie}, expected=403)
        call('POST', prefix + '/auth/refresh', {}, headers={**mobile, 'Cookie': cookie}, expected=400)
        call('GET', prefix + '/users/me', headers={'Cookie': cookie}, expected=401)
        login(user_email, password, web, expected=403)
        _, preflight = call('OPTIONS', prefix + '/auth/login', headers={'Origin': web['Origin'], 'Access-Control-Request-Method': 'POST',
                            'Access-Control-Request-Headers': 'content-type,x-client-type,x-csrf-protection'})
        check(preflight.get('Access-Control-Allow-Origin') == web['Origin'], 'exact CORS origin')
        w2, h2 = call('POST', prefix + '/auth/refresh', {}, headers={**web, 'Cookie': cookie})
        check('refresh_token' not in w2, 'web rotation transport')
        _, logout_headers = call('POST', prefix + '/auth/logout', {}, headers={**web, 'Cookie': h2['Set-Cookie'].split(';', 1)[0]}, expected=204)
        check('Max-Age=0' in logout_headers['Set-Cookie'], 'web logout clears cookie')
        next_u, _ = call('POST', prefix + '/auth/refresh', {'refresh_token': u['refresh_token']}, headers=mobile)
        call('POST', prefix + '/auth/refresh', {'refresh_token': u['refresh_token']}, headers=mobile, expected=401)
        call('GET', prefix + '/users/me', token=next_u['access_token'], expected=401)
        u, _ = login(user_email, password)
        ut = u['access_token']
        call('PATCH', prefix + '/admin/users/' + user['id'] + '/status', {'status': 'LOCKED'}, token=at)
        call('GET', prefix + '/users/me', token=ut, expected=401)
        call('PATCH', prefix + '/admin/users/' + user['id'] + '/status', {'status': 'ACTIVE'}, token=at)
        call('GET', prefix + '/users/me', token=ut, expected=401)
        u, _ = login(user_email, password)
        ut = u['access_token']
        changed_password = secrets.token_urlsafe(32)
        call('POST', prefix + '/users/me/change-password', {'current_password': password, 'new_password': changed_password}, token=ut, expected=204)
        b, _ = call('GET', prefix + '/users/me/sessions', token=ut)
        current = next(s['id'] for s in b['items'] if s['is_current'])
        call('DELETE', prefix + '/users/me/sessions/' + current, token=ut, expected=204)
        call('GET', prefix + '/users/me', token=ut, expected=401)
        b, _ = call('POST', prefix + '/auth/forgot-password', {'email': user_email}, expected=202)
        check('reset_token' not in b, 'reset API never exposes token')
        message = email.message_from_bytes(mailbox.get(timeout=20))
        check(message['To'] == user_email, 'SMTP sink received intended recipient')
        text = message.get_payload(decode=True).decode(message.get_content_charset() or 'utf-8')
        reset_token = text.split('minutes: ', 1)[1].strip()
        reset_password = secrets.token_urlsafe(32)
        # SMTP delivery occurs inside the transaction; allow the commit to finish.
        time.sleep(.2)
        call('POST', prefix + '/auth/reset-password', {'reset_token': reset_token, 'new_password': reset_password}, expected=204)
        call('POST', prefix + '/auth/reset-password', {'reset_token': reset_token, 'new_password': reset_password}, expected=400)
        login(user_email, reset_password)
        stop()
        start()
        call('GET', '/actuator/health/readiness')
        a2, _ = login(admin_email, admin_password)
        call('GET', prefix + '/admin/devices', token=a2['access_token'])
        check(True, 'same database and signing keys restart successful')
        print('PASS: runtime HTTP, CLI-created admin login, SMTP sink reset, restart; no credentials printed')
    finally:
        stop()
        sink.shutdown()
        sink.server_close()
        (local / 'backend-runtime-results.json').write_text(json.dumps(results, indent=2), encoding='utf-8')


if __name__ == '__main__':
    run()
