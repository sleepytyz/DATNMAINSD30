(function () {
    "use strict";
    if (!Array.isArray(bienThe) || bienThe.length === 0) return;

    var selectedColor = null;
    var selectedSize = null;

    var colorWrap = document.getElementById("colorSwatches");
    var sizeWrap = document.getElementById("sizeSwatches");
    var btnAdd = document.getElementById("btnAddCart");
    var qtyInput = document.getElementById("qtyInput");
    var stockNote = document.getElementById("stockNote");
    var priceNow = document.getElementById("priceNow");
    var priceOld = document.getElementById("priceOld");
    var pricePct = document.getElementById("pricePct");
    var mainImg = document.getElementById("mainImg");

    function fmt(n) {
        return Math.round(n).toLocaleString("vi-VN") + "đ";
    }

    function uniqueColors() {
        var seen = {}, list = [];
        bienThe.forEach(function (b) {
            if (!seen[b.maMauSac]) { seen[b.maMauSac] = true; list.push({ id: b.maMauSac, ten: b.tenMauSac }); }
        });
        return list;
    }

    function sizesForColor(colorId) {
        var seen = {}, list = [];
        bienThe.filter(function (b) { return b.maMauSac === colorId; }).forEach(function (b) {
            if (!seen[b.maKichThuoc]) { seen[b.maKichThuoc] = true; list.push({ id: b.maKichThuoc, ten: b.tenKichThuoc, ton: b.soLuongTon }); }
        });
        return list;
    }

    function findVariant() {
        return bienThe.find(function (b) { return b.maMauSac === selectedColor && b.maKichThuoc === selectedSize; });
    }

    function renderColors() {
        colorWrap.innerHTML = "";
        uniqueColors().forEach(function (c) {
            var btn = document.createElement("button");
            btn.type = "button";
            btn.className = "fs-swatch" + (c.id === selectedColor ? " active" : "");
            btn.textContent = c.ten;
            btn.onclick = function () {
                selectedColor = c.id;
                selectedSize = null;
                renderColors();
                renderSizes();
                updateSelection();
            };
            colorWrap.appendChild(btn);
        });
    }

    function renderSizes() {
        sizeWrap.innerHTML = "";
        if (selectedColor === null) {
            sizeWrap.innerHTML = '<span class="fs-stock-note">Vui lòng chọn màu trước</span>';
            return;
        }
        sizesForColor(selectedColor).forEach(function (s) {
            var btn = document.createElement("button");
            btn.type = "button";
            btn.className = "fs-swatch" + (s.id === selectedSize ? " active" : "");
            btn.textContent = s.ten;
            btn.disabled = s.ton <= 0;
            btn.onclick = function () {
                selectedSize = s.id;
                renderSizes();
                updateSelection();
            };
            sizeWrap.appendChild(btn);
        });
    }

    function updateSelection() {
        var v = findVariant();
        if (!v) {
            btnAdd.disabled = true;
            btnAdd.removeAttribute("data-ma-spct");
            stockNote.textContent = "Vui lòng chọn màu và kích thước";
            return;
        }

        if (v.anh) mainImg.src = v.anh;
        priceNow.textContent = fmt(v.giaSauGiam);
        if (v.phanTramGiam > 0) {
            priceOld.style.display = "inline";
            priceOld.textContent = fmt(v.giaGoc);
            pricePct.style.display = "inline";
            pricePct.textContent = "-" + v.phanTramGiam + "%";
        } else {
            priceOld.style.display = "none";
            pricePct.style.display = "none";
        }

        if (v.soLuongTon > 0) {
            btnAdd.disabled = false;
            btnAdd.setAttribute("data-ma-spct", v.maSanPhamChiTiet);
            qtyInput.max = Math.min(v.soLuongTon, 10);
            if (parseInt(qtyInput.value, 10) > qtyInput.max) qtyInput.value = qtyInput.max;
            stockNote.textContent = "Còn " + v.soLuongTon + " sản phẩm";
        } else {
            btnAdd.disabled = true;
            btnAdd.removeAttribute("data-ma-spct");
            stockNote.textContent = "Hết hàng với lựa chọn này";
        }
    }

    renderColors();
    renderSizes();
    updateSelection();
})();