<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>${title}</title>
    <style>
        body {
            font-family: "Segoe UI", "Helvetica Neue", Arial, sans-serif;
            margin: 24px;
            color: #1f2933;
            background-color: #ffffff;
        }
        h1, h2, h3 {
            color: #0b3d62;
        }
        h1 {
            font-size: 28px;
            margin-bottom: 6px;
        }
        h2 {
            font-size: 22px;
            margin-top: 24px;
            border-bottom: 2px solid #e5e7eb;
            padding-bottom: 4px;
        }
        h3 {
            font-size: 18px;
            margin-bottom: 6px;
        }
        .meta {
            margin-bottom: 12px;
            font-size: 14px;
            color: #4b5563;
        }
        .runtime-summary {
            margin-bottom: 18px;
            padding: 12px;
            background-color: #f3f4f6;
            border-left: 4px solid #2563eb;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 12px;
        }
        th, td {
            border: 1px solid #d1d5db;
            padding: 8px 10px;
            text-align: left;
        }
        th {
            background-color: #f9fafb;
        }
        .chart {
            margin-top: 16px;
            page-break-inside: avoid;
        }
        .chart img {
            width: 100%;
            max-width: 720px;
            height: auto;
            border: 1px solid #d1d5db;
            border-radius: 4px;
            background: #fff;
        }
        ul.recommendations {
            list-style-type: disc;
            padding-left: 20px;
        }
        ul.recommendations li {
            margin-bottom: 10px;
        }
    </style>
</head>
<body>
<h1>${title}</h1>
<div class="meta">
    <div><strong>Source:</strong> ${sourcePath}</div>
    <div><strong>Generated:</strong> ${generatedAt}</div>
</div>
<div class="runtime-summary">${runtimeSummary}</div>

<h2>Key metrics</h2>
<table>
    <tr>
        <th>Metric</th>
        <th>Value</th>
    </tr>
    <tr>
        <td>Total pause time</td>
        <td>${summary.totalPauseTime?string["0.00"]} ms</td>
    </tr>
    <tr>
        <td>Percent paused</td>
        <td>${summary.percentPaused?string["0.00"]} %</td>
    </tr>
    <tr>
        <td>Average pause</td>
        <td>${summary.averagePause?string["0.00"]} ms</td>
    </tr>
    <tr>
        <td>Median pause</td>
        <td>${summary.medianPause?string["0.00"]} ms</td>
    </tr>
    <tr>
        <td>P90 pause</td>
        <td>${summary.p90Pause?string["0.00"]} ms</td>
    </tr>
    <tr>
        <td>P99 pause</td>
        <td>${summary.p99Pause?string["0.00"]} ms</td>
    </tr>
    <tr>
        <td>Max pause</td>
        <td>${summary.maxPause?string["0.00"]} ms</td>
    </tr>
</table>

<h2>Recommendations</h2>
<ul class="recommendations">
    <#list recommendations as recommendation>
        <li><strong>${recommendation.title}</strong> – ${recommendation.detail}</li>
    </#list>
</ul>

<#if charts?has_content>
    <h2>Visual trends</h2>
    <#list charts as chart>
        <div class="chart">
            <h3>${chart.title}</h3>
            <img src="${chart.imageDataUri}" alt="${chart.title}" />
            <p>${chart.description}</p>
        </div>
    </#list>
</#if>

<h2>GC cause breakdown</h2>
<#if causeRows?has_content>
    <table>
        <tr>
            <th>Cause</th>
            <th>Events</th>
            <th>Average duration (ms)</th>
        </tr>
        <#list causeRows as row>
            <tr>
                <td>${row.cause}</td>
                <td>${row.count}</td>
                <td>${row.averageDuration?string["0.00"]}</td>
            </tr>
        </#list>
    </table>
<#else>
    <p>No GC cause data was available for this log.</p>
</#if>

<h2>Collection cycle counts</h2>
<#if cycleRows?has_content>
    <table>
        <tr>
            <th>GC type</th>
            <th>Cycles</th>
        </tr>
        <#list cycleRows as row>
            <tr>
                <td>${row.type}</td>
                <td>${row.count}</td>
            </tr>
        </#list>
    </table>
<#else>
    <p>No GC cycle data was recorded.</p>
</#if>

</body>
</html>
