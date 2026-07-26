/* =====================================================================
   FS SHOES — storefront.js
   Toast • AJAX giỏ hàng (CSRF) • Yêu thích • Gợi ý tìm kiếm • Reveal
   • Theo dõi đơn hàng THỜI GIAN THỰC (SockJS/STOMP)
   ===================================================================== */
(function () {
    'use strict';

    // ---------------- Tiện ích chung ----------------
    var META_TOKEN = document.querySelector('meta[name="_csrf"]');
    var META_HEADER = document.querySelector('meta[name="_csrf_header"]');

    function fsTien(n) {
        var so = Number(n || 0);
        return so.toLocaleString('vi-VN') + '\u20ab';
    }
    window.fsTien = fsTien;

    function fsPost(url, data) {
        var body = new URLSearchParams();
        if (data) Object.keys(data).forEach(function (k) {
            if (data[k] !== undefined && data[k] !== null) body.append(k, data[k]);
        });
        var headers = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
        if (META_TOKEN && META_HEADER) headers[META_HEADER.content] = META_TOKEN.content;
        return fetch(url, { method: 'POST', headers: headers, body: body.toString() })
            .then(function (r) {
                if (r.status === 401 || r.status === 403) {
                    fsToast('Vui lòng đăng nhập để tiếp tục.', 'err');
                    throw new Error('unauthorized');
                }
                return r.json();
            });
    }
    window.fsPost = fsPost;

    // ---------------- Toast ----------------
    function fsToast(msg, type) {
        var box = document.getElementById('fsToasts');
        if (!box) return alert(msg);
        var t = document.createElement('div');
        t.className = 'fs-toast ' + (type === 'err' ? 'err' : (type === 'info' ? 'info' : 'ok'));
        var icon = type === 'err' ? 'bi-x-circle-fill' : (type === 'info' ? 'bi-info-circle-fill' : 'bi-check-circle-fill');
        t.innerHTML = '<i class="bi ' + icon + '"></i><span></span>';
        t.querySelector('span').textContent = msg;
        box.appendChild(t);
        setTimeout(function () {
            t.classList.add('out');
            setTimeout(function () { t.remove(); }, 260);
        }, 3400);
    }
    window.fsToast = fsToast;

    // ---------------- Badge giỏ hàng ----------------
    function fsBadge(soLuong) {
        var b = document.getElementById('fsGioBadge');
        if (!b) return;
        b.textContent = soLuong;
        b.classList.remove('bump');
        void b.offsetWidth;
        b.classList.add('bump');
    }
    window.fsBadge = fsBadge;

    // ---------------- Header đổ bóng khi cuộn ----------------
    var header = document.getElementById('fsHeader');
    if (header) {
        window.addEventListener('scroll', function () {
            header.classList.toggle('scrolled', window.scrollY > 8);
        }, { passive: true });
    }

    // ---------------- Reveal khi cuộn ----------------
    if ('IntersectionObserver' in window) {
        var io = new IntersectionObserver(function (entries) {
            entries.forEach(function (e) {
                if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); }
            });
        }, { threshold: 0.08 });
        document.querySelectorAll('.reveal').forEach(function (el) { io.observe(el); });
    } else {
        document.querySelectorAll('.reveal').forEach(function (el) { el.classList.add('in'); });
    }

    // ---------------- Tabs (chi tiết sản phẩm...) ----------------
    document.querySelectorAll('.fs-tab-head button[data-tab]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var wrap = btn.closest('.fs-tabs');
            wrap.querySelectorAll('.fs-tab-head button').forEach(function (b) { b.classList.remove('active'); });
            wrap.querySelectorAll('.fs-tab-panel').forEach(function (p) { p.classList.remove('active'); });
            btn.classList.add('active');
            var panel = wrap.querySelector('#' + btn.dataset.tab);
            if (panel) panel.classList.add('active');
        });
    });

    // ---------------- Yêu thích (uỷ quyền sự kiện) ----------------
    document.addEventListener('click', function (ev) {
        var heart = ev.target.closest('.js-yeu-thich');
        if (!heart) return;
        ev.preventDefault();
        var ma = heart.dataset.ma;
        if (!ma) return;
        fsPost('/cua-hang/yeu-thich/' + encodeURIComponent(ma), {}).then(function (kq) {
            heart.classList.toggle('active', !!kq.yeuThich);
            var ic = heart.querySelector('i');
            if (ic) ic.className = 'bi ' + (kq.yeuThich ? 'bi-heart-fill' : 'bi-heart');
            fsToast(kq.thongBao || 'Đã cập nhật yêu thích.', 'ok');
        }).catch(function () {});
    });

    // ---------------- Thêm giỏ nhanh (nút có .js-them-gio) ----------------
    document.addEventListener('click', function (ev) {
        var btn = ev.target.closest('.js-them-gio');
        if (!btn) return;
        ev.preventDefault();
        var ma = btn.dataset.ma;
        var soLuong = parseInt(btn.dataset.soLuong || '1', 10);
        if (!ma) return;
        btn.disabled = true;
        fsPost('/gio-hang/them', { maSanPhamChiTiet: ma, soLuong: soLuong }).then(function (kq) {
            fsBadge(kq.tongSoLuong);
            fsToast(kq.thongBao, kq.ok ? 'ok' : 'err');
        }).catch(function () {}).finally(function () { btn.disabled = false; });
    });

    // =====================================================================
    // TRANG GIỎ HÀNG — cập nhật số lượng / xoá / voucher, tổng tiền tính lại tức thì
    // =====================================================================
    var cartPage = document.getElementById('fsCartPage');

    function apDungTomTat(kq) {
        fsBadge(kq.tongSoLuong);
        var sTien = document.getElementById('sumTienHang');
        if (sTien) sTien.textContent = fsTien(kq.tongTienHang);
        var rowTK = document.getElementById('rowTietKiem');
        if (rowTK) {
            rowTK.style.display = Number(kq.tietKiemKhuyenMai) > 0 ? '' : 'none';
            var v = document.getElementById('sumTietKiem');
            if (v) v.textContent = '-' + fsTien(kq.tietKiemKhuyenMai);
        }
        var rowVc = document.getElementById('rowVoucher');
        if (rowVc) {
            rowVc.style.display = Number(kq.soTienGiamVoucher) > 0 ? '' : 'none';
            var v2 = document.getElementById('sumVoucher');
            if (v2) v2.textContent = '-' + fsTien(kq.soTienGiamVoucher);
        }
        var sShip = document.getElementById('sumShip');
        if (sShip) sShip.textContent = Number(kq.tienShip) > 0 ? fsTien(kq.tienShip) : 'Miễn phí';
        var sTong = document.getElementById('sumTong');
        if (sTong) sTong.textContent = fsTien(kq.tongThanhToan);

        // Thanh tiến độ freeship
        var fill = document.getElementById('fsFreeshipFill');
        var txt = document.getElementById('fsFreeshipText');
        if (fill && txt) {
            var thieu = Number(kq.conThieuDeFreeship || 0);
            var nguong = 500000;
            var dat = Math.max(0, Math.min(100, (1 - thieu / nguong) * 100));
            if (Number(kq.tongSoLuong) === 0) dat = 0;
            fill.style.width = dat + '%';
            txt.innerHTML = thieu > 0
                ? ('Mua thêm <b>' + fsTien(thieu) + '</b> để được <b>miễn phí vận chuyển</b>')
                : (Number(kq.tongSoLuong) > 0 ? '<b>Tuyệt vời!</b> Đơn của bạn được miễn phí vận chuyển 🎉' : 'Đơn từ 500.000\u20ab được miễn phí vận chuyển');
        }

        // Hộp voucher đã áp dụng
        var apBox = document.getElementById('fsVoucherApplied');
        var nhapBox = document.getElementById('fsVoucherNhap');
        if (apBox && nhapBox) {
            if (kq.tenVoucher) {
                apBox.style.display = '';
                nhapBox.style.display = 'none';
                var tenEl = document.getElementById('fsVoucherTen');
                if (tenEl) tenEl.textContent = kq.tenVoucher;
            } else {
                apBox.style.display = 'none';
                nhapBox.style.display = '';
            }
        }

        // Cập nhật từng dòng
        (kq.dong || []).forEach(function (d) {
            var line = document.querySelector('.fs-cart-line[data-ma="' + d.ma + '"]');
            if (!line) return;
            var inp = line.querySelector('.js-qty-input');
            if (inp) { inp.value = d.soLuong; inp.max = d.soLuongTon; }
            var tt = line.querySelector('.js-line-total');
            if (tt) tt.textContent = fsTien(d.thanhTien);
        });
        // Xoá phần tử của dòng không còn trong giỏ
        document.querySelectorAll('.fs-cart-line[data-ma]').forEach(function (line) {
            var ma = line.dataset.ma;
            var conLai = (kq.dong || []).some(function (d) { return d.ma === ma; });
            if (!conLai) line.remove();
        });
        // Giỏ trống → tải lại để hiện trạng thái trống
        if ((kq.dong || []).length === 0 && document.querySelector('.fs-cart-list')) {
            location.reload();
        }
        (kq.canhBao || []).forEach(function (c) { fsToast(c, 'info'); });
    }
    window.fsApDungTomTat = apDungTomTat;

    if (cartPage) {
        function capNhatDong(ma, soLuong) {
            fsPost('/gio-hang/cap-nhat', { maSanPhamChiTiet: ma, soLuong: soLuong }).then(function (kq) {
                apDungTomTat(kq);
                if (kq.thongBao) fsToast(kq.thongBao, 'info');
            }).catch(function () {});
        }

        cartPage.addEventListener('click', function (ev) {
            var minus = ev.target.closest('.js-qty-minus');
            var plus = ev.target.closest('.js-qty-plus');
            var xoa = ev.target.closest('.js-xoa-line');
            if (minus || plus) {
                var line = ev.target.closest('.fs-cart-line');
                var inp = line.querySelector('.js-qty-input');
                var v = parseInt(inp.value || '1', 10) + (plus ? 1 : -1);
                var max = parseInt(inp.max || '99', 10);
                if (v > max) { fsToast('Chỉ còn ' + max + ' sản phẩm trong kho.', 'info'); v = max; }
                capNhatDong(line.dataset.ma, Math.max(0, v));
            }
            if (xoa) {
                var line2 = ev.target.closest('.fs-cart-line');
                fsPost('/gio-hang/xoa', { maSanPhamChiTiet: line2.dataset.ma }).then(function (kq) {
                    apDungTomTat(kq);
                    fsToast(kq.thongBao, 'ok');
                }).catch(function () {});
            }
        });
        cartPage.addEventListener('change', function (ev) {
            var inp = ev.target.closest('.js-qty-input');
            if (!inp) return;
            var line = ev.target.closest('.fs-cart-line');
            capNhatDong(line.dataset.ma, Math.max(0, parseInt(inp.value || '0', 10)));
        });

        // Voucher: nhập tay
        var vForm = document.getElementById('fsVoucherForm');
        if (vForm) vForm.addEventListener('submit', function (ev) {
            ev.preventDefault();
            var input = vForm.querySelector('input[name=maVoucher]');
            fsPost('/gio-hang/ap-dung-voucher', { maVoucher: input.value }).then(function (kq) {
                apDungTomTat(kq);
                fsToast(kq.thongBao, kq.ok ? 'ok' : 'err');
                if (kq.ok) input.value = '';
            }).catch(function () {});
        });
        // Voucher: bấm chọn từ gợi ý
        document.addEventListener('click', function (ev) {
            var pick = ev.target.closest('.js-ap-voucher');
            if (!pick) return;
            fsPost('/gio-hang/ap-dung-voucher', { maVoucher: pick.dataset.ten }).then(function (kq) {
                apDungTomTat(kq);
                fsToast(kq.thongBao, kq.ok ? 'ok' : 'err');
            }).catch(function () {});
        });
        // Bỏ voucher
        document.addEventListener('click', function (ev) {
            var bo = ev.target.closest('.js-bo-voucher');
            if (!bo) return;
            fsPost('/gio-hang/bo-voucher', {}).then(function (kq) {
                apDungTomTat(kq);
                fsToast(kq.thongBao, 'ok');
            }).catch(function () {});
        });
    }

    // =====================================================================
    // GỢI Ý TÌM KIẾM NHANH
    // =====================================================================
    var searchInput = document.getElementById('fsSearchInput');
    var suggestBox = document.getElementById('fsSearchSuggest');
    if (searchInput && suggestBox) {
        var timer = null;
        searchInput.addEventListener('input', function () {
            clearTimeout(timer);
            var q = searchInput.value.trim();
            if (q.length < 2) { suggestBox.classList.remove('open'); return; }
            timer = setTimeout(function () {
                fetch('/api/cua-hang/tim-kiem?q=' + encodeURIComponent(q))
                    .then(function (r) { return r.json(); })
                    .then(function (ds) {
                        if (!ds.length) {
                            suggestBox.innerHTML = '<div class="fs-suggest-empty">Không tìm thấy sản phẩm phù hợp</div>';
                        } else {
                            suggestBox.innerHTML = ds.map(function (s) {
                                var gia = fsTien(s.gia) + (s.phanTramGiam > 0 ? ' <s>' + fsTien(s.giaGoc) + '</s>' : '');
                                return '<a class="fs-suggest-item" href="/cua-hang/san-pham/' + encodeURIComponent(s.ma) + '">'
                                    + '<img src="' + s.anh + '" onerror="this.src=\'/storefront/img/no-image.svg\'" alt="">'
                                    + '<span><span class="ten"></span><span class="gia">' + gia + (s.conHang ? '' : ' • Hết hàng') + '</span></span></a>';
                            }).join('');
                            var items = suggestBox.querySelectorAll('.fs-suggest-item .ten');
                            ds.forEach(function (s, i) { if (items[i]) items[i].textContent = s.ten; });
                        }
                        suggestBox.classList.add('open');
                    }).catch(function () {});
            }, 250);
        });
        document.addEventListener('click', function (ev) {
            if (!ev.target.closest('#fsSearchBox')) suggestBox.classList.remove('open');
        });
    }

    // =====================================================================
    // THEO DÕI ĐƠN HÀNG THỜI GIAN THỰC
    // Trang cần dùng: nạp SockJS + STOMP (CDN) rồi gọi fsKetNoiDonHang('HD...')
    // Quy ước DOM: #fsTTChip (chip trạng thái), #fsTimeline .fs-step[data-buoc=1..4], #fsHuyNote
    // =====================================================================
    var TT_BUOC = { 'Chờ thanh toán': 1, 'Chờ xác nhận': 1, 'Đã xác nhận': 2, 'Đang giao': 3, 'Đã giao': 4 };
    var TT_CLASS = {
        'Chờ thanh toán': 'fs-tt-cho', 'Chờ xác nhận': 'fs-tt-cho', 'Đã xác nhận': 'fs-tt-xacnhan',
        'Đang giao': 'fs-tt-giao', 'Đã giao': 'fs-tt-xong', 'Đã huỷ': 'fs-tt-huy'
    };

    function fsVeTrangThai(tt) {
        var chip = document.getElementById('fsTTChip');
        if (chip) {
            chip.textContent = tt;
            chip.className = 'fs-tt ' + (TT_CLASS[tt] || 'fs-tt-cho');
        }
        var tl = document.getElementById('fsTimeline');
        if (tl) {
            var buoc = TT_BUOC[tt] || 0;
            tl.querySelectorAll('.fs-step').forEach(function (st) {
                var b = parseInt(st.dataset.buoc, 10);
                st.classList.remove('done', 'now');
                if (buoc > 0) {
                    if (b < buoc || (buoc === 4 && b === 4)) st.classList.add('done');
                    if (b === buoc && buoc < 4) st.classList.add('now');
                }
            });
        }
        var huy = document.getElementById('fsHuyNote');
        if (huy) huy.style.display = (tt === 'Đã huỷ') ? '' : 'none';
        var nutHuy = document.getElementById('fsNutHuyDon');
        if (nutHuy) nutHuy.style.display = (tt === 'Chờ xác nhận' || tt === 'Chờ thanh toán') ? '' : 'none';
    }
    window.fsVeTrangThai = fsVeTrangThai;

    window.fsKetNoiDonHang = function (maHoaDon) {
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
        try {
            var sock = new SockJS('/ws');
            var client = Stomp.over(sock);
            client.debug = null;
            client.connect({}, function () {
                client.subscribe('/topic/don-hang/' + maHoaDon, function (frame) {
                    try {
                        var msg = JSON.parse(frame.body);
                        if (msg.loai === 'DOI_TRANG_THAI') {
                            fsVeTrangThai(msg.trangThaiMoi);
                            fsToast('Đơn ' + maHoaDon + ': ' + msg.trangThaiCu + ' → ' + msg.trangThaiMoi
                                + ' (' + (msg.nguoiThucHien || 'hệ thống') + ')', 'info');
                        }
                    } catch (e) { /* bỏ qua */ }
                });
            }, function () { /* mất kết nối: trang vẫn dùng được bình thường */ });
        } catch (e) { /* SockJS không khả dụng */ }
    };
})();


/* ===== Header cố định: đo chiều cao thật để đẩy nội dung xuống vừa khít ===== */
(function () {
    function capNhatCaoHeader() {
        var h = document.querySelector('.fs-header');
        if (h) document.documentElement.style.setProperty('--fs-header-h', h.offsetHeight + 'px');
    }
    window.addEventListener('load', capNhatCaoHeader);
    window.addEventListener('resize', capNhatCaoHeader);
    capNhatCaoHeader();

    // Trên thiết bị cảm ứng: chạm lần đầu vào "Sản phẩm" để MỞ menu, chạm mục con để đi
    document.querySelectorAll('.fs-nav-drop > a').forEach(function (a) {
        a.addEventListener('click', function (e) {
            var drop = a.parentElement;
            var chuaMo = !drop.classList.contains('open');
            var camUng = window.matchMedia('(hover: none)').matches;
            if (camUng && chuaMo && drop.querySelector('.fs-nav-menu')) {
                e.preventDefault();
                document.querySelectorAll('.fs-nav-drop.open').forEach(function (d) { d.classList.remove('open'); });
                drop.classList.add('open');
            }
        });
    });
    document.addEventListener('click', function (e) {
        if (!e.target.closest('.fs-nav-drop')) {
            document.querySelectorAll('.fs-nav-drop.open').forEach(function (d) { d.classList.remove('open'); });
        }
    });
})();


/* ===== Highlight ô nav của TRANG ĐANG MỞ ===== */
(function fsNavActive() {
    var nav = document.querySelector('.fs-nav');
    if (!nav) return;
    var duongDan = location.pathname;
    var truyVan = location.search || '';
    var links = nav.querySelectorAll('.fs-nav-inner > a, .fs-nav-drop > a');
    var tot = null, diemTot = -1;
    links.forEach(function (a) {
        var href = a.getAttribute('href') || '';
        if (!href || href.charAt(0) === '#') return;
        var phan = href.split('?');
        var path = phan[0], query = phan[1] || '';
        var khopPath = (path === '/') ? (duongDan === '/')
            : (duongDan === path || duongDan.indexOf(path + '/') === 0 || duongDan.indexOf(path) === 0);
        if (!khopPath) return;
        // Ưu tiên link khớp cả tham số (vd Bán chạy ?sapXep=ban-chay), sau đó tới path dài nhất
        var diem = path.length + (query && truyVan.indexOf(query) >= 0 ? 1000 : 0);
        if (query && truyVan.indexOf(query) < 0 && path !== '/') diem -= 1; // link có query không khớp thì lép vế
        if (diem > diemTot) { diemTot = diem; tot = a; }
    });
    if (tot) tot.classList.add('active');
})();