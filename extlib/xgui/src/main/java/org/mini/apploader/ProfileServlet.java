package org.mini.apploader;

import org.mini.http.MiniHttpServer;
import org.mini.util.SysLog;
import org.mini.gui.callback.GCallBack;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class ProfileServlet extends MiniHttpServer.UserServlet {
    private static final int SLOW_MAGIC = 0x53435632;
    private static final int SLOW_NODE_SIZE = 40;
    private static final int SLOW_FLAG_EXCLUDED = 1;
    private static final String SLOW_SESSION_CACHE_KEY = "profile.slow.snapshot.cache";
    private static final java.util.Map<Integer, String> METHOD_NAME_CACHE = new java.util.HashMap<>();
    private static final java.util.Map<Integer, String> METHOD_LABEL_CACHE = new java.util.HashMap<>();

    @Override
    public boolean doHttp(MiniHttpServer.HttpRequest req, MiniHttpServer.HttpResponse res) throws IOException {
        long start = System.currentTimeMillis();
        if ("/dump.hprof".equals(req.getPath())) {
            return handleDumpDownload(res);
        }
        if ("/pro".equals(req.getPath())) {
            String action = req.getParameter("action");
            if ("slowchildren".equals(action)) {
                int idx = parseIntOrDefault(req.getParameter("idx"), -1);
                int parentId = parseIntOrDefault(req.getParameter("parent"), -1);
                int depth = parseIntOrDefault(req.getParameter("depth"), 0);
                String snapshotKey = req.getParameter("key");
                String html = renderSlowChildrenRows(req, idx, snapshotKey, parentId, depth);
                res.setResponseHeader("Content-Type", "text/html; charset=UTF-8");
                res.getOutputStream().write(html.getBytes("UTF-8"));
                return true;
            }
            if ("reset".equals(action)) {
                org.mini.vm.RefNative.resetProfile();
            } else if ("watch".equals(action)) {
                String methodId = req.getParameter("method");
                String threshold = req.getParameter("threshold");
                if (methodId != null && threshold != null) {
                    int mid = Integer.parseInt(methodId);
                    long thr = Long.parseLong(threshold) * 1000000; // convert ms to ns
                    org.mini.vm.RefNative.watchSlowCallMethod(mid, thr);
                }
            } else if ("unwatch".equals(action)) {
                String methodId = req.getParameter("method");
                if (methodId != null) {
                    int mid = Integer.parseInt(methodId);
                    org.mini.vm.RefNative.unwatchSlowCallMethod(mid);
                }
            } else if ("clearcache".equals(action)) {
                org.mini.vm.RefNative.clearSlowCallCache();
                invalidateSlowSnapshotCache(req);
            } else if ("dump".equals(action)) {
                String saveRoot = GCallBack.getInstance().getAppSaveRoot();
                String dumpPath = new File(saveRoot, "dump.hprof").getAbsolutePath();
                org.mini.vm.RefNative.dumpHeap(dumpPath, 0);
            }
            int selectedSlowIdx = -1;
            if ("viewslow".equals(action)) {
                String idx = req.getParameter("idx");
                if (idx != null) {
                    try {
                        selectedSlowIdx = Integer.parseInt(idx);
                    } catch (Exception ignored) {
                    }
                }
            }
            String html = generateProfilePage(req, selectedSlowIdx);
            res.getOutputStream().write(html.getBytes("UTF-8"));
            return true;
        }
        long end = System.currentTimeMillis();
        //SysLog.info("ProfileServlet: " + (end - start) + "ms");
        return false;
    }

    private String generateProfilePage(MiniHttpServer.HttpRequest req, int selectedSlowIdx) {
        String profileData = org.mini.vm.RefNative.getProfileAll();
        String watchData = org.mini.vm.RefNative.getSlowCallWatchMethods();
        String[] snapshotList = org.mini.vm.RefNative.getSlowCallSnapshotList();
        String vmInfoRows = buildVmInfoRows();
        String gcLogText = buildGcLogText();

        // Parse profile data for tab 0 (original performance view)
        String[] lines = profileData.split("\n");
        StringBuilder tableRows = new StringBuilder();
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("\\|");
            if (parts.length >= 6) {
                tableRows.append("<tr>")
                        .append("<td>").append(escapeHtml(parts[0])).append("</td>")
                        .append("<td>").append(escapeHtml(parts[1])).append("</td>")
                        .append("<td>").append(escapeHtml(parts[2])).append("</td>")
                        .append("<td>").append(escapeHtml(parts[3])).append("</td>")
                        .append("<td style='cursor:pointer;color:#2196F3;font-weight:bold;' onclick='watchMethod(\"" + escapeHtml(parts[4]) + "\")'>").append(escapeHtml(parts[4])).append("</td>")
                        .append("<td>").append(escapeHtml(parts[5])).append("</td>")
                        .append("</tr>");
            }
        }

        // Parse slow call cache and generate tree structure for tab 1
        StringBuilder treeHtml = new StringBuilder();
        if (snapshotList != null && snapshotList.length > 0) {
            treeHtml.append("<div class='slow-wrap'>");
            treeHtml.append("<div class='slow-menu'><h3>Slow Snapshots</h3>");
            for (int i = snapshotList.length - 1, showNo = snapshotList.length; i >= 0; i--, showNo--) {
                SnapshotMeta h = parseSnapshotMeta(snapshotList[i]);
                String label = buildSlowMenuLabel(h, showNo);
                treeHtml.append("<a class='slow-menu-item")
                        .append(i == selectedSlowIdx ? " active" : "")
                        .append("' href='/pro?tab=1&action=viewslow&idx=").append(i).append("'>")
                        .append(escapeHtml(label))
                        .append("</a>");
            }
            treeHtml.append("</div>");
            treeHtml.append("<div class='slow-detail'>");
            if (selectedSlowIdx >= 0 && selectedSlowIdx < snapshotList.length) {
                SlowSnapshotCache cache = getOrBuildSlowSnapshotCache(req, selectedSlowIdx, snapshotList[selectedSlowIdx]);
                treeHtml.append(renderSlowCallRoot(cache));
            } else {
                treeHtml.append("<p style='color:#666;'>Please select snapshot in left list</p>");
            }
            treeHtml.append("</div></div>");
        } else {
            treeHtml.append("<div class='slow-wrap'>");
            treeHtml.append("<div class='slow-menu'><h3>Slow Snapshots</h3><p style='color:#666;'>No snapshot</p></div>");
            treeHtml.append("<div class='slow-detail'><p style='color:#666;'>No slow call data available</p></div>");
            treeHtml.append("</div>");
        }

        // Parse watch data for the original profile view
        StringBuilder watchRows = new StringBuilder();
        if (watchData != null) {
            String[] watchLines = watchData.split("\n");
            for (String line : watchLines) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 3) {
                    watchRows.append("<tr>")
                            .append("<td style='cursor:pointer;color:#f44336;font-weight:bold;' onclick='unwatchMethod(\"" + escapeHtml(parts[0]) + "\")'>").append(escapeHtml(parts[0])).append("</td>")
                            .append("<td>").append(escapeHtml(parts[1])).append("</td>")
                            .append("<td>").append(escapeHtml(parts[2])).append("</td>")
                            .append("</tr>");
                }
            }
        }

        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "<meta charset='UTF-8'>\n"
                + "<title>MiniJVM Performance Profile</title>\n"
                + "<style>\n"
                + "body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }\n"
                + "h1 { color: #333; margin-bottom: 20px; }\n"
                + ".btn-container { margin-bottom: 15px; }\n"
                + ".btn { padding: 10px 20px; color: white; border: none; cursor: pointer; font-size: 14px; margin-right: 10px; }\n"
                + ".btn:hover { opacity: 0.8; }\n"
                + ".toggle-btn { background: #4CAF50; }\n"
                + ".toggle-btn.stopped { background: #f44336; }\n"
                + ".reset-btn { background: #2196F3; }\n"
                + ".clear-btn { background: #ff9800; }\n"
                + ".status { display: inline-block; margin-left: 10px; font-weight: bold; }\n"
                + ".status.running { color: #4CAF50; }\n"
                + ".status.stopped { color: #f44336; }\n"
                + "table { border-collapse: collapse; background: white; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }\n"
                + "th, td { padding: 6px 12px; text-align: left; border-bottom: 1px solid #ddd; font-size: 13px; }\n"
                + "th { background-color: #4CAF50; color: white; font-weight: bold; cursor: pointer; user-select: none; }\n"
                + "th:hover { background-color: #45a049; }\n"
                + "th.sorted-asc::after { content: ' ▲'; }\n"
                + "th.sorted-desc::after { content: ' ▼'; }\n"
                + "tr:hover { background-color: #f5f5f5; }\n"
                + ".tabs { display: flex; border-bottom: 2px solid #ddd; margin-bottom: 20px; }\n"
                + ".tab { padding: 10px 20px; cursor: pointer; background: #e0e0e0; margin-right: 5px; border: none; font-size: 14px; }\n"
                + ".tab.active { background: #4CAF50; color: white; }\n"
                + ".tab-content { display: none; }\n"
                + ".tab-content.active { display: block; }\n"
                + ".tree-node { margin-left: 20px; border-left: 2px solid #ddd; padding-left: 10px; }\n"
                + ".tree-root { margin-left: 0; border-left: none; padding-left: 0; }\n"
                + ".method-name { font-weight: bold; color: #333; }\n"
                + ".method-stats { color: #666; font-size: 11px; margin-left: 10px; }\n"
                + ".node-header { padding: 4px 8px; background: #f9f9f9; border-radius: 3px; cursor: pointer; }\n"
                + ".node-header:hover { background: #f0f0f0; }\n"
                + ".slow-wrap { display:flex; gap:12px; align-items:flex-start; }\n"
                + ".slow-menu { width:320px; max-height:70vh; overflow:auto; background:#fff; border:1px solid #ddd; padding:10px; box-sizing:border-box; }\n"
                + ".slow-menu-item { display:block; padding:6px 8px; text-decoration:none; color:#333; border-bottom:1px solid #f0f0f0; font-size:12px; }\n"
                + ".slow-menu-item:nth-child(even) { background:#fafafa; }\n"
                + ".slow-menu-item:hover { background:#e8f4ff; }\n"
                + ".slow-menu-item.active { background:#d9f0ff; font-weight:bold; }\n"
                + ".slow-detail { flex:1; min-width:0; }\n"
                + ".slow-header { margin:0 0 8px 0; padding:8px; background:#fff; border:1px solid #ddd; font-size:12px; }\n"
                + ".slow-table { width:100%; border-collapse:collapse; background:#fff; }\n"
                + ".slow-table th,.slow-table td { border:1px solid #e5e5e5; padding:6px 8px; font-size:12px; }\n"
                + ".slow-table th { background:#f0f7ff; color:#333; cursor:default; }\n"
                + ".slow-table tbody tr:nth-child(even) { background:#fafafa; }\n"
                + ".slow-table tbody tr:hover { background:#eaf6ff; }\n"
                + ".slow-row.selected { background:#d8ecff !important; }\n"
                + ".slow-row.sibling { background:#eef7ff !important; }\n"
                + ".slow-row.dim { opacity:0.45; }\n"
                + ".chain-cell { font-family:monospace; white-space:nowrap; }\n"
                + ".toggle-btn-mini { display:inline-block; width:16px; text-align:center; cursor:pointer; color:#666; user-select:none; }\n"
                + ".toggle-spacer { display:inline-block; width:16px; }\n"
                + ".chain-label { cursor:pointer; display:inline-block; padding-left:calc(var(--d,0) * 10px); }\n"
                + ".vm-table { width:100%; max-width:900px; }\n"
                + ".vm-table th { cursor:default; }\n"
                + ".vm-log { width:100%; min-height:240px; box-sizing:border-box; background:#111; color:#9be29b; padding:10px; border-radius:4px; overflow:auto; }\n"
                + "</style>\n"
                + "</head>\n"
                + "<body>\n"
                + "<h1>MiniJVM Performance Profile</h1>\n"
                + "<div class='btn-container'>\n"
                + "<button id='toggleBtn' class='btn toggle-btn' onclick='toggleAutoRefresh()'>Stop</button>\n"
                + "<span id='status' class='status running'>Running (5s refresh)</span>\n"
                + "</div>\n"
                + "<div class='tabs'>\n"
                + "<button class='tab' id='tabBtn0' onclick='switchTab(0)'>Performance</button>\n"
                + "<button class='tab' id='tabBtn1' onclick='switchTab(1)'>Slow Call Tree</button>\n"
                + "<button class='tab' id='tabBtn2' onclick='switchTab(2)'>Download</button>\n"
                + "<button class='tab' id='tabBtn3' onclick='switchTab(3)'>VM Info</button>\n"
                + "</div>\n"
                + "<div id='tab0' class='tab-content'>\n"
                + "<div class='btn-container'>\n"
                + "<button class='btn reset-btn' onclick='resetProfile()'>Reset</button>\n"
                + "</div>\n"
                + "<h3>Performance Stats</h3>\n"
                + "<table>\n"
                + "<thead>\n"
                + "<tr><th onclick='sortTable(0)' id='th0'>Call Count</th><th onclick='sortTable(1)' id='th1'>Total Time (ms)</th><th onclick='sortTable(2)' id='th2'>Max Time (ms)</th><th onclick='sortTable(3)' id='th3'>Avg Time (ms)</th><th>Method Id</th><th>Method</th></tr>\n"
                + "</thead>\n"
                + "<tbody>\n"
                + tableRows.toString()
                + "</tbody>\n"
                + "</table>\n"
                + "<h3>Watched Methods</h3>\n"
                + "<table>\n"
                + "<thead>\n"
                + "<tr><th>Method Id (click to unwatch)</th><th>Threshold (ms)</th><th>Method</th></tr>\n"
                + "</thead>\n"
                + "<tbody>\n"
                + watchRows.toString()
                + "</tbody>\n"
                + "</table>\n"
                + "</div>\n"
                + "<div id='tab1' class='tab-content'>\n"
                + "<div class='btn-container'>\n"
                + "<button class='btn clear-btn' onclick='clearCache()'>Clear Cache</button>\n"
                + "</div>\n"
                + "<h3>Watched Methods</h3>\n"
                + "<table>\n"
                + "<thead>\n"
                + "<tr><th>Method Id (click to unwatch)</th><th>Threshold (ms)</th><th>Method</th></tr>\n"
                + "</thead>\n"
                + "<tbody>\n"
                + watchRows.toString()
                + "</tbody>\n"
                + "</table>\n"
                + treeHtml.toString()
                + "</div>\n"
                + "<div id='tab2' class='tab-content'>\n"
                + "<h3>Download</h3>\n"
                + "<div class='btn-container'>\n"
                + "<button class='btn reset-btn' onclick='dumpHeapNow()'>Dump Heap</button>\n"
                + "</div>\n"
                + "<a href='/dump.hprof'>download heap dump</a>\n"
                + "</div>\n"
                + "<div id='tab3' class='tab-content'>\n"
                + "<h3>VM Information</h3>\n"
                + "<table class='vm-table'>\n"
                + "<thead><tr><th>Key</th><th>Value</th></tr></thead>\n"
                + "<tbody>\n"
                + vmInfoRows
                + "</tbody>\n"
                + "</table>\n"
                + "<h3>GC Logs</h3>\n"
                + "<pre class='vm-log'>"
                + gcLogText
                + "</pre>\n"
                + "</div>\n"
                + "<script>\n"
                + "let currentTab = 0;\n"
                + "let autoRefresh = true;\n"
                + "let refreshInterval = null;\n"
                + "let sortColumn = -1;\n"
                + "let sortAscending = true;\n"
                + "let slowSnapshotIdx = -1;\n"
                + "let slowSnapshotKey = '';\n"
                + "let slowExpandedNodes = new Set();\n"
                + "\n"
                + "function switchTab(index) {\n"
                + "    currentTab = index;\n"
                + "    localStorage.setItem('profileTab', index);\n"
                + "    updateTabURL();\n"
                + "    const tabs = document.querySelectorAll('.tab');\n"
                + "    const contents = document.querySelectorAll('.tab-content');\n"
                + "    tabs.forEach((tab, i) => {\n"
                + "        if (i === index) {\n"
                + "            tab.classList.add('active');\n"
                + "            contents[i].classList.add('active');\n"
                + "        } else {\n"
                + "            tab.classList.remove('active');\n"
                + "            contents[i].classList.remove('active');\n"
                + "        }\n"
                + "    });\n"
                + "}\n"
                + "\n"
                + "function getTabFromURL() {\n"
                + "    const params = new URLSearchParams(window.location.search);\n"
                + "    const tab = params.get('tab');\n"
                + "    if (tab !== null) {\n"
                + "        currentTab = parseInt(tab);\n"
                + "    } else {\n"
                + "        const saved = localStorage.getItem('profileTab');\n"
                + "        if (saved !== null) {\n"
                + "            currentTab = parseInt(saved);\n"
                + "        }\n"
                + "    }\n"
                + "    switchTab(currentTab);\n"
                + "}\n"
                + "\n"
                + "function updateTabURL() {\n"
                + "    const url = new URL(window.location);\n"
                + "    url.searchParams.set('tab', currentTab);\n"
                + "    window.history.replaceState({}, '', url);\n"
                + "}\n"
                + "\n"
                + "function clearOneShotActionFromURL() {\n"
                + "    const url = new URL(window.location);\n"
                + "    const action = url.searchParams.get('action');\n"
                + "    if (action === 'clearcache' || action === 'reset' || action === 'dump') {\n"
                + "        url.searchParams.delete('action');\n"
                + "        window.history.replaceState({}, '', url);\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "function getSortFromURL() {\n"
                + "    const params = new URLSearchParams(window.location.search);\n"
                + "    const col = params.get('sort');\n"
                + "    if (col !== null) {\n"
                + "        sortColumn = parseInt(col);\n"
                + "        const order = params.get('order');\n"
                + "        sortAscending = order !== 'desc';\n"
                + "        updateSortIndicator();\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "function updateSortURL() {\n"
                + "    const url = new URL(window.location);\n"
                + "    if (sortColumn >= 0) {\n"
                + "        url.searchParams.set('sort', sortColumn);\n"
                + "        url.searchParams.set('order', sortAscending ? 'asc' : 'desc');\n"
                + "    } else {\n"
                + "        url.searchParams.delete('sort');\n"
                + "        url.searchParams.delete('order');\n"
                + "    }\n"
                + "    window.history.replaceState({}, '', url);\n"
                + "}\n"
                + "\n"
                + "function updateSortIndicator() {\n"
                + "    for (let i = 0; i < 4; i++) {\n"
                + "        const th = document.getElementById('th' + i);\n"
                + "        th.classList.remove('sorted-asc', 'sorted-desc');\n"
                + "    }\n"
                + "    if (sortColumn >= 0 && sortColumn < 4) {\n"
                + "        const th = document.getElementById('th' + sortColumn);\n"
                + "        th.classList.add(sortAscending ? 'sorted-asc' : 'sorted-desc');\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "function sortTable(columnIndex) {\n"
                + "    if (sortColumn === columnIndex) {\n"
                + "        sortAscending = !sortAscending;\n"
                + "    } else {\n"
                + "        sortColumn = columnIndex;\n"
                + "        sortAscending = false;\n"
                + "    }\n"
                + "    updateSortIndicator();\n"
                + "    updateSortURL();\n"
                + "    doSort();\n"
                + "}\n"
                + "\n"
                + "function doSort() {\n"
                + "    const tbody = document.querySelector('#tab0 tbody');\n"
                + "    const rows = Array.from(tbody.querySelectorAll('tr'));\n"
                + "    \n"
                + "    rows.sort((a, b) => {\n"
                + "        const aVal = getNumericValue(a.cells[sortColumn]);\n"
                + "        const bVal = getNumericValue(b.cells[sortColumn]);\n"
                + "        return sortAscending ? aVal - bVal : bVal - aVal;\n"
                + "    });\n"
                + "    \n"
                + "    rows.forEach(row => tbody.appendChild(row));\n"
                + "}\n"
                + "\n"
                + "function getNumericValue(cell) {\n"
                + "    const text = cell.textContent.trim();\n"
                + "    const value = parseFloat(text);\n"
                + "    return isNaN(value) ? 0 : value;\n"
                + "}\n"
                + "\n"
                + "function toggleAutoRefresh() {\n"
                + "    autoRefresh = !autoRefresh;\n"
                + "    localStorage.setItem('autoRefresh', autoRefresh);\n"
                + "    const btn = document.getElementById('toggleBtn');\n"
                + "    const status = document.getElementById('status');\n"
                + "    if (autoRefresh) {\n"
                + "        btn.textContent = 'Stop';\n"
                + "        btn.classList.remove('stopped');\n"
                + "        status.textContent = 'Running (5s refresh)';\n"
                + "        status.classList.remove('stopped');\n"
                + "        status.classList.add('running');\n"
                + "        startAutoRefresh();\n"
                + "    } else {\n"
                + "        btn.textContent = 'Start';\n"
                + "        btn.classList.add('stopped');\n"
                + "        status.textContent = 'Stopped';\n"
                + "        status.classList.remove('running');\n"
                + "        status.classList.add('stopped');\n"
                + "        stopAutoRefresh();\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "function getAutoRefreshFromStorage() {\n"
                + "    const saved = localStorage.getItem('autoRefresh');\n"
                + "    if (saved !== null) {\n"
                + "        autoRefresh = saved === 'true';\n"
                + "    }\n"
                + "    if (!autoRefresh) {\n"
                + "        const btn = document.getElementById('toggleBtn');\n"
                + "        const status = document.getElementById('status');\n"
                + "        btn.textContent = 'Start';\n"
                + "        btn.classList.add('stopped');\n"
                + "        status.textContent = 'Stopped';\n"
                + "        status.classList.remove('running');\n"
                + "        status.classList.add('stopped');\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "function resetProfile() {\n"
                + "    const form = document.createElement('form');\n"
                + "    form.method = 'POST';\n"
                + "    const input = document.createElement('input');\n"
                + "    input.type = 'hidden';\n"
                + "    input.name = 'action';\n"
                + "    input.value = 'reset';\n"
                + "    form.appendChild(input);\n"
                + "    document.body.appendChild(form);\n"
                + "    form.submit();\n"
                + "}\n"
                + "\n"
                + "function watchMethod(methodPtr) {\n"
                + "    const threshold = prompt('Enter slow call threshold (ms):', '150');\n"
                + "    if (threshold !== null) {\n"
                + "        window.location.href = '/pro?action=watch&method=' + methodPtr + '&threshold=' + threshold;\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "function unwatchMethod(methodPtr) {\n"
                + "    if (confirm('Unwatch this method?')) {\n"
                + "        window.location.href = '/pro?action=unwatch&method=' + methodPtr;\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "function clearCache() {\n"
                + "    window.location.href = '/pro?tab=1&action=clearcache';\n"
                + "}\n"
                + "\n"
                + "function dumpHeapNow() {\n"
                + "    window.location.href = '/pro?tab=2&action=dump';\n"
                + "}\n"
                + "\n"
                + "function loadSlowState() {\n"
                + "    const stateEl = document.getElementById('slowState');\n"
                + "    if (!stateEl) return;\n"
                + "    slowSnapshotIdx = parseInt(stateEl.getAttribute('data-snapshot-idx') || '-1');\n"
                + "    slowSnapshotKey = stateEl.getAttribute('data-snapshot-key') || '';\n"
                + "}\n"
                + "\n"
                + "function getSlowExpandedStorageKey() {\n"
                + "    if (!slowSnapshotKey) return '';\n"
                + "    return 'slowExpanded::' + slowSnapshotKey;\n"
                + "}\n"
                + "\n"
                + "function loadSlowExpandedFromStorage() {\n"
                + "    slowExpandedNodes = new Set();\n"
                + "    const key = getSlowExpandedStorageKey();\n"
                + "    if (!key) return;\n"
                + "    const raw = localStorage.getItem(key);\n"
                + "    if (!raw) return;\n"
                + "    try {\n"
                + "        const arr = JSON.parse(raw);\n"
                + "        if (Array.isArray(arr)) {\n"
                + "            arr.forEach(v => slowExpandedNodes.add(String(v)));\n"
                + "        }\n"
                + "    } catch (e) {\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "function saveSlowExpandedToStorage() {\n"
                + "    const key = getSlowExpandedStorageKey();\n"
                + "    if (!key) return;\n"
                + "    localStorage.setItem(key, JSON.stringify(Array.from(slowExpandedNodes)));\n"
                + "}\n"
                + "\n"
                + "function focusSlowRow(row) {\n"
                + "    const rows = Array.from(document.querySelectorAll('#tab1 .slow-row'));\n"
                + "    const parentId = row.getAttribute('data-parent');\n"
                + "    rows.forEach(r => {\n"
                + "        r.classList.remove('selected', 'sibling', 'dim');\n"
                + "    });\n"
                + "    rows.forEach(r => {\n"
                + "        if (r.getAttribute('data-parent') === parentId) {\n"
                + "            r.classList.add('sibling');\n"
                + "        } else {\n"
                + "            r.classList.add('dim');\n"
                + "        }\n"
                + "    });\n"
                + "    row.classList.add('selected');\n"
                + "    row.classList.remove('sibling', 'dim');\n"
                + "}\n"
                + "\n"
                + "function updateSlowToggleText() {\n"
                + "    const toggles = Array.from(document.querySelectorAll('#tab1 .toggle-btn-mini'));\n"
                + "    toggles.forEach(t => {\n"
                + "        const tr = t.closest('.slow-row');\n"
                + "        t.textContent = tr.classList.contains('node-collapsed') ? '▸' : '▾';\n"
                + "    });\n"
                + "}\n"
                + "\n"
                + "function refreshSlowVisibility() {\n"
                + "    const rows = Array.from(document.querySelectorAll('#tab1 .slow-row'));\n"
                + "    const map = {};\n"
                + "    rows.forEach(r => map[r.getAttribute('data-node')] = r);\n"
                + "    rows.forEach(r => {\n"
                + "        let p = r.getAttribute('data-parent');\n"
                + "        let hidden = false;\n"
                + "        while (p && p !== '-1') {\n"
                + "            const pr = map[p];\n"
                + "            if (!pr) break;\n"
                + "            if (pr.classList.contains('node-collapsed')) {\n"
                + "                hidden = true;\n"
                + "                break;\n"
                + "            }\n"
                + "            p = pr.getAttribute('data-parent');\n"
                + "        }\n"
                + "        r.style.display = hidden ? 'none' : '';\n"
                + "    });\n"
                + "    updateSlowToggleText();\n"
                + "}\n"
                + "\n"
                + "function appendChildRows(parentRow, htmlFragment) {\n"
                + "    const parentId = parentRow.getAttribute('data-node');\n"
                + "    const tbody = document.getElementById('slowBody');\n"
                + "    if (!tbody || !htmlFragment) return;\n"
                + "    const marker = document.createElement('tbody');\n"
                + "    marker.innerHTML = htmlFragment;\n"
                + "    let insertAfter = parentRow;\n"
                + "    while (insertAfter.nextElementSibling) {\n"
                + "        const next = insertAfter.nextElementSibling;\n"
                + "        let p = next.getAttribute('data-parent');\n"
                + "        let isDesc = false;\n"
                + "        while (p && p !== '-1') {\n"
                + "            if (p === parentId) {\n"
                + "                isDesc = true;\n"
                + "                break;\n"
                + "            }\n"
                + "            const pp = document.querySelector('#tab1 .slow-row[data-node=\"' + p + '\"]');\n"
                + "            if (!pp) break;\n"
                + "            p = pp.getAttribute('data-parent');\n"
                + "        }\n"
                + "        if (!isDesc) break;\n"
                + "        insertAfter = next;\n"
                + "    }\n"
                + "    while (marker.firstElementChild) {\n"
                + "        insertAfter.insertAdjacentElement('afterend', marker.firstElementChild);\n"
                + "        insertAfter = insertAfter.nextElementSibling;\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "async function fetchSlowChildren(row) {\n"
                + "    if (row.getAttribute('data-loaded') === 'true') return true;\n"
                + "    if (row.getAttribute('data-has-children') !== 'true') return false;\n"
                + "    const nodeId = row.getAttribute('data-node');\n"
                + "    const depth = parseInt(row.getAttribute('data-depth') || '0');\n"
                + "    const toggle = row.querySelector('.toggle-btn-mini');\n"
                + "    if (toggle) toggle.textContent = '…';\n"
                + "    const url = '/pro?action=slowchildren&idx=' + encodeURIComponent(slowSnapshotIdx)\n"
                + "        + '&key=' + encodeURIComponent(slowSnapshotKey)\n"
                + "        + '&parent=' + encodeURIComponent(nodeId)\n"
                + "        + '&depth=' + encodeURIComponent(depth);\n"
                + "    try {\n"
                + "        const resp = await fetch(url, { method: 'GET' });\n"
                + "        if (!resp.ok) throw new Error('http ' + resp.status);\n"
                + "        const html = await resp.text();\n"
                + "        if (html && html.trim().length > 0) {\n"
                + "            appendChildRows(row, html);\n"
                + "        } else {\n"
                + "            row.setAttribute('data-has-children', 'false');\n"
                + "            if (toggle) {\n"
                + "                toggle.classList.remove('toggle-btn-mini');\n"
                + "                toggle.classList.add('toggle-spacer');\n"
                + "                toggle.textContent = '';\n"
                + "            }\n"
                + "        }\n"
                + "        row.setAttribute('data-loaded', 'true');\n"
                + "        return true;\n"
                + "    } catch (e) {\n"
                + "        if (toggle) toggle.textContent = '!';\n"
                + "        return false;\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "async function expandSlowRow(row) {\n"
                + "    if (!row || row.getAttribute('data-has-children') !== 'true') return;\n"
                + "    row.classList.remove('node-collapsed');\n"
                + "    await fetchSlowChildren(row);\n"
                + "    slowExpandedNodes.add(row.getAttribute('data-node'));\n"
                + "    saveSlowExpandedToStorage();\n"
                + "    refreshSlowVisibility();\n"
                + "}\n"
                + "\n"
                + "function collapseSlowRow(row) {\n"
                + "    if (!row) return;\n"
                + "    row.classList.add('node-collapsed');\n"
                + "    slowExpandedNodes.delete(row.getAttribute('data-node'));\n"
                + "    saveSlowExpandedToStorage();\n"
                + "    refreshSlowVisibility();\n"
                + "}\n"
                + "\n"
                + "async function restoreSlowExpandedRows() {\n"
                + "    if (slowExpandedNodes.size === 0) return;\n"
                + "    let pending = Array.from(slowExpandedNodes);\n"
                + "    for (let round = 0; round < 20 && pending.length > 0; round++) {\n"
                + "        let progress = false;\n"
                + "        const next = [];\n"
                + "        for (let i = 0; i < pending.length; i++) {\n"
                + "            const nodeId = pending[i];\n"
                + "            const row = document.querySelector('#tab1 .slow-row[data-node=\"' + nodeId + '\"]');\n"
                + "            if (!row) {\n"
                + "                next.push(nodeId);\n"
                + "                continue;\n"
                + "            }\n"
                + "            if (row.getAttribute('data-has-children') === 'true') {\n"
                + "                row.classList.remove('node-collapsed');\n"
                + "                await fetchSlowChildren(row);\n"
                + "            }\n"
                + "            progress = true;\n"
                + "        }\n"
                + "        pending = next;\n"
                + "        if (!progress) break;\n"
                + "    }\n"
                + "    refreshSlowVisibility();\n"
                + "}\n"
                + "\n"
                + "function initSlowTreeUI() {\n"
                + "    loadSlowState();\n"
                + "    loadSlowExpandedFromStorage();\n"
                + "    const tbody = document.getElementById('slowBody');\n"
                + "    if (!tbody) return;\n"
                + "    tbody.addEventListener('click', async function(e) {\n"
                + "        const row = e.target.closest('.slow-row');\n"
                + "        if (!row) return;\n"
                + "        const toggle = e.target.closest('.toggle-btn-mini');\n"
                + "        if (toggle) {\n"
                + "            if (row.classList.contains('node-collapsed')) {\n"
                + "                await expandSlowRow(row);\n"
                + "            } else {\n"
                + "                collapseSlowRow(row);\n"
                + "            }\n"
                + "            e.stopPropagation();\n"
                + "            return;\n"
                + "        }\n"
                + "        focusSlowRow(row);\n"
                + "    });\n"
                + "    refreshSlowVisibility();\n"
                + "    restoreSlowExpandedRows();\n"
                + "}\n"
                + "\n"
                + "function startAutoRefresh() {\n"
                + "    if (refreshInterval) clearInterval(refreshInterval);\n"
                + "    refreshInterval = setInterval(function() {\n"
                + "        window.location.reload();\n"
                + "    }, 5000);\n"
                + "}\n"
                + "\n"
                + "function stopAutoRefresh() {\n"
                + "    if (refreshInterval) {\n"
                + "        clearInterval(refreshInterval);\n"
                + "        refreshInterval = null;\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "clearOneShotActionFromURL();\n"
                + "getTabFromURL();\n"
                + "getAutoRefreshFromStorage();\n"
                + "getSortFromURL();\n"
                + "if (sortColumn >= 0) {\n"
                + "    updateSortIndicator();\n"
                + "    doSort();\n"
                + "}\n"
                + "\n"
                + "if (autoRefresh) {\n"
                + "    startAutoRefresh();\n"
                + "}\n"
                + "initSlowTreeUI();\n"
                + "</script>\n"
                + "</body>\n"
                + "</html>";
    }

    private boolean handleDumpDownload(MiniHttpServer.HttpResponse res) throws IOException {
        String saveRoot = GCallBack.getInstance().getAppSaveRoot();
        File dumpFile = new File(saveRoot, "dump.hprof");
        if (!dumpFile.exists() || !dumpFile.isFile()) {
            res.setResponseHeader("Content-Type", "text/plain; charset=UTF-8");
            res.getOutputStream().write(("dump file not found: " + dumpFile.getAbsolutePath()).getBytes("UTF-8"));
            return true;
        }
        res.setResponseHeader("Content-Type", "application/octet-stream");
        res.setResponseHeader("Content-Disposition", "attachment; filename=\"dump.hprof\"");
        res.setResponseHeader("Content-Length", String.valueOf(dumpFile.length()));
        FileInputStream fis = new FileInputStream(dumpFile);
        byte[] buf = new byte[4096];
        try {
            int len, total = 0;
            while ((len = fis.read(buf)) != -1) {
                res.getOutputStream().write(buf, 0, len);
                total += len;
            }
            System.out.println("dump file sent: " + total);
        } finally {
            fis.close();
        }
        return true;
    }

    private String buildVmInfoRows() {
        String[] propertyKeys = new String[]{
                "java.version",
                "java.vendor",
                "java.vendor.url",
                "java.class.path",
                "java.library.path",
                "os.name",
                "os.version",
                "os.arch",
                "file.separator",
                "path.separator",
                "user.name",
                "user.home",
                "user.dir"
        };
        StringBuilder rows = new StringBuilder();
        for (String key : propertyKeys) {
            rows.append("<tr><td>")
                    .append(escapeHtml(key))
                    .append("</td><td>")
                    .append(escapeHtml(System.getProperty(key, "")))
                    .append("</td></tr>");
        }
        int ptrSize = org.mini.vm.RefNative.refIdSize();
        Class[] classes = org.mini.vm.RefNative.getClasses();
        Thread[] threads = org.mini.vm.RefNative.getThreads();
        rows.append("<tr><td>vm.refIdSize</td><td>").append(ptrSize).append("</td></tr>");
        rows.append("<tr><td>vm.loadedClassCount</td><td>").append(classes == null ? 0 : classes.length).append("</td></tr>");
        rows.append("<tr><td>vm.threadCount</td><td>").append(threads == null ? 0 : threads.length).append("</td></tr>");
        return rows.toString();
    }

    private String buildGcLogText() {
        Object[] logs = org.mini.vm.VmUtil.gcHistory.toArray();
        if (logs.length == 0) {
            return "No GC logs";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = logs.length - 1; i >= 0; i--) {
            Object obj = logs[i];
            if (obj == null) continue;
            sb.append(escapeHtml(String.valueOf(obj))).append('\n');
        }
        return sb.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private String renderSlowChildrenRows(MiniHttpServer.HttpRequest req, int idx, String snapshotKey, int parentId, int depth) {
        String[] snapshotList = org.mini.vm.RefNative.getSlowCallSnapshotList();
        int resolvedIdx = resolveSnapshotIdx(snapshotList, idx, snapshotKey);
        if (snapshotList == null || resolvedIdx < 0 || resolvedIdx >= snapshotList.length) return "";
        SlowSnapshotCache cache = getOrBuildSlowSnapshotCache(req, resolvedIdx, snapshotList[resolvedIdx]);
        if (cache == null || !cache.valid) return "";
        java.util.List<NodeInfo> children = getDirectChildren(cache, parentId);
        if (children.isEmpty()) return "";
        StringBuilder html = new StringBuilder();
        int childDepth = depth + 1;
        for (NodeInfo n : children) {
            appendSlowRowHtml(html, n, childDepth, parentId, cache.parentNodeSet.contains(n.nodeId), true);
        }
        return html.toString();
    }

    private SlowSnapshotCache getOrBuildSlowSnapshotCache(MiniHttpServer.HttpRequest req, int idx, String snapshotMetaLine) {
        SnapshotMeta meta = parseSnapshotMeta(snapshotMetaLine);
        String key = buildSnapshotKey(meta);
        MiniHttpServer.HttpSession session = req.getSession(false);
        SlowSnapshotCache cache = null;
        if (session != null) {
            Object obj = session.getAttribute(SLOW_SESSION_CACHE_KEY);
            if (obj instanceof SlowSnapshotCache) {
                cache = (SlowSnapshotCache) obj;
            }
        }
        if (cache != null && cache.snapshotIdx != idx) {
            cache = null;
        }
        if (cache != null && !key.equals(cache.snapshotKey)) {
            cache = null;
        }
        if (cache == null) {
            byte[] payload = org.mini.vm.RefNative.getSlowCallStackSnapshot(idx);
            cache = buildSlowSnapshotCache(idx, key, payload);
            if (session != null) {
                session.setAttribute(SLOW_SESSION_CACHE_KEY, cache);
            }
        }
        return cache;
    }

    private void invalidateSlowSnapshotCache(MiniHttpServer.HttpRequest req) {
        MiniHttpServer.HttpSession session = req.getSession(false);
        if (session != null) {
            session.setAttribute(SLOW_SESSION_CACHE_KEY, null);
        }
    }

    private SlowSnapshotCache buildSlowSnapshotCache(int idx, String key, byte[] payload) {
        SlowSnapshotCache cache = new SlowSnapshotCache();
        cache.snapshotIdx = idx;
        cache.snapshotKey = key;
        cache.payload = payload;
        if (payload == null || payload.length < 40) {
            return cache;
        }
        int magic = readInt(payload, 0);
        if (magic != SLOW_MAGIC) {
            return cache;
        }
        cache.tsMs = readLong(payload, 8);
        cache.spentNs = readLong(payload, 16);
        cache.thresholdNs = readLong(payload, 24);
        cache.rootMethodId = readInt(payload, 32);
        cache.nodeCount = readInt(payload, 36);
        java.util.HashSet<Integer> rootIds = new java.util.HashSet<>();
        NodeInfo nodeZero = null;
        int off = 40;
        int parsed = 0;
        while (parsed < cache.nodeCount && off + SLOW_NODE_SIZE <= payload.length) {
            NodeInfo n = new NodeInfo();
            n.nodeId = readInt(payload, off);
            n.parentId = readInt(payload, off + 4);
            n.methodId = readInt(payload, off + 8);
            n.flags = readInt(payload, off + 12);
            n.totalNs = readLong(payload, off + 16);
            n.selfNs = readLong(payload, off + 24);
            n.callCount = readInt(payload, off + 32);
            if (n.callCount <= 0) {
                n.callCount = 1;
            }
            if (n.parentId == -1 && rootIds.add(n.nodeId)) {
                cache.rootNodes.add(n);
            }
            if (n.nodeId == 0) {
                nodeZero = n;
            }
            if (n.parentId >= 0) {
                cache.parentNodeSet.add(n.parentId);
            }
            off += SLOW_NODE_SIZE;
            parsed++;
        }
        if (cache.rootNodes.isEmpty() && nodeZero != null) {
            cache.rootNodes.add(nodeZero);
        }
        cache.rootNodes.sort((a, b) -> Long.compare(b.totalNs, a.totalNs));
        cache.parsedNodeCount = parsed;
        cache.valid = true;
        return cache;
    }

    private java.util.List<NodeInfo> getDirectChildren(SlowSnapshotCache cache, int parentId) {
        synchronized (cache) {
            java.util.List<NodeInfo> cached = cache.childNodeCache.get(parentId);
            if (cached != null) {
                return cached;
            }
            java.util.ArrayList<NodeInfo> list = new java.util.ArrayList<>();
            byte[] payload = cache.payload;
            if (payload == null || payload.length < 40) {
                cache.childNodeCache.put(parentId, list);
                return list;
            }
            int off = 40;
            int parsed = 0;
            while (parsed < cache.nodeCount && off + SLOW_NODE_SIZE <= payload.length) {
                int p = readInt(payload, off + 4);
                if (p == parentId) {
                    NodeInfo n = new NodeInfo();
                    n.nodeId = readInt(payload, off);
                    n.parentId = p;
                    n.methodId = readInt(payload, off + 8);
                    n.flags = readInt(payload, off + 12);
                    n.totalNs = readLong(payload, off + 16);
                    n.selfNs = readLong(payload, off + 24);
                    n.callCount = readInt(payload, off + 32);
                    if (n.callCount <= 0) {
                        n.callCount = 1;
                    }
                    list.add(n);
                }
                off += SLOW_NODE_SIZE;
                parsed++;
            }
            sortList(list);
            cache.childNodeCache.put(parentId, list);
            return list;
        }
    }

    private String renderSlowCallRoot(SlowSnapshotCache cache) {
        if (cache == null || !cache.valid) {
            return "<p style='color:#d00;'>Invalid snapshot format</p>";
        }
        StringBuilder html = new StringBuilder();
        html.append("<div id='slowState' data-snapshot-idx='")
                .append(cache.snapshotIdx)
                .append("' data-snapshot-key='")
                .append(escapeHtml(cache.snapshotKey))
                .append("'></div>");
        html.append("<div class='slow-header'>")
                .append("root=").append(escapeHtml(resolveMethodName(cache.rootMethodId)))
                .append(" | root_id=").append(cache.rootMethodId)
                .append(" | spent_ns=").append(cache.spentNs)
                .append(" | ts_ms=").append(cache.tsMs)
                .append(" | threshold_ns=").append(cache.thresholdNs)
                .append(" | node_count=").append(cache.parsedNodeCount)
                .append("</div>");
        html.append("<table class='slow-table'><thead><tr>")
                .append("<th>Call</th><th>Total(ms)</th><th>Self(ms)</th><th>Max(ms)</th><th>Chain</th>")
                .append("</tr></thead><tbody id='slowBody'>");
        if (cache.rootNodes.isEmpty()) {
            html.append("<tr><td colspan='5'>No rows parsed</td></tr>");
        } else {
            for (NodeInfo root : cache.rootNodes) {
                appendSlowRowHtml(html, root, 0, -1, cache.parentNodeSet.contains(root.nodeId), true);
            }
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private void appendSlowRowHtml(StringBuilder html, NodeInfo n, int depth, int parentId, boolean hasChildren, boolean collapsed) {
        html.append("<tr class='slow-row");
        if (hasChildren && collapsed) {
            html.append(" node-collapsed");
        }
        html.append("' data-node='").append(n.nodeId)
                .append("' data-parent='").append(parentId)
                .append("' data-depth='").append(depth)
                .append("' data-loaded='").append(hasChildren ? "false" : "true")
                .append("' data-has-children='").append(hasChildren ? "true" : "false")
                .append("'>")
                .append("<td>").append(n.callCount).append("</td>")
                .append("<td>").append(format(n.totalNs / 1000000.0, 3)).append("</td>")
                .append("<td>").append(format(n.selfNs / 1000000.0, 3)).append("</td>")
                .append("<td>").append(format(n.totalNs / 1000000.0, 3)).append("</td>")
                .append("<td class='chain-cell'>")
                .append(hasChildren ? "<span class='toggle-btn-mini'>" + (collapsed ? "▸" : "▾") + "</span>" : "<span class='toggle-spacer'></span>")
                .append("<span class='chain-label' style='--d:").append(depth).append("'>")
                .append(escapeHtml(resolveMethodLabel(n.methodId)))
                .append((n.flags & SLOW_FLAG_EXCLUDED) != 0 ? " E" : "")
                .append("</span>")
                .append("</td>")
                .append("</tr>");
    }

    private String buildSnapshotKey(SnapshotMeta meta) {
        return meta.tsMs + "_" + meta.spentNs + "_" + meta.rootMethodId + "_" + meta.nodeCount;
    }

    private int resolveSnapshotIdx(String[] snapshotList, int fallbackIdx, String snapshotKey) {
        if (snapshotList == null || snapshotList.length == 0) return -1;
        if (snapshotKey != null && snapshotKey.length() > 0) {
            int i;
            for (i = 0; i < snapshotList.length; i++) {
                SnapshotMeta meta = parseSnapshotMeta(snapshotList[i]);
                String key = buildSnapshotKey(meta);
                if (snapshotKey.equals(key)) {
                    return i;
                }
            }
        }
        if (fallbackIdx >= 0 && fallbackIdx < snapshotList.length) {
            return fallbackIdx;
        }
        return -1;
    }

    private SnapshotMeta parseSnapshotMeta(String line) {
        SnapshotMeta meta = new SnapshotMeta();
        if (line == null) return meta;
        String[] parts = line.split("\\|");
        if (parts.length >= 5) {
            meta.idx = parseIntSafe(parts[0]) == null ? -1 : parseIntSafe(parts[0]);
            meta.tsMs = parseLongSafe(parts[1]);
            meta.spentNs = parseLongSafe(parts[2]);
            meta.rootMethodId = parseIntSafe(parts[3]);
            meta.nodeCount = parseIntSafe(parts[4]);
        }
        return meta;
    }

    private String buildSlowMenuLabel(SnapshotMeta h, int indexNo) {
        String ts = "no_time";
        if (h.tsMs > 0) {
            ts = new java.text.SimpleDateFormat("HHmmss_SSS").format(new java.util.Date(h.tsMs));
        }
        String method = "id_" + h.rootMethodId;
        String full = resolveMethodName(h.rootMethodId);
        if (full != null && full.length() > 0) {
            int lp = full.indexOf('(');
            String m = lp > 0 ? full.substring(0, lp) : full;
            int dot = m.lastIndexOf('.');
            method = dot >= 0 && dot + 1 < m.length() ? m.substring(dot + 1) : m;
        }
        long spentMs = h.spentNs / 1000000L;
        return ts + "_" + indexNo + "_" + method + "_" + spentMs + "ms";
    }

    private String resolveMethodName(int methodId) {
        if (methodId <= 0) return "id_" + methodId;
        synchronized (METHOD_NAME_CACHE) {
            String cached = METHOD_NAME_CACHE.get(methodId);
            if (cached != null) return cached;
        }
        String m = org.mini.vm.RefNative.getMethodByUniId(methodId);
        if (m == null || m.isEmpty()) {
            return "id_" + methodId;
        }
        synchronized (METHOD_NAME_CACHE) {
            METHOD_NAME_CACHE.put(methodId, m);
        }
        return m;
    }

    private String resolveMethodLabel(int methodId) {
        if (methodId <= 0) return "id_" + methodId;
        synchronized (METHOD_LABEL_CACHE) {
            String cached = METHOD_LABEL_CACHE.get(methodId);
            if (cached != null) return cached;
        }
        String full = resolveMethodName(methodId);
        String label = shortenMethodLabel(full);
        synchronized (METHOD_LABEL_CACHE) {
            METHOD_LABEL_CACHE.put(methodId, label);
        }
        return label;
    }

    private String shortenMethodLabel(String full) {
        if (full == null || full.isEmpty()) return full;
        if (full.startsWith("id_")) return full;
        int lp = -1;//full.indexOf('(');
        String sig = lp >= 0 ? full.substring(0, lp) : full;
        int lastDot = sig.lastIndexOf('.');
        if (lastDot <= 0 || lastDot >= sig.length() - 1) {
            return sig;
        }
        String owner = sig.substring(0, lastDot);
        String method = sig.substring(lastDot + 1);
        String[] seg = owner.split("/");
        if (seg.length == 0) return method;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seg.length; i++) {
            String s = seg[i];
            if (s.isEmpty()) continue;
            if (i == seg.length - 1) {
                sb.append(s);
            } else {
                sb.append(s.charAt(0));
            }
            if (i != seg.length - 1) sb.append('/');
        }
        sb.append('.').append(method);
        return sb.toString();
    }

    private int readInt(byte[] arr, int off) {
        return ((arr[off] & 0xff) << 24)
                | ((arr[off + 1] & 0xff) << 16)
                | ((arr[off + 2] & 0xff) << 8)
                | (arr[off + 3] & 0xff);
    }

    private long readLong(byte[] arr, int off) {
        return ((long) (arr[off] & 0xff) << 56)
                | ((long) (arr[off + 1] & 0xff) << 48)
                | ((long) (arr[off + 2] & 0xff) << 40)
                | ((long) (arr[off + 3] & 0xff) << 32)
                | ((long) (arr[off + 4] & 0xff) << 24)
                | ((long) (arr[off + 5] & 0xff) << 16)
                | ((long) (arr[off + 6] & 0xff) << 8)
                | ((long) (arr[off + 7] & 0xff));
    }

    private Long parseLongSafe(String v) {
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseIntSafe(String v) {
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return null;
        }
    }

    private int parseIntOrDefault(String v, int def) {
        Integer val = parseIntSafe(v);
        return val == null ? def : val;
    }

    static class SnapshotMeta {
        int idx = -1;
        long tsMs;
        long spentNs;
        int rootMethodId;
        int nodeCount;
    }

    static class NodeInfo {
        int nodeId;
        int parentId;
        int methodId;
        int flags;
        long totalNs;
        long selfNs;
        int callCount = 1;
    }

    static class SlowSnapshotCache {
        int snapshotIdx;
        String snapshotKey = "";
        byte[] payload;
        long tsMs;
        long spentNs;
        long thresholdNs;
        int rootMethodId;
        int nodeCount;
        int parsedNodeCount;
        boolean valid;
        java.util.List<NodeInfo> rootNodes = new java.util.ArrayList<>();
        java.util.Set<Integer> parentNodeSet = new java.util.HashSet<>();
        java.util.Map<Integer, java.util.List<NodeInfo>> childNodeCache = new java.util.HashMap<>();
    }


    public static String format(double value, int decimalPlaces) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "Infinity" : "-Infinity";
        }

        boolean negative = value < 0;
        if (negative) {
            value = -value;
        }

        long integerPart = (long) value;
        double fractionalPart = value - integerPart;

        for (int i = 0; i < decimalPlaces; i++) {
            fractionalPart *= 10;
        }
        long fractionRounded = Math.round(fractionalPart);

        if (fractionRounded >= pow10(decimalPlaces)) {
            integerPart += 1;
            fractionRounded = 0;
        }

        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append('-');
        }
        sb.append(integerPart);
        sb.append('.');

        String fractionStr = Long.toString(fractionRounded);
        int leadingZeros = decimalPlaces - fractionStr.length();
        for (int i = 0; i < leadingZeros; i++) {
            sb.append('0');
        }
        if (fractionRounded > 0 || decimalPlaces > 0) {
            sb.append(fractionStr);
        } else {
            for (int i = 0; i < decimalPlaces; i++) {
                sb.append('0');
            }
        }

        return sb.toString();
    }

    public static String format(float value, int decimalPlaces) {
        return format((double) value, decimalPlaces);
    }

    private static long pow10(int n) {
        long result = 1;
        for (int i = 0; i < n; i++) {
            result *= 10;
        }
        return result;
    }

    void sortList(List<NodeInfo> rows) {
        int size = rows.size();
        if (size <= 1) return;

        for (int i = 1; i < size; i++) {
            NodeInfo key = rows.get(i);
            long keyTotal = key.totalNs;

            int left = 0;
            int right = i;
            while (left < right) {
                int mid = (left + right) >>> 1;
                if (rows.get(mid).totalNs < keyTotal) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }

            for (int j = i; j > left; j--) {
                rows.set(j, rows.get(j - 1));
            }
            rows.set(left, key);
        }
    }
}
