param(
    [string]$JaninoHome = 'D:/github/janino-continue',
    [string]$JavaHome = 'D:/jdk-1.8',
    [string]$GccHome = 'D:/mingw64',
    [int]$Jobs = 8,
    [int]$Repeats = 3,
    [ValidateSet('main', 'aot')][string[]]$Modes = @('main', 'aot'),
    [long]$AotHeapBytes = 0,
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path.Replace('\', '/')
$build = "$repo/j2c/build/janino-benchmark"
$env:PATH = "$GccHome/bin;" + $env:PATH
$previousHeapOverride = $env:J2C_BENCH_HEAP_BYTES
$series = if ($AotHeapBytes -gt 0) { "heap-$AotHeapBytes" } else { 'default' }
New-Item -ItemType Directory -Force "$build/translator", "$build/config-src", "$build/main-project" | Out-Null
Push-Location $repo
try {
    if (!$SkipBuild) {
        $config = 'package probe; final class JaninoPaths { static final String REPO = "' + $repo + '"; static final String OUTPUT = "' + $build + '/unused"; }'
        Set-Content "$build/config-src/JaninoPaths.java" $config -Encoding UTF8
        @(rg --files j2c/translator/src/main/java -g '*.java') | Set-Content "$build/sources.txt"
        & "$JavaHome/bin/javac.exe" -encoding UTF-8 -d "$build/translator" "@$build/sources.txt"
        if ($LASTEXITCODE) { throw 'Translator compilation failed' }
        $sources = "minijvm/java/src/main/java;test/j2c_janino;$build/config-src;$JaninoHome/commons-compiler/src/main/java;$JaninoHome/janino/src/main/java"
        & "$JavaHome/bin/java.exe" -cp "$build/translator;$JavaHome/lib/tools.jar" com.ebsee.Main $sources "$build/classes" "$build/c" *> "$build/translate.log"
        if ($LASTEXITCODE) { throw "Translation failed: $build/translate.log" }
        & cmake -S j2c/app -B "$build/aot-release" -G 'MinGW Makefiles' "-DCMAKE_C_COMPILER=$GccHome/bin/gcc.exe" "-DCMAKE_MAKE_PROGRAM=$GccHome/bin/mingw32-make.exe" "-DJ2C_GENERATED_DIR=$build/c" -DCMAKE_BUILD_TYPE=Release
        if ($LASTEXITCODE) { throw 'AOT configure failed' }
        & cmake --build "$build/aot-release" -j $Jobs *> "$build/aot-build.log"
        if ($LASTEXITCODE) { throw "AOT build failed: $build/aot-build.log" }
        $cmake = @'
cmake_minimum_required(VERSION 3.23)
project(janino_benchmark_main C)
add_subdirectory("@REPO@/minijvm/c" runtime)
set_target_properties(mini_jvm PROPERTIES RUNTIME_OUTPUT_DIRECTORY "@BUILD@/main-bin")
'@
        $cmake.Replace('@REPO@', $repo).Replace('@BUILD@', $build) | Set-Content "$build/main-project/CMakeLists.txt"
        & cmake -S "$build/main-project" -B "$build/main-release" -G 'MinGW Makefiles' "-DCMAKE_C_COMPILER=$GccHome/bin/gcc.exe" "-DCMAKE_MAKE_PROGRAM=$GccHome/bin/mingw32-make.exe" -DCMAKE_BUILD_TYPE=Release
        if ($LASTEXITCODE) { throw 'Main runtime configure failed' }
        & cmake --build "$build/main-release" --target mini_jvm -j $Jobs *> "$build/main-build.log"
        if ($LASTEXITCODE) { throw "Main runtime build failed: $build/main-build.log" }
    }
    if ($AotHeapBytes -gt 0) {
        # Link a test-only constructor wrapper; never alter production GC policy.
        $policy = @'
function(add_janino_benchmark_policy)
    target_sources(app PRIVATE "@REPO@/test/j2c_janino/heap_override.c")
    target_link_options(app PRIVATE "-Wl,--wrap=garbage_collector_create")
endfunction()
cmake_language(DEFER CALL add_janino_benchmark_policy)
'@
        $policy.Replace('@REPO@', $repo) | Set-Content "$build/heap-policy.cmake"
        & cmake -S j2c/app -B "$build/aot-release" "-DCMAKE_PROJECT_app_INCLUDE=$build/heap-policy.cmake" *> "$build/heap-configure.log"
        if ($LASTEXITCODE) { throw "Heap policy configuration failed: $build/heap-configure.log" }
        & cmake --build "$build/aot-release" -j $Jobs *> "$build/heap-build.log"
        if ($LASTEXITCODE) { throw "Heap policy build failed: $build/heap-build.log" }
        $env:J2C_BENCH_HEAP_BYTES = "$AotHeapBytes"
    } else {
        Remove-Item Env:J2C_BENCH_HEAP_BYTES -ErrorAction SilentlyContinue
    }
    $reference = "$build/host-reference-$([guid]::NewGuid().ToString('N'))"
    Set-Content "$build/current-run.properties" "output=$reference" -Encoding ASCII
    & "$JavaHome/bin/java.exe" -cp "$build/classes" probe.JaninoBenchmark *> "$build/host-reference.log"
    if ($LASTEXITCODE -or !(Get-Content "$build/host-reference.log" -Raw).Contains('JANINO_BENCHMARK_PASS')) { throw "Host reference failed: $build/host-reference.log" }
    $expected = @{}
    Get-ChildItem $reference -Recurse -Filter '*.class' | ForEach-Object { $expected[$_.FullName.Substring($reference.Length + 1)] = (Get-FileHash -LiteralPath $_.FullName).Hash }
    $results = [Collections.Generic.List[object]]::new()
    for ($round = 1; $round -le $Repeats; $round++) {
        $order = if ($round % 2) { @('main', 'aot') } else { @('aot', 'main') }
        $order = @($order | Where-Object { $Modes -contains $_ })
        foreach ($mode in $order) {
            $id = "$series-$mode-$round-$([guid]::NewGuid().ToString('N'))"
            $out = "$build/$id-classes"
            Set-Content "$build/current-run.properties" "output=$out" -Encoding ASCII
            $exe = if ($mode -eq 'main') { "$build/main-bin/mini_jvm.exe" } else { "$build/aot-release/app.exe" }
            $runArgs = if ($mode -eq 'main') { @('-bootclasspath', "$repo/binary/lib/minijvm_rt.jar", '-cp', "$build/classes", 'probe.JaninoBenchmark') } else { @('probe.JaninoBenchmark') }
            $timer = [Diagnostics.Stopwatch]::StartNew()
            $process = Start-Process -WindowStyle Hidden -FilePath $exe -ArgumentList $runArgs -PassThru -RedirectStandardOutput "$build/$id.stdout.log" -RedirectStandardError "$build/$id.stderr.log"
            while (!$process.WaitForExit(1000)) {
                if ($timer.Elapsed.TotalMinutes -gt 25) { $process.Kill(); throw "Timed out: $id" }
            }
            $timer.Stop()
            $stdout = Get-Content "$build/$id.stdout.log" -Raw
            $match = [regex]::Match($stdout, 'JANINO_BENCHMARK_PASS units=(\d+) compile_ms=(\d+) driver_ms=(\d+) initial_heap=(\d+) final_heap=(\d+)')
            if ($process.ExitCode -ne 0 -or !$match.Success) { throw "Failed $id : $build/$id.stdout.log and stderr.log" }
            $files = @(Get-ChildItem $out -Recurse -Filter '*.class')
            if ($files.Count -ne $expected.Count) { throw "Class count differs: $id" }
            foreach ($file in $files) {
                $relative = $file.FullName.Substring($out.Length + 1)
                if (!$expected.ContainsKey($relative) -or (Get-FileHash -LiteralPath $file.FullName).Hash -ne $expected[$relative]) { throw "Bytecode differs: $id/$relative" }
            }
            $row = [pscustomobject]@{ mode=$mode; round=$round; compile_ms=[long]$match.Groups[2].Value; driver_ms=[long]$match.Groups[3].Value; process_ms=[math]::Round($timer.Elapsed.TotalMilliseconds, 1); initial_heap=[long]$match.Groups[4].Value; final_heap=[long]$match.Groups[5].Value; classes=$files.Count; output=$out }
            $results.Add($row)
            $results | ConvertTo-Json -Depth 4 | Set-Content "$build/results.json"
            $results | ConvertTo-Json -Depth 4 | Set-Content "$build/results-$series.json"
            $row | ConvertTo-Json -Compress
        }
    }
} finally {
    $env:J2C_BENCH_HEAP_BYTES = $previousHeapOverride
    Pop-Location
}
