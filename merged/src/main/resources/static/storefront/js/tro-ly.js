/* ============================================================================
   TRỢ LÝ ẢO FS SHOES — widget chat nổi ở góc phải mọi trang website bán hàng.
   • Chatbot trả lời tự động câu hỏi về sản phẩm / giá / tồn kho / khuyến mãi /
     voucher / đơn hàng / chính sách.
   • Khách gõ "gặp nhân viên" → chuyển sang nhân viên hỗ trợ; tin nhắn 2 chiều
     hiện THỜI GIAN THỰC qua WebSocket (/topic/hotro/phien/{maPhien}),
     không cần tải lại trang.
   Tự chứa: tự chèn HTML + CSS, dùng SockJS + STOMP (nạp động nếu thiếu).
   ============================================================================ */
(function () {
    if (window.__fsTroLyInit) return;
    window.__fsTroLyInit = true;

    var maPhien = null;
    var idDaHienThi = {};   // chống hiện trùng khi vừa POST vừa nhận realtime
    var stompClient = null;

    /* ---------- CSS ---------- */
    var css = document.createElement('style');
    css.textContent = [
        '#fsChatFab{position:fixed;right:22px;bottom:22px;z-index:1400;width:60px;height:60px;border-radius:999px;',
        'border:0;cursor:pointer;background:linear-gradient(135deg,#d92d20,#f04438);color:#fff;font-size:26px;',
        'box-shadow:0 12px 30px rgba(217,45,32,.42);display:grid;place-items:center;transition:transform .15s}',
        '#fsChatFab:hover{transform:translateY(-3px) scale(1.04)}',
        '#fsChatFab .badge{position:absolute;top:-3px;right:-3px;min-width:20px;height:20px;background:#fff;',
        'color:#d92d20;border-radius:999px;font-size:12px;font-weight:800;display:none;place-items:center;padding:0 5px}',
        '#fsChatBox{position:fixed;right:22px;bottom:92px;z-index:1400;width:376px;max-width:calc(100vw - 28px);',
        'height:540px;max-height:calc(100vh - 130px);background:#fff;border-radius:18px;overflow:hidden;display:none;',
        'flex-direction:column;box-shadow:0 26px 64px rgba(16,24,40,.32);border:1px solid #eceff4;',
        'font-family:system-ui,-apple-system,"Be Vietnam Pro",sans-serif}',
        '#fsChatBox.open{display:flex;animation:fsChatUp .22s ease}',
        '@keyframes fsChatUp{from{opacity:0;transform:translateY(12px)}}',
        '.fs-chat-head{background:linear-gradient(135deg,#d92d20,#f04438);color:#fff;padding:14px 16px;display:flex;',
        'align-items:center;gap:11px}',
        '.fs-chat-head .ava{width:38px;height:38px;border-radius:999px;background:rgba(255,255,255,.22);display:grid;',
        'place-items:center;font-size:20px}',
        '.fs-chat-head b{font-size:15px;display:block;line-height:1.2}',
        '.fs-chat-head small{font-size:11.5px;opacity:.92;display:flex;align-items:center;gap:5px}',
        '.fs-chat-head .cham{width:8px;height:8px;border-radius:999px;background:#41e08a;display:inline-block}',
        '.fs-chat-head .dong{margin-left:auto;background:transparent;border:0;color:#fff;font-size:22px;cursor:pointer;opacity:.9}',
        '.fs-chat-body{flex:1;overflow-y:auto;padding:15px;background:#f7f8fa;display:flex;flex-direction:column;gap:10px}',
        '.fs-msg{max-width:82%;padding:9px 13px;border-radius:15px;font-size:13.7px;line-height:1.5;white-space:pre-wrap;',
        'word-wrap:break-word}',
        '.fs-msg a{color:inherit;font-weight:700;text-decoration:underline}',
        '.fs-msg.khach{align-self:flex-end;background:linear-gradient(135deg,#d92d20,#f04438);color:#fff;border-bottom-right-radius:5px}',
        '.fs-msg.bot{align-self:flex-start;background:#fff;color:#101828;border:1px solid #eaecf0;border-bottom-left-radius:5px}',
        '.fs-msg.nhanvien{align-self:flex-start;background:#eef4ff;color:#0b3b8f;border:1px solid #cfe0ff;border-bottom-left-radius:5px}',
        '.fs-msg .who{display:block;font-size:10.5px;font-weight:800;opacity:.75;margin-bottom:2px}',
        '.fs-msg .khi{display:block;font-size:10px;opacity:.6;margin-top:3px;text-align:right}',
        '.fs-chat-foot{padding:10px;border-top:1px solid #eef0f4;background:#fff;display:flex;gap:8px}',
        '.fs-chat-foot textarea{flex:1;resize:none;border:1px solid #dfe3ea;border-radius:12px;padding:9px 12px;',
        'font-size:13.5px;font-family:inherit;max-height:96px;outline:none}',
        '.fs-chat-foot textarea:focus{border-color:#d92d20}',
        '.fs-chat-foot button{width:42px;border:0;border-radius:12px;background:linear-gradient(135deg,#d92d20,#f04438);',
        'color:#fff;font-size:18px;cursor:pointer;flex-shrink:0}',
        '.fs-chat-goiy{display:flex;flex-wrap:wrap;gap:6px;padding:0 15px 6px}',
        '.fs-chat-goiy button{background:#fff;border:1px solid #fecdca;color:#b42318;border-radius:999px;',
        'padding:5px 11px;font-size:12px;cursor:pointer;font-family:inherit}',
        '.fs-chat-goiy button:hover{background:#fef3f2}',
        '.fs-typing{align-self:flex-start;color:#98a2b3;font-size:12.5px;padding:2px 4px}'
    ].join('');
    document.head.appendChild(css);

    /* ---------- HTML ---------- */
    var fab = document.createElement('button');
    fab.id = 'fsChatFab';
    fab.type = 'button';
    fab.title = 'Trợ lý ảo FS Shoes';
    fab.innerHTML = '<i class="bi bi-chat-heart-fill"></i><span class="badge" id="fsChatBadge">1</span>';
    document.body.appendChild(fab);

    var box = document.createElement('div');
    box.id = 'fsChatBox';
    box.innerHTML =
        '<div class="fs-chat-head">' +
        '<div class="ava"><i class="bi bi-robot"></i></div>' +
        '<div><b>Trợ lý FS Shoes</b><small><span class="cham"></span> Thường trả lời ngay</small></div>' +
        '<button class="dong" type="button" title="Đóng">&times;</button>' +
        '</div>' +
        '<div class="fs-chat-body" id="fsChatBody"></div>' +
        '<div class="fs-chat-goiy" id="fsChatGoiy">' +
        '<button type="button">Có khuyến mãi gì không?</button>' +
        '<button type="button">Xem giày bán chạy</button>' +
        '<button type="button">Phí ship bao nhiêu?</button>' +
        '<button type="button">Gặp nhân viên</button>' +
        '</div>' +
        '<div class="fs-chat-foot">' +
        '<textarea id="fsChatInput" rows="1" placeholder="Nhập câu hỏi của bạn..."></textarea>' +
        '<button type="button" id="fsChatSend" title="Gửi"><i class="bi bi-send-fill"></i></button>' +
        '</div>';
    document.body.appendChild(box);

    var body = box.querySelector('#fsChatBody');
    var input = box.querySelector('#fsChatInput');
    var badge = fab.querySelector('#fsChatBadge');
    var chuaDoc = 0;

    function escapeHtml(s) {
        return String(s == null ? '' : s).replace(/[&<>"]/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c];
        });
    }
    // Biến link (đường dẫn nội bộ + URL) và số hotline thành thẻ bấm được
    function linkify(s) {
        var t = escapeHtml(s);
        t = t.replace(/(https?:\/\/[^\s]+)/g, '<a href="$1" target="_blank" rel="noopener">$1</a>');
        t = t.replace(/(^|[\s(])(\/[A-Za-z0-9\-_/?.=&#%]+)/g, '$1<a href="$2">$2</a>');
        return t;
    }

    function themTin(m) {
        if (m.maTinNhan && idDaHienThi[m.maTinNhan]) return;
        if (m.maTinNhan) idDaHienThi[m.maTinNhan] = true;
        var el = document.createElement('div');
        var loai = m.nguoiGui === 'KHACH' ? 'khach' : (m.nguoiGui === 'NHANVIEN' ? 'nhanvien' : 'bot');
        el.className = 'fs-msg ' + loai;
        var who = '';
        if (m.nguoiGui === 'BOT') who = '<span class="who">🤖 Trợ lý ảo</span>';
        else if (m.nguoiGui === 'NHANVIEN') who = '<span class="who">👩‍💼 ' + escapeHtml(m.tenHienThi || 'Nhân viên') + '</span>';
        el.innerHTML = who + linkify(m.noiDung) +
            (m.thoiGian ? '<span class="khi">' + escapeHtml(m.thoiGian) + '</span>' : '');
        body.appendChild(el);
        body.scrollTop = body.scrollHeight;
    }

    var typingEl = null;
    function hienTyping() {
        if (typingEl) return;
        typingEl = document.createElement('div');
        typingEl.className = 'fs-typing';
        typingEl.textContent = 'Trợ lý đang soạn trả lời…';
        body.appendChild(typingEl);
        body.scrollTop = body.scrollHeight;
    }
    function anTyping() { if (typingEl) { typingEl.remove(); typingEl = null; } }

    /* ---------- API ---------- */
    function taiLichSu() {
        fetch('/api/ho-tro/lich-su').then(function (r) { return r.json(); }).then(function (d) {
            maPhien = d.maPhien;
            body.innerHTML = '';
            idDaHienThi = {};
            if (!d.tinNhans || d.tinNhans.length === 0) {
                themTin({ nguoiGui: 'BOT',
                    noiDung: 'Xin chào 👋 Mình là trợ lý ảo của FS Shoes. Mình có thể giúp bạn tìm giày, ' +
                        'xem giá, kiểm tra size/màu còn hàng, khuyến mãi, hay tra cứu đơn hàng. ' +
                        'Bạn cần hỗ trợ gì nào?' });
            } else {
                d.tinNhans.forEach(themTin);
            }
            ketNoiRealtime();
        }).catch(function () {
            themTin({ nguoiGui: 'BOT', noiDung: 'Không kết nối được máy chủ chat. Bạn thử tải lại trang nhé.' });
        });
    }

    function gui(noiDung) {
        noiDung = (noiDung || '').trim();
        if (!noiDung) return;
        input.value = '';
        input.style.height = 'auto';
        // Hiện tin của khách NGAY LẬP TỨC (trước khi chờ máy chủ) để luôn đứng TRÊN câu trả lời
        themTin({ nguoiGui: 'KHACH', noiDung: noiDung });
        hienTyping();
        fetch('/api/ho-tro/gui', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ noiDung: noiDung })
        }).then(function (r) { return r.json(); }).then(function (d) {
            anTyping();
            maPhien = d.maPhien || maPhien;
            (d.tinMoi || []).forEach(function (m) {
                if (m.nguoiGui === 'KHACH') {
                    // đã hiện bản nháp ngay khi gửi — chỉ ghi nhận id để chống trùng realtime
                    if (m.maTinNhan) idDaHienThi[m.maTinNhan] = true;
                } else {
                    themTin(m);
                }
            });
            if (!stompClient) ketNoiRealtime();
        }).catch(function () {
            anTyping();
            themTin({ nguoiGui: 'BOT', noiDung: 'Gửi không thành công, bạn thử lại giúp mình nhé.' });
        });
    }

    /* ---------- realtime ---------- */
    function ketNoiRealtime() {
        if (stompClient || !maPhien) return;
        function connect() {
            if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
            var sock = new SockJS('/ws');
            stompClient = Stomp.over(sock);
            stompClient.debug = null;
            stompClient.connect({}, function () {
                stompClient.subscribe('/topic/hotro/phien/' + maPhien, function (f) {
                    try {
                        var m = JSON.parse(f.body);
                        if (m.nguoiGui === 'KHACH') return;   // tin của chính mình đã hiện khi gõ
                        anTyping();
                        themTin(m);
                        if (!box.classList.contains('open')) {
                            chuaDoc++; badge.textContent = chuaDoc; badge.style.display = 'grid';
                        }
                    } catch (e) {}
                });
            }, function () { stompClient = null; setTimeout(connect, 5000); });
        }
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            napThuVien(connect);
        } else {
            connect();
        }
    }

    function napThuVien(xong) {
        var d1 = document.createElement('script');
        d1.src = 'https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.6.1/sockjs.min.js';
        d1.onload = function () {
            var d2 = document.createElement('script');
            d2.src = 'https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js';
            d2.onload = xong;
            document.head.appendChild(d2);
        };
        document.head.appendChild(d1);
    }

    /* ---------- sự kiện ---------- */
    fab.addEventListener('click', function () {
        box.classList.toggle('open');
        if (box.classList.contains('open')) {
            chuaDoc = 0; badge.style.display = 'none';
            if (maPhien === null) taiLichSu();
            input.focus();
        }
    });
    box.querySelector('.dong').addEventListener('click', function () { box.classList.remove('open'); });
    box.querySelector('#fsChatSend').addEventListener('click', function () { gui(input.value); });
    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); gui(input.value); }
    });
    input.addEventListener('input', function () {
        input.style.height = 'auto';
        input.style.height = Math.min(input.scrollHeight, 96) + 'px';
    });
    box.querySelector('#fsChatGoiy').addEventListener('click', function (e) {
        if (e.target.tagName === 'BUTTON') gui(e.target.textContent);
    });

    // Nạp lịch sử sẵn để badge hoạt động dù khách chưa mở khung
    taiLichSu();
})();